package com.lizongying.mytv.agent

import android.content.Context
import android.os.SystemClock
import android.util.AtomicFile
import com.lizongying.mytv.PlaylistChannel
import com.lizongying.mytv.PlaylistParser
import com.lizongying.mytv.TVList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.coroutineContext

object AgentRepository {
    internal val runLock = Mutex()
    private val diskLock = Any()
    private fun prefs(c: Context) = c.getSharedPreferences("agent_state", Context.MODE_PRIVATE)
    internal fun read(c: Context, name: String): String? = synchronized(diskLock) {
        runCatching { AtomicFile(File(c.filesDir, name)).openRead().bufferedReader().use { it.readText() } }.getOrNull()
    }
    internal fun write(c: Context, name: String, value: String) = synchronized(diskLock) {
        val file = AtomicFile(File(c.filesDir, name)); val out = file.startWrite()
        try { out.write(value.toByteArray(Charsets.UTF_8)); file.finishWrite(out) }
        catch (e: Exception) { file.failWrite(out); throw e }
    }

    fun extraChannels(c: Context): List<PlaylistChannel> = read(c, "agent-playlist.m3u")?.let { SourceTools.parse(it) }.orEmpty()

    fun mediaItem(c: Context, url: String, id: String = url): androidx.media3.common.MediaItem {
        val routes=runCatching { AgentConfigStore.strings(JSONArray(prefs(c).getString("hlsRoutes","[]"))) }.getOrDefault(emptyList())
        return androidx.media3.common.MediaItem.Builder().setUri(url).setMediaId(id).apply {
            if(url in routes) setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
        }.build()
    }

    fun health(c: Context): Map<String, HealthRecord> = runCatching {
        val o = JSONObject(read(c, "agent-health.json") ?: "{}")
        o.keys().asSequence().associateWith { url ->
            val v = o.getJSONObject(url)
            HealthCodec.read(v)
        }
    }.getOrDefault(emptyMap())

    private fun record(c: Context, url: String, passed: Boolean, latency: Long, decoded: Boolean, reason: String = "请求失败 / 媒体无效") = synchronized(diskLock) {
        val o = runCatching { JSONObject(read(c, "agent-health.json") ?: "{}") }.getOrDefault(JSONObject())
        val previous = o.optJSONObject(url)?.let(HealthCodec::read)
        val next = HealthPolicy.observe(previous, passed, decoded, System.currentTimeMillis(), latency, reason)
        o.put(url, HealthCodec.write(next))
        if (o.length() > 2000) {
            val oldest = o.keys().asSequence().sortedWith(compareBy<String> { (o.optJSONObject(it)?.optLong("removedAt") ?: 0) > 0 }
                .thenBy { o.optJSONObject(it)?.optLong("at") ?: 0 }).take(o.length() - 2000).toList()
            oldest.forEach(o::remove)
        }
        write(c, "agent-health.json", o.toString())
        if ((previous?.removedAt ?: 0) == 0L && next.removedAt > 0) {
            log(c, "已移除无效线路：" + NetworkPolicy.displayUrl(url).take(100) + "；" + next.reason)
            bumpRevision(c)
        } else if ((previous?.removedAt ?: 0) > 0 && next.removedAt == 0L) {
            log(c, "线路连续检测恢复，已重新加入")
            bumpRevision(c)
        }
    }

    internal fun recordProbe(c: Context, url: String, passed: Boolean, latency: Long) = record(c, url, passed, latency, false)

    suspend fun recordPlayback(c: Context, url: String, passed: Boolean, reason: String = "播放器报错 / 缓冲超时") = withContext(Dispatchers.IO) {
        if (url.isNotBlank()) runCatching {
            if (passed) HealthScanner.noteEvidence(c)
            if (passed || HealthScanner.canRecordFailures(c, false)) record(c, url, passed, 0, true, reason)
        }
    }

    fun rank(c: Context, channels: List<PlaylistChannel>, includeRemoved: Boolean = false): List<PlaylistChannel> {
        val h = health(c); val blocked = AgentConfigStore(c).load().blocked; val now = System.currentTimeMillis()
        val denied = prepareMemory(c,h).adIds()
        return channels.map { it.copy(urls = HealthRanking.sort(it.urls.filterNot { url -> MemoryPolicy.id(url) in denied }, h, blocked, now, includeRemoved)) }.filter { it.urls.isNotEmpty() }
    }

