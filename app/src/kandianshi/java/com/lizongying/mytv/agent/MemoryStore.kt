package com.lizongying.mytv.agent

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File

class MemoryStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("agent_memory_settings",Context.MODE_PRIVATE)
    private fun read(name: String): JSONObject = runCatching {
        AtomicFile(File(context.filesDir,name)).openRead().bufferedReader().use { JSONObject(it.readText()) }
    }.getOrDefault(JSONObject())
    private fun write(name: String, data: JSONObject) {
        val f = AtomicFile(File(context.filesDir,name)); val out = f.startWrite()
        try { out.write(data.toString().toByteArray(Charsets.UTF_8)); f.finishWrite(out) }
        catch(e: Exception) { f.failWrite(out); throw e }
    }
    fun settings() = MemorySettings(prefs.getInt("mib",1).takeIf { it in listOf(1,2,5) } ?: 1,
        prefs.getInt("days",7).takeIf { it in listOf(1,7,30,90) } ?: 7)
    fun configure(settings: MemorySettings) = synchronized(lock) {
        check(prefs.edit().putInt("mib",settings.mib).putInt("days",settings.days).commit()) { "无法保存记忆设置" }
        saveRecent(read(RECENT),settings)
    }
    private fun saveRecent(book: JSONObject, settings: MemorySettings = settings()) {
        MemoryPolicy.trim(book,System.currentTimeMillis(),settings); write(RECENT,book)
    }
    fun migrated(): Boolean = prefs.getBoolean("migrated130",false)
    // Call outside the repository / vision file locks. Existing user-confirmed ads survive upgrade.
    fun migrateLegacy(health: Map<String,HealthRecord>, results: List<Pair<String,JSONObject>>) = synchronized(lock) {
        if (migrated()) return@synchronized
        val ads = read(ADS)
        val recent = read(RECENT)
        MemoryPolicy.importLegacy(ads,recent,health,results)
        write(ADS,ads); saveRecent(recent)
        check(prefs.edit().putBoolean("migrated130",true).commit())
    }
    fun adIds(): Set<String> = synchronized(lock) { read(ADS).keys().asSequence().toSet() }
    fun ads(): List<Pair<String,JSONObject>> = synchronized(lock) {
        val a = read(ADS)
        a.keys().asSequence().map { it to a.getJSONObject(it) }.sortedByDescending { it.second.optLong("at") }.toList()
    }
    fun confirmAd(url: String, title: String) = synchronized(lock) {
        val ads=read(ADS); MemoryPolicy.pin(ads,url,title,System.currentTimeMillis()); write(ADS,ads)
        val book=read(RECENT); book.remove("v:"+MemoryPolicy.id(url)); book.remove("r:"+MemoryPolicy.id(url)); saveRecent(book)
    }
    fun forgetAd(id: String) = synchronized(lock) {
        val a=read(ADS); a.remove(id); write(ADS,a)
        val book=read(RECENT); book.remove("v:"+id); book.remove("r:"+id); saveRecent(book)
    }
    fun cached(url: String): JSONObject? = synchronized(lock) {
        MemoryPolicy.cached(read(RECENT),url,System.currentTimeMillis(),settings())
    }
    fun results(kind: String?): List<Pair<String,JSONObject>> = synchronized(lock) {
        val original=read(RECENT)
        val before=MemoryPolicy.bytes(original)
        val book=MemoryPolicy.trim(original,System.currentTimeMillis(),settings())
        if (MemoryPolicy.bytes(book) != before) write(RECENT,book)
        book.keys().asSequence().filter { it.startsWith("v:") }.map { book.getJSONObject(it) }
            .filter { kind == null || it.optString("kind") == kind }.map { it.getString("url") to it }
            .sortedByDescending { it.second.optLong("at") }.toList()
    }
    fun putVision(route: VisionRoute, verdict: VisionVerdict) = synchronized(lock) {
        val book=read(RECENT); MemoryPolicy.putVision(book,route,verdict,System.currentTimeMillis())
        if(verdict.kind!="suspect") book.remove("r:"+MemoryPolicy.id(route.url))
        saveRecent(book)
    }
    fun resolve(url: String, kind: String, expectedAt: Long) = synchronized(lock) {
        val book=MemoryPolicy.trim(read(RECENT),System.currentTimeMillis(),settings()); val ads=read(ADS)
        MemoryPolicy.resolve(book,ads,url,kind,expectedAt,System.currentTimeMillis())
        book.remove("r:"+MemoryPolicy.id(url))
        if (kind == "removed") write(ADS,ads)
        saveRecent(book)
    }
    fun cooling(url: String): Boolean = synchronized(lock) {
        MemoryPolicy.cooling(read(RECENT),url,System.currentTimeMillis(),settings())
    }
    fun sourceFailed(url: String) = synchronized(lock) {
        val book=read(RECENT); MemoryPolicy.failedSource(book,url,System.currentTimeMillis()); saveRecent(book)
    }
    fun sourceSucceeded(url: String) = synchronized(lock) {
        val book=read(RECENT); if (book.has("s:"+MemoryPolicy.id(url))) { book.remove("s:"+MemoryPolicy.id(url)); saveRecent(book) }
    }
    fun autoReview(url: String): JSONObject? = synchronized(lock) {
        read(RECENT).optJSONObject("r:"+MemoryPolicy.id(url))?.takeIf { MemoryPolicy.fresh(it,System.currentTimeMillis(),settings()) }
    }
    fun pendingAutoReviews(): List<VisionRoute> = synchronized(lock) {
        val book=read(RECENT); saveRecent(book)
        book.keys().asSequence().filter { it.startsWith("r:") }.map { book.getJSONObject(it) }
            .map { VisionRoute(it.optString("title"),it.getString("url")) }.toList()
    }
    fun dueAutoReviews(): List<VisionRoute> = synchronized(lock) {
        val now=System.currentTimeMillis(); val book=read(RECENT); saveRecent(book)
        book.keys().asSequence().filter { it.startsWith("r:") }.map { book.getJSONObject(it) }
            .filter { now-it.optLong("at") >= AutoVisionPolicy.RECHECK_MS }.sortedBy { it.optLong("at") }
            .map { VisionRoute(it.optString("title"),it.getString("url")) }.toList()
    }
    fun recordAuto(route: VisionRoute, verdict: VisionVerdict, run: String, autoBlock: Boolean): Boolean = synchronized(lock) {
        val now=System.currentTimeMillis(); val book=MemoryPolicy.trim(read(RECENT),now,settings()); val ads=read(ADS)
        MemoryPolicy.putVision(book,route,verdict,now)
        val blocked=AutoVisionPolicy.observe(book,ads,route,verdict,now,run,autoBlock)
        if (blocked) write(ADS,ads)
        saveRecent(book); blocked
    }
    fun clearRecent() = synchronized(lock) {
        write(RECENT,JSONObject())
        write("agent-discovery.json",JSONObject())
        context.getSharedPreferences("agent_state",Context.MODE_PRIVATE).edit().putInt("discoveryPending",0).apply()
    }
    fun summary(): String = synchronized(lock) {
        val book=read(RECENT); saveRecent(book)
        val ads=read(ADS); val settings=settings()
        "普通记忆：" + MemoryPolicy.bytes(book)/1024 + " KB / " + settings.mib + " MB，保留 " + settings.days + " 天\n" +
            "永久广告屏蔽：" + ads.length() + " 条，" + MemoryPolicy.bytes(ads)/1024 + " KB / 2 MB\n" +
            "巡检进度：" + File(context.filesDir,"agent-vision.json").length()/1024 + " KB（固定最多 1 MB）\n" +
            "找源索引 / 进度：" + File(context.filesDir,"agent-discovery.json").length()/1024 + " KB（固定最多 256 KB，不保存网页或聊天）"
    }
    companion object {
        private val lock=Any()
        private const val RECENT="agent-memory.json"
        private const val ADS="agent-ad-memory.json"
    }
}
