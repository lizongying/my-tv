package com.lizongying.mytv.agent

import android.content.Context
import org.json.JSONObject
import java.util.concurrent.Executors

object LogoStore {
    private val lock = Any()
    private var state: JSONObject? = null
    private val writer = Executors.newSingleThreadExecutor()
    private fun state(c: Context): JSONObject = state ?: runCatching {
        JSONObject(AgentRepository.read(c, "channel-icons.json") ?: "{}")
    }.getOrDefault(JSONObject()).also { state = it }
    private fun save(c: Context) {
        AgentRepository.write(c, "channel-icons.json", LogoPolicy.bound(state(c), AgentConfigStore(c).load().logoCacheKb).toString())
    }
    fun design(c: Context, title: String): LogoDesign = synchronized(lock) {
        LogoPolicy.design(state(c).optJSONObject(LogoPolicy.id(title))) ?: LogoPolicy.fallback(title)
    }
    fun needsDesign(c: Context, title: String): Boolean = synchronized(lock) {
        LogoPolicy.design(state(c).optJSONObject(LogoPolicy.id(title))) == null
    }
    fun failedRemote(c: Context, title: String, url: String, now: Long = System.currentTimeMillis()): Boolean = synchronized(lock) {
        val o = state(c).optJSONObject(LogoPolicy.id(title))
        o != null && o.optString("badUrl") == MemoryPolicy.id(url) && now - o.optLong("badAt", 0) in 0 until LogoPolicy.RETRY_MS
    }
    fun noteMissing(c: Context, title: String, failedUrl: String? = null) {
        val app = c.applicationContext
        writer.execute { runCatching {
            enqueue(app, listOf(title))
            if (failedUrl != null) synchronized(lock) {
                state(app).optJSONObject(LogoPolicy.id(title))?.put("badUrl", MemoryPolicy.id(failedUrl))?.put("badAt", System.currentTimeMillis())
                save(app)
            }
            if (needsDesign(app, title)) LogoScheduler.nudge(app)
        } }
    }
    fun applyLimit(c: Context) { val app = c.applicationContext; writer.execute { runCatching { synchronized(lock) { save(app) } } } }
    fun enqueue(c: Context, titles: List<String>) = synchronized(lock) {
        val data = state(c); var changed = false
        titles.take(500).forEach { name ->
            val key = LogoPolicy.id(name)
            if (!data.has(key)) {
                data.put(key, JSONObject().put("title", LogoPolicy.title(name)).put("at", System.currentTimeMillis()))
                changed = true
            }
        }
        if (changed) save(c)
    }
    fun pending(c: Context, now: Long = System.currentTimeMillis()): List<String> = synchronized(lock) { LogoPolicy.pending(state(c), now) }
    fun attempted(c: Context, names: List<String>, now: Long) = synchronized(lock) {
        names.forEach { state(c).optJSONObject(LogoPolicy.id(it))?.put("attempt", now) }; save(c)
    }
    fun complete(c: Context, designs: Map<String, LogoDesign>) = synchronized(lock) {
        designs.forEach { (id, d) -> state(c).optJSONObject(id)?.put("palette", d.palette)?.put("motif", d.motif) }
        save(c)
        if (designs.isNotEmpty()) AgentRepository.memoryChanged(c)
    }
    fun summary(c: Context): String = synchronized(lock) {
        val data = state(c); val ready = data.keys().asSequence().count { LogoPolicy.design(data.optJSONObject(it)) != null }
        "频道图标：已设计 $ready，等待 ${data.length() - ready}；缓存上限 ${AgentConfigStore(c).load().logoCacheKb} KB"
    }
}