    fun prepareMemory(c: Context, h: Map<String,HealthRecord> = health(c)): MemoryStore {
        val store=MemoryStore(c)
        if (!store.migrated()) store.migrateLegacy(h,VisionStore.legacyResults(c))
        return store
    }
    fun memoryChanged(c: Context) = bumpRevision(c)

    fun restoreAdMemory(c: Context, id: String) = synchronized(diskLock) {
        val o=JSONObject(read(c,"agent-health.json") ?: "{}")
        o.keys().asSequence().filter { MemoryPolicy.id(it) == id && o.getJSONObject(it).optString("reason").contains("纯广告") }
            .toList().forEach(o::remove)
        write(c,"agent-health.json",o.toString()); bumpRevision(c)
        log(c,"已解除一条永久广告屏蔽，下次可重新检查")
    }

    private fun bumpRevision(c: Context) { val p = prefs(c); p.edit().putLong("revision", maxOf(System.currentTimeMillis(), p.getLong("revision", 0) + 1)).apply() }

    fun removeCurrent(c: Context, url: String, reason: String = "手动报告黑屏 / 无效") = synchronized(diskLock) {
        require(url.isNotBlank()) { "当前没有播放线路" }
        val o = JSONObject(read(c, "agent-health.json") ?: "{}")
        o.put(url, HealthCodec.write(HealthPolicy.manuallyRemove(o.optJSONObject(url)?.let(HealthCodec::read), System.currentTimeMillis(), reason)))
        write(c, "agent-health.json", o.toString()); bumpRevision(c)
        log(c, "已手动移除线路，可在已移除列表中恢复")
    }

    fun removed(c: Context): List<Pair<String, HealthRecord>> {
        val h=health(c); val denied=prepareMemory(c,h).adIds()
        return h.filter { it.value.removedAt > 0 && MemoryPolicy.id(it.key) !in denied }.toList().sortedByDescending { it.second.removedAt }
    }

    fun restoreRemoved(c: Context, url: String? = null): Int {
        val denied=prepareMemory(c).adIds()
        return synchronized(diskLock) {
        val o = JSONObject(read(c, "agent-health.json") ?: "{}")
        val removed = o.keys().asSequence().filter { (url == null || it == url) && MemoryPolicy.id(it) !in denied && o.getJSONObject(it).optLong("removedAt") > 0 }.toList()
        removed.forEach { o.remove(it) }
        write(c, "agent-health.json", o.toString()); bumpRevision(c)
        log(c, "已恢复 " + removed.size + " 条线路，失败计数重新开始")
        removed.size
        }
    }

