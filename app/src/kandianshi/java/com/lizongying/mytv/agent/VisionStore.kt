package com.lizongying.mytv.agent

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.sync.Mutex
import org.json.JSONObject
import java.io.File

class VisionStore(private val context: Context, private val planKey: String = "plan") {
    private val file = AtomicFile(File(context.filesDir, "agent-vision.json"))
    private val prefs = context.getSharedPreferences("agent_vision_budget", Context.MODE_PRIVATE)
    private fun read(): JSONObject = runCatching { file.openRead().bufferedReader().use { JSONObject(it.readText()) } }.getOrDefault(JSONObject())
    private fun write(o: JSONObject) {
        o.remove("results") // Migrated to the bounded ordinary-memory store.
        check(MemoryPolicy.bytes(o) <= 1024*1024) { "巡检进度超过容量上限，请先清理普通记忆与进度" }
        val out = file.startWrite()
        try { out.write(o.toString().toByteArray(Charsets.UTF_8)); file.finishWrite(out) }
        catch (e: Exception) { file.failWrite(out); throw e }
    }
    fun limit(): Int = prefs.getInt("limit", 10).takeIf { it in setOf(10, 50, 200) } ?: 10
    fun setLimit(limit: Int) { require(limit in setOf(10, 50, 200)); check(prefs.edit().putInt("limit", limit).commit()) }
    fun used(): Int = synchronized(lock) { VisionQuota(prefs.getLong("day", -1), prefs.getInt("calls", 0)).used(System.currentTimeMillis()) }
    fun reserve() = synchronized(lock) {
        val q = VisionQuota(prefs.getLong("day", -1), prefs.getInt("calls", 0)).reserve(System.currentTimeMillis(), limit())
        check(prefs.edit().putLong("day", q.day).putInt("calls", q.calls).commit()) { "无法保存用量，未发送截图" }
    }
    fun plan(): VisionPlan? = synchronized(lock) { runCatching { VisionPlan.read(read().getJSONObject(planKey)) }.getOrNull() }
    fun begin(routes: List<VisionRoute>, replace: Boolean): VisionPlan = synchronized(lock) {
        val o = read()
        val old = runCatching { VisionPlan.read(o.getJSONObject(planKey)) }.getOrNull()
        val plan = if (!replace && old != null && !old.done) old else VisionPlan(MemoryPolicy.boundedRoutes(routes,if (planKey == "autoPlan") 192*1024 else MemoryPolicy.PLAN_BYTES))
        o.put(planKey, plan.json()); write(o); plan
    }
    fun advanceCached(plan: VisionPlan, route: VisionRoute): VisionPlan = synchronized(lock) {
        val o=read(); check(VisionPlan.read(o.getJSONObject(planKey)) == plan) { "巡检进度已变化，请重新打开" }
        val next=plan.advance(route.url); o.put(planKey,next.json()); write(o); next
    }
    fun commit(plan: VisionPlan, route: VisionRoute, verdict: VisionVerdict): VisionPlan = synchronized(lock) {
        val o=read(); check(VisionPlan.read(o.getJSONObject(planKey)) == plan) { "巡检进度已变化，请重新打开" }
        MemoryStore(context).putVision(route,verdict)
        val next=plan.advance(route.url); o.put(planKey,next.json()); write(o); next
    }
    fun results(kind: String? = null): List<Pair<String, JSONObject>> = MemoryStore(context).results(kind)
    fun resolve(url: String, kind: String, expectedAt: Long) {
        MemoryStore(context).resolve(url,kind,expectedAt)
        if (kind == "removed") {
            AgentRepository.memoryChanged(context)
            AgentRepository.log(context,"已永久记住确认的纯广告线路，可在记忆管理中解除")
        }
    }
    fun clearPlans() = synchronized(lock) { write(JSONObject()) }
    companion object {
        private val lock = Any()
        val runLock = Mutex()
        fun legacyResults(context: Context): List<Pair<String,JSONObject>> = synchronized(lock) {
            val o=VisionStore(context).read().optJSONObject("results") ?: JSONObject()
            o.keys().asSequence().map { it to o.getJSONObject(it) }.toList()
        }
    }
}
