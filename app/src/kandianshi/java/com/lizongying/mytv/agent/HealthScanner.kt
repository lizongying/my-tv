package com.lizongying.mytv.agent

import android.content.Context
import android.net.ConnectivityManager
import android.os.SystemClock
import android.util.AtomicFile
import com.lizongying.mytv.TVList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.coroutineContext

/** A resumable catalogue scan. Failed routes get three separate rounds; successes need one. */
object HealthScanner {
    enum class Outcome { COMPLETE, MORE, RETRY, BUSY }
    private val evidenceLock = Any()
    fun noteEvidence(c: Context) = c.getSharedPreferences("agent_state", Context.MODE_PRIVATE).edit()
        .putLong("mediaEvidence", System.currentTimeMillis()).apply()

    @Suppress("DEPRECATION")
    fun networkConnected(c: Context): Boolean = runCatching {
        (c.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo?.isConnected == true
    }.getOrDefault(false)

    fun canRecordFailures(c: Context, hasSuccess: Boolean): Boolean = synchronized(evidenceLock) {
        val p = c.getSharedPreferences("agent_state", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val connected = networkConnected(c)
        if (connected && hasSuccess) noteEvidence(c)
        NetworkEvidence.permitsFailures(connected, hasSuccess || now - p.getLong("mediaEvidence", 0) in 0 until 20_000L) {
        if (now - p.getLong("failedMediaEvidence", 0) in 0 until 20_000L) return@permitsFailures false
        // A whole failed batch may be a disconnected router or blocked network. Verify another media route first.
        val known = AgentRepository.health(c).filter { it.value.passed && it.value.removedAt == 0L }.keys.take(1)
        val verified = JSONArray(c.assets.open("verified_urls.json").bufferedReader().use { it.readText() })
        val controls = (known + (0 until minOf(3, verified.length())).map { verified.getString(it) }).distinct().take(3)
        val passed = controls.any { runCatching { HttpFetch.probe(it) }.isSuccess }
        if (passed) noteEvidence(c) else p.edit().putLong("failedMediaEvidence", System.currentTimeMillis()).apply()
        passed
        }
    }

    private fun file(c: Context) = AtomicFile(File(c.filesDir, "agent-scan.json"))
    private fun read(c: Context): JSONObject? = runCatching { file(c).openRead().bufferedReader().use { JSONObject(it.readText()) } }.getOrNull()
    private fun write(c: Context, state: JSONObject) {
        val f = file(c); val out = f.startWrite()
        try { out.write(state.toString().toByteArray(Charsets.UTF_8)); f.finishWrite(out) }
        catch (e: Exception) { f.failWrite(out); throw e }
    }
    private fun status(c: Context, text: String) = c.getSharedPreferences("agent_state", Context.MODE_PRIVATE).edit().putString("scanStatus", text).apply()

    suspend fun scan(c: Context, begin: Boolean, generation: Long): Outcome = withContext(Dispatchers.IO) {
        if (!AgentRepository.runLock.tryLock()) return@withContext Outcome.BUSY
        val prefs = c.getSharedPreferences("agent_state", Context.MODE_PRIVATE)
        fun checkSession() {
            if (prefs.getLong("scanGeneration", 0) != generation) throw CancellationException("检测已停止或重新开始")
        }
        prefs.edit().putLong("runningAt", System.currentTimeMillis()).apply()
        try {
            checkSession()
            var state = read(c)
            if (state == null || begin && (state.optBoolean("done") || System.currentTimeMillis() - state.optLong("started") > 24 * 60 * 60 * 1000L)) {
                val urls = TVList.load(c, 64, true).flatMap { it.urls }.distinct().sorted().take(2000)
                state = ScanPlan.create(urls, System.currentTimeMillis()).state
                checkSession()
                write(c, state)
                AgentRepository.log(c, "开始全列表检测：" + urls.size + " 条线路；失败线路将间隔复核两轮")
            }
            val current = requireNotNull(state)
            val plan = ScanPlan(current)
            if (current.optBoolean("done")) return@withContext Outcome.COMPLETE
            val queue = AgentConfigStore.strings(current.getJSONArray("queue"))
            val deadline = SystemClock.elapsedRealtime() + 240000L
            var cursor = current.optInt("cursor")
            while (cursor < queue.size && SystemClock.elapsedRealtime() < deadline - 20000) {
                coroutineContext.ensureActive()
                checkSession()
                status(c, "全列表检测第 " + current.optInt("round") + "/3 轮：" + cursor + "/" + queue.size + "；正在检查清单和媒体分片")
                val routes = plan.pending()
                val results = mutableListOf<HttpFetch.Check>()
                // Three parallel requests at a time, leaving bandwidth and the hardware decoder for watching TV.
                for (batch in routes.chunked(3)) {
                    results += coroutineScope { batch.map { url -> async {
                        val result = HttpFetch.check(url)
                        ensureActive(); result
                    } }.awaitAll() }
                }
                coroutineContext.ensureActive()
                val allowFailures = results.none { it.failed } || canRecordFailures(c, results.any { it.passed && System.currentTimeMillis() - it.checkedAt < 20_000 })
                coroutineContext.ensureActive()
                checkSession()
                if (!allowFailures) {
                    results.filter { it.passed }.forEach { AgentRepository.recordProbe(c, it.url, true, it.latency) }
                    status(c, "检测暂停：未确认媒体网络可用，保留频道；联网后重试，可重新点击继续")
                    return@withContext Outcome.RETRY
                }
                results.forEach {
                    if (it.conclusive) AgentRepository.recordProbe(c, it.url, it.passed, it.latency)
                }
                plan.accept(results.map { it.url to !it.failed })
                cursor = plan.cursor
                current.put("unverified", current.optInt("unverified") + results.count { !it.conclusive })
                write(c, current)
            }
            if (cursor < queue.size) return@withContext Outcome.MORE
            checkSession()
            if (plan.finishRound()) {
                write(c, current)
                status(c, "本轮结束，等待 45 秒后复核 " + plan.queue.size + " 条失败线路")
                return@withContext Outcome.MORE
            }
            write(c, current)
            val summary = "全列表检测完成：" + current.optInt("total") + " 条线路；未能验证 " + current.optInt("unverified") + " 条保留；当前已移除 " + AgentRepository.removed(c).size + " 条（可恢复）"
            status(c, summary); AgentRepository.log(c, summary)
            Outcome.COMPLETE
        } finally {
            prefs.edit().putLong("runningAt", 0).apply()
            AgentRepository.runLock.unlock()
        }
    }
}