    fun log(c: Context, message: String) = synchronized(diskLock) {
        val time = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date())
        val old = prefs(c).getString("log", "").orEmpty()
        prefs(c).edit().putString("log", (time + "  " + message.take(220) + "\n" + old).lineSequence().take(35).joinToString("\n")).apply()
    }

    fun status(c: Context): String {
        val config = AgentConfigStore(c); val state = prefs(c)
        val timestamp = state.getLong("lastRun", 0)
        val last = if (timestamp == 0L) "尚未执行" else SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(timestamp))
        val active = System.currentTimeMillis() - state.getLong("runningAt", 0) < 6 * 60 * 1000L
        val h = health(c)
        return (if (active) "正在维护…\n" else "上次维护：" + last + "\n") +
            "自动维护：" + (if (config.load().enabled) "已开启" else "已暂停") +
            "　AI：" + (if (config.load().aiEnabled && config.apiKey().isNotBlank()) "已配置" else "未启用") +
            "\n已检测线路 " + h.size + "，24 小时内成功 " + h.values.count { it.passed && System.currentTimeMillis() - it.checkedAt < 24 * 60 * 60 * 1000L } +
            "\n已移除无效线路 " + h.values.count { it.removedAt > 0 } + "（可恢复）；连续 3 次失败才自动移除" +
            "\n后台找源：" + state.getString("discoveryStatus", "退出播放后自动安排；所有功能均已内置") +
            "\n" + AutoVisionSettings(c).summary() +
            "\n" + RealLogoStore.summary(c) +
            "\n" + state.getString("scanStatus", "可点“检测并移除无效线路”检查全列表") +
            "\n" + state.getString("summary", "自主爬取电视台网站、解析动态播放器及 M3U/TXT；不需要搜索引擎或 AI Key。") +
            "\n\n" + state.getString("log", "")
    }

    fun discoveryPending(c: Context): Boolean = prefs(c).getInt("discoveryPending", 0) > 0

    suspend fun maintain(c: Context, force: Boolean = false, idleOnly: Boolean = false) = withContext(Dispatchers.IO) {
        val config = AgentConfigStore(c).load(); val state = prefs(c)
        if (!config.enabled && !force) return@withContext
        if (!force && state.getInt("crawlerVersion",0)>=10800 && !discoveryPending(c) && System.currentTimeMillis() - state.getLong("lastRun", 0) < config.intervalHours * 3_600_000L) return@withContext
        if (!runLock.tryLock()) throw ScanDeferred("其他维护正在执行，空闲后续查")
        val deadline = SystemClock.elapsedRealtime() + 270000
        fun withinBudget() = SystemClock.elapsedRealtime() < deadline
        fun allowed() {
            if (idleOnly && DecoderGate.foregroundWanted()) throw ScanDeferred("正在看电视，后台找源已让出，退出播放后继续")
            if (!force && !AgentConfigStore(c).load().enabled) throw ScanDeferred("自动维护已关闭")
        }
        var plan = runCatching { JSONObject(read(c,"agent-discovery.json") ?: "{}") }.getOrDefault(JSONObject())
        fun checkpoint() {
            DiscoveryPolicy.bound(plan)
            write(c,"agent-discovery.json",plan.toString())
            state.edit().putInt("discoveryPending",plan.optJSONArray("queue")?.length() ?: 0).apply()
        }
        state.edit().putLong("runningAt", System.currentTimeMillis()).apply()
        try {
            allowed()
            log(c, "空闲维护：电视台网站自主爬取、更新订阅并检查线路")
            val memory=prepareMemory(c); val adIds=memory.adIds()
            val beforeRoutes=TVList.load(c,64,true).flatMap { it.urls }.toSet()
            TVList.refresh(c, force)
            val names=TVList.channelNames(c)
            val discovered=runCatching { AgentConfigStore.strings(JSONArray(state.getString("discovered","[]"))) }.getOrDefault(emptyList())
            var selected=discovered
            val payload=runCatching { JSONObject(read(c,"agent-sources.json") ?: "{}") }.getOrDefault(JSONObject())
            val cache=payload.optJSONObject("sources") ?: payload
            val oldRoots=payload.optJSONObject("roots") ?: JSONObject()
            val entries=StationCrawler.entries+RepositoryCrawler.entries
            val sourceUrls=(entries.map { it.url }+config.sources+selected).mapNotNull(DiscoveryPolicy::canonical).distinct().take(70)
            val meta=plan.optJSONObject("meta") ?: JSONObject().also { plan.put("meta",it) }
            // New roots enter a persisted frontier. Unfinished child pages survive process death.
            sourceUrls.forEach { root ->
                if(DiscoveryPolicy.add(plan,listOf(root),System.currentTimeMillis(),force)>0)
                    meta.put(root,JSONObject().put("root",root).put("depth",0).put("kind","page")
                        .put("title",entries.firstOrNull { DiscoveryPolicy.canonical(it.url)==root }?.title.orEmpty()))
            }
            checkpoint()
            allowed()
            val next=JSONObject(); val nextRoots=JSONObject()
            cache.keys().asSequence().toList().forEach { url ->
                val root=DiscoveryPolicy.canonical(oldRoots.optString(url,url))
                if(root in sourceUrls) { next.put(url,cache.getString(url)); nextRoots.put(url,root) }
            }
            // Collapse mirrored copies already retained by earlier versions.
            val fingerprints=mutableMapOf<String,String>()
            var duplicates=0
            next.keys().asSequence().toList().forEach { url ->
                val id=DiscoveryPolicy.contentId(SourceTools.parse(next.getString(url),names))
                if(fingerprints[id]!=null) { next.remove(url); nextRoots.remove(url); duplicates++ } else fingerprints[id]=url
            }
            var accepted=0; var newSubscriptions=0; var fetched=0; var rendered=0; var directChecks=0
            val directRefreshed=mutableSetOf<String>()
            val hlsRoutes=runCatching { AgentConfigStore.strings(JSONArray(state.getString("hlsRoutes","[]"))).toMutableSet() }.getOrDefault(mutableSetOf())
            fun commitSources() {
                val result=JSONObject().put("sources",next).put("roots",nextRoots)
                if(result.toString().toByteArray(Charsets.UTF_8).size>6*1024*1024) {
                    log(c,"订阅缓存超过 6 MB，保留之前频道"); return
                }
                val extra=PlaylistParser.merge(next.keys().asSequence().map { SourceTools.parse(next.getString(it),names) }.toList(),64)
                write(c,"agent-sources.json",result.toString())
                write(c,"agent-playlist.m3u",SourceTools.encode(extra))
                val active=extra.flatMap { it.urls }.filter { it in hlsRoutes }.distinct().take(1000).toMutableList()
                while(JSONArray(active).toString().toByteArray(Charsets.UTF_8).size>256*1024) active.removeAt(active.lastIndex)
                state.edit().putString("hlsRoutes",JSONArray(active).toString()).apply()
                bumpRevision(c)
            }
            try {
                while(fetched<20 && withinBudget()) {
                    coroutineContext.ensureActive(); allowed()
                    val url=DiscoveryPolicy.next(plan) ?: break
                    val info=meta.optJSONObject(url) ?: JSONObject().put("root",url)
                    val root=info.optString("root",url)
                    if(root !in sourceUrls || memory.cooling(url) || MemoryPolicy.id(url) in adIds) {
                        DiscoveryPolicy.done(plan,url,System.currentTimeMillis()); meta.remove(url); checkpoint(); continue
                    }
                    fetched++
                    try {
                        state.edit().putString("discoveryStatus","后台读取页面 "+fetched+"："+NetworkPolicy.displayUrl(url).take(100)).apply()
                        // Any discovered public repo/raw link can reveal more playlists through its catalogue.
                        RepositoryCrawler.catalogue(url)?.let { catalog ->
                            StationCrawler.enqueue(plan,CrawlLink(catalog,"公开节目单目录","catalog"),root,info.optInt("depth",0),url,System.currentTimeMillis())
                        }
                        val address=RepositoryCrawler.fetchAddress(DiscoveryPolicy.fetchAddress(url))
                        val reply=HttpFetch.get(address,512*1024,sample=info.optString("kind")=="media")
                        val text=reply.bytes.toString(Charsets.UTF_8)
                        allowed(); coroutineContext.ensureActive()
                        val parsed=SourceTools.parse(text,names)
                        val direct=if(SourceTools.isHls(text) || info.optString("kind")=="media")
                            StationCrawler.channelTitle(info.optString("title"),names.values) ?: StationCrawler.channelTitle(URI(url).path,names.values) else null
                        var livePassed=false
                        if(parsed.isEmpty() && direct!=null) {
                            if(directChecks>=6) break
                            directChecks++
                            val live=LiveStreamVerifier.check(address,reply,::allowed)
                            livePassed=live.passed
                            if(livePassed && live.hls) hlsRoutes.add(address)
                            log(c,"自主发现 "+direct+"："+live.reason)
                        }
                        val candidates=if(parsed.isEmpty() && direct!=null && livePassed) listOf(PlaylistChannel(direct,
                            if(direct.startsWith("CCTV",true) || direct.startsWith("CGTN",true)) "央视" else if(direct.contains("卫视")) "卫视" else "地方台","",listOf(address))) else parsed
                        val channels=candidates.map { it.copy(urls=it.urls.filterNot { route -> MemoryPolicy.id(route) in adIds }) }.filter { it.urls.isNotEmpty() }
                        if(channels.isNotEmpty()) {
                            val cacheKey=if(direct!=null && info.optString("origin").isNotBlank()) info.getString("origin")+"#channel="+MemoryPolicy.id(direct).take(16) else url
                            val id=DiscoveryPolicy.contentId(channels)
                            val same=fingerprints[id]
                            if(same!=null && same!=cacheKey) {
                                duplicates++; memory.sourceSucceeded(url)
                            } else {
                                val trusted=url in config.sources
                                val samplePassed=livePassed || trusted || channels.take(3).any { channel -> channel.urls.take(1).any { route ->
                                    allowed()
                                    val probe=runCatching { HttpFetch.probe(route) }
                                    if(probe.isSuccess) recordProbe(c,route,true,probe.getOrThrow())
                                    probe.isSuccess
                                } }
                                if(samplePassed) {
                                    val existed=next.has(cacheKey)
                                    if(existed) fingerprints.remove(DiscoveryPolicy.contentId(SourceTools.parse(next.getString(cacheKey),names)))
                                    val merged=if(direct!=null && !directRefreshed.add(cacheKey) && existed)
                                        PlaylistParser.merge(listOf(SourceTools.parse(next.getString(cacheKey),names),channels),8) else channels
                                    next.put(cacheKey,SourceTools.encode(merged).take(512*1024)); nextRoots.put(cacheKey,root); fingerprints[id]=cacheKey
                                    accepted++; if(!existed) newSubscriptions++
                                    memory.sourceSucceeded(url)
                                    log(c,(if(existed) "更新已有订阅：" else "接收新订阅：")+NetworkPolicy.displayUrl(url).take(100)+"，"+channels.size+" 个频道（媒体抽检通过，实播待巡检）")
                                } else memory.sourceFailed(url)
                            }
                        } else if(!SourceTools.isHls(text) && info.optString("kind")!="media") {
                            val title=SourceTools.pageTitle(text).ifBlank { info.optString("title") }
                            val links=(RepositoryCrawler.links(url,text)+StationCrawler.links(reply.url,text,title)+StationCrawler.jsonStreams(reply.url,text,title)+
                                SourceTools.pageLinks(address,text).map { CrawlLink(it,title) }).toMutableList()
                            val depth=info.optInt("depth",0)
                            val html=text.trimStart().startsWith("<")
                            if(html && info.optString("kind")!="script" && rendered<3 && withinBudget() &&
                                (StationCrawler.isPlayerPage(url,text) || links.isEmpty() && text.contains("<script",true))) {
                                allowed(); rendered++
                                state.edit().putString("discoveryStatus","自主解析播放器 "+rendered+"："+NetworkPolicy.displayUrl(url)).apply()
                                links.addAll(runCatching { WebPageResolver(c).resolve(reply.url,text,
                                    StationCrawler.channelTitle(info.optString("title"),names.values) ?: title,::allowed) }.getOrElse { e ->
                                    if(e is ScanDeferred || e is kotlinx.coroutines.CancellationException) throw e
                                    log(c,"本机网页组件未完成解析，保留静态发现结果"); emptyList()
                                })
                            }
                            if(links.isEmpty()) memory.sourceFailed(url) else memory.sourceSucceeded(url)
                            links.distinctBy { it.url }.take(32).forEach { child ->
                                val continuation=RepositoryCrawler.continuation(url,child.url)
                                if(child.kind in setOf("media","playlist") || continuation || depth<StationCrawler.MAX_DEPTH)
                                    StationCrawler.enqueue(plan,child,root,if(continuation) depth else depth+1,
                                        if(info.optString("kind")=="script") info.optString("origin",url) else url,System.currentTimeMillis())
                            }
                        }
                    } catch(e: ScanDeferred) { throw e }
                    catch(_: Exception) { coroutineContext.ensureActive(); memory.sourceFailed(url) }
                    DiscoveryPolicy.done(plan,url,System.currentTimeMillis()); meta.remove(url)
                    checkpoint()
                }
            } finally {
                // Completed pages and subscriptions are durable even when viewing resumes.
                commitSources(); checkpoint()
            }
            allowed()
            val now=System.currentTimeMillis()
            // An explicit refresh may search again after five minutes; automatic discovery follows the chosen interval.
            val gap=if(force) 5*60_000L else config.intervalHours*3_600_000L
            if(config.aiEnabled && now-state.getLong("aiAttempt",0)>=gap && withinBudget()) {
                allowed(); coroutineContext.ensureActive()
                val cursor=state.getInt("discoveryTopic",0).coerceAtLeast(0)%DiscoveryPolicy.topics.size
                state.edit().putLong("aiAttempt",now).putInt("discoveryTopic",(cursor+1)%DiscoveryPolicy.topics.size).apply()
                try {
                    state.edit().putString("discoveryStatus","正在联网找源："+DiscoveryPolicy.topics[cursor]).apply()
                    val hits=AiClient(c).discover(DiscoveryPolicy.topics[cursor],config.sources+selected,::allowed)
                        .filterNot { memory.cooling(it.url) || MemoryPolicy.id(it.url) in adIds }
                    val fresh=hits.map { it.url }.filterNot { it in selected || it in config.sources }.distinct()
                    selected=(fresh+hits.map { it.url }+selected).distinct().take(40)
                    fresh.forEach { root ->
                        if(DiscoveryPolicy.add(plan,listOf(root),System.currentTimeMillis())>0)
                            meta.put(root,JSONObject().put("root",root).put("kind","page").put("depth",0))
                    }
                    checkpoint()
                    // Persist a paid response before checking foreground priority.
                    state.edit().putString("discovered",JSONArray(selected).toString())
                        .putString("discoveryStatus","搜索返回 "+hits.size+" 个真实结果；新候选页面 "+fresh.size+" 个（尚待解析验证）").apply()
                    log(c,state.getString("discoveryStatus","").orEmpty())
                } catch(e: ScanDeferred) { throw e }
                catch(e: Exception) {
                    coroutineContext.ensureActive()
                    log(c,"本轮联网搜索未完成，已有来源保留。"+if(e is IllegalArgumentException) e.message.orEmpty().take(100) else "稍后自动重试")
                }
            }
            val urls=TVList.load(c,64,true).flatMap { it.urls }.distinct().sorted()
            val cursor=state.getInt("cursor",0).let { if(urls.isEmpty()) 0 else it%urls.size }
            val sample=if(urls.isEmpty()) emptyList() else (0 until minOf(12,urls.size)).map { urls[(cursor+it)%urls.size] }
            val semaphore=Semaphore(3)
            val checks=coroutineScope { sample.map { url -> async {
                semaphore.withPermit { ensureActive(); allowed(); if(!withinBudget()) null else HttpFetch.check(url) }
            } }.awaitAll().filterNotNull() }
            val passed=checks.count { it.passed }
            val allowFailures=checks.none { it.failed } || HealthScanner.canRecordFailures(c,checks.any { it.passed && System.currentTimeMillis()-it.checkedAt<20_000 })
            checks.forEach { if(it.passed || it.failed && allowFailures) recordProbe(c,it.url,it.passed,it.latency) }
            val addedRoutes=(TVList.load(c,64,true).flatMap { it.urls }.toSet()-beforeRoutes).size
            val summary="本轮自主读取 "+fetched+" 页，动态播放器 "+rendered+" 个，直接直播验证 "+directChecks+" 条；订阅更新 "+accepted+"，新增订阅 "+newSubscriptions+"，重复节目单 "+duplicates+"；净增线路 "+addedRoutes+"；媒体抽检 "+passed+"/"+checks.size+"。画面及广告另行巡检。"
            state.edit().putLong("lastRun",System.currentTimeMillis()).putInt("cursor",cursor+sample.size)
                .putString("summary",summary).putString("discoveryStatus",if(discoveryPending(c)) "进度已保存，空闲时继续剩余页面" else "本轮已完成，等待退出播放或定时补查")
                .putInt("crawlerVersion",10800).putLong("revision",System.currentTimeMillis()).apply()
            log(c,summary)
        } finally {
            state.edit().putLong("runningAt",0).apply()
            runLock.unlock()
        }
    }

    fun block(c: Context, url: String) {
        require(url.isNotBlank()) { "当前没有播放线路" }
        prepareMemory(c).confirmAd(url,"手动确认纯广告线路")
        memoryChanged(c)
        log(c,"已永久记住纯广告线路，可在记忆管理中解除")
    }
}
