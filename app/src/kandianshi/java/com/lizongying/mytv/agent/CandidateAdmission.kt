package com.lizongying.mytv.agent
import android.content.Context
import android.os.SystemClock
import com.lizongying.mytv.PlaylistChannel
import com.lizongying.mytv.PlaylistParser
import org.json.JSONObject
import kotlinx.coroutines.*

object CandidateAdmission {
    private val lock=Any()
    private fun read(c: Context)=runCatching { JSONObject(AgentRepository.read(c,"agent-candidates.json") ?: "{}") }.getOrDefault(JSONObject())
    private fun save(c: Context,b: JSONObject)=BufferedState.write(c,"agent-candidates.json",b.toString(),true)
    fun offer(c: Context,channels: List<PlaylistChannel>)=synchronized(lock) {
        val known=SourceTools.parse(AgentRepository.read(c,"agent-validated.m3u").orEmpty()).flatMap { it.urls }.toSet()
        val book=read(c);val blocked=MemoryStore(c).let { it.adIds()+it.coolingIds() }
        CandidatePolicy.add(book,channels.map { ch -> ch.copy(urls=ch.urls.filterNot { it in known || MemoryPolicy.id(it) in blocked }) },System.currentTimeMillis(),RecoveryRequests.shared.pending(System.currentTimeMillis())); save(c,book)
    }
    suspend fun drain(c: Context) {
        val watching=DecoderGate.foregroundWanted();val end=SystemClock.elapsedRealtime()+if(watching) 75_000 else 180_000
        var requests=0;var bytes=0;var exhausted=false;val good=mutableListOf<PlaylistChannel>();val hls=mutableSetOf<String>()
        val waitingForRepair=PlaybackRoute.url.isBlank() && RecoveryRequests.shared.pending(System.currentTimeMillis()).isNotEmpty()
        val ids=synchronized(lock) { CandidatePolicy.due(read(c),System.currentTimeMillis(),PlaybackRoute.url,RecoveryRequests.shared.pending(System.currentTimeMillis())).take(if(watching && !waitingForRepair) 2 else 8) }
        try {
            for(id in ids) {
                currentCoroutineContext().ensureActive()
                val item=synchronized(lock) { read(c).optJSONObject(id) } ?: continue
                val url=item.getString("url")
                val region=IptvOrgPolicy.country(item.optString("group"))
                if(region!=null && region !in IptvOrgStore.selected(c) && url!=PlaybackRoute.url) {
                    synchronized(lock) {val b=read(c);b.remove(id);save(c,b)};continue
                }
                if(url==PlaybackRoute.url || !AgentConfigStore(c).load().enabled) break
                if(MemoryPolicy.id(url) in MemoryStore(c).adIds()) { synchronized(lock) { val b=read(c);b.remove(id);save(c,b) };continue }
                fun allowed() {
                    if(PlaybackRoute.buffering || SystemClock.elapsedRealtime()>=end || requests>=16 || bytes>=512*1024 || !watching && DecoderGate.foregroundWanted()) { exhausted=true;throw ScanDeferred("轻量验证预算已用完，稍后继续") }
                    if(url==PlaybackRoute.url) { exhausted=true;throw ScanDeferred("跳过正在观看的线路") }
                }
                fun fetch(address: String): HttpFetch.Reply {
                    allowed();requests++
                    return HttpFetch.get(address,32*1024,sample=true).also { bytes+=it.bytes.size; if(it.bytes.size==32*1024 && SourceTools.isHls(it.bytes.toString(Charsets.UTF_8))) throw HttpFetch.UnverifiedMediaException() }
                }
                val passed=try {
                    val initial=fetch(url)
                    val text=initial.bytes.toString(Charsets.UTF_8)
                    if(!SourceTools.isHls(text)) SourceTools.parse(text).takeIf { it.isNotEmpty() }?.let { offer(c,it) }
                    val result=LiveStreamVerifier.check(url,initial,::allowed,::fetch)
                    if(result.passed && result.hls) hls.add(url)
                    result.passed
                } catch(e: CancellationException) { throw e }
                catch(_: Exception) { false }
                if(exhausted) break
                synchronized(lock) {
                    val b=read(c)
                    if(passed) { good.add(PlaylistChannel(item.getString("title"),item.getString("group"),item.optString("logo"),listOf(url))) }
                    else { CandidatePolicy.failed(b,id,System.currentTimeMillis());MemoryStore(c).sourceFailed(url) }
                    save(c,b)
                }
                if(passed) { MemoryStore(c).sourceSucceeded(url); AgentRepository.recordProbe(c,url,true,0) }
            }
        } finally {
            // Publish only completed successes, including when viewing starts midway through a batch.
            if(good.isNotEmpty()) {
                val old=SourceTools.parse(AgentRepository.read(c,"agent-validated.m3u").orEmpty())
                val prefs=c.getSharedPreferences("agent_state",Context.MODE_PRIVATE)
                val prior=runCatching { AgentConfigStore.strings(org.json.JSONArray(prefs.getString("hlsRoutes","[]"))) }.getOrDefault(emptyList())
                val mime=((if(PlaybackRoute.url in prior) listOf(PlaybackRoute.url) else emptyList())+hls+prior).distinct().take(1000).toMutableList()
                while(org.json.JSONArray(mime).toString().toByteArray(Charsets.UTF_8).size>256*1024) mime.removeAt(mime.lastIndex)
                check(prefs.edit().putString("hlsRoutes",org.json.JSONArray(mime).toString()).commit())
                val protected=old.filter { PlaybackRoute.url in it.urls }
                var remaining=1024
                val bounded=PlaylistParser.merge(listOf(protected,good,old),8).mapNotNull { ch ->
                    val urls=ch.urls.take(remaining.coerceAtLeast(0));remaining-=urls.size
                    ch.copy(urls=urls).takeIf { urls.isNotEmpty() }
                }.toMutableList()
                while(SourceTools.encode(bounded).toByteArray(Charsets.UTF_8).size>1024*1024) bounded.removeAt(bounded.lastIndex)
                AgentRepository.write(c,"agent-validated.m3u",SourceTools.encode(bounded))
                synchronized(lock) { val b=read(c);good.flatMap { it.urls }.forEach { b.remove(MemoryPolicy.id(it)) };save(c,b) }
                AgentRepository.memoryChanged(c)
                AgentRepository.log(c,"逐条直播验证通过 "+good.size+" 条，已加入频道；其余候选不进入播放列表")
            }
        }
    }
    suspend fun sweep(c: Context) {
        val state=c.getSharedPreferences("agent_state",Context.MODE_PRIVATE)
        val priorHealth=AgentRepository.health(c);val now=System.currentTimeMillis()
        val urls=com.lizongying.mytv.TVList.load(c,64,true).flatMap { it.urls }.distinct().sorted().filterNot { u -> u==PlaybackRoute.url || priorHealth[u]?.let { it.manualRemoval || it.removedAt>0 && now-it.checkedAt<6*3_600_000L }==true }
        if(urls.isEmpty()) return
        val selection=LightHealthSelection.select(urls,priorHealth,state.getInt("lightHealthCursor",0),PlaybackRoute.url,System.currentTimeMillis())
        val results=mutableListOf<HttpFetch.Check>()
        var checked=0
        val deadline=SystemClock.elapsedRealtime()+75_000
        for(i in selection.first.indices) {
            if(PlaybackRoute.buffering || SystemClock.elapsedRealtime()>=deadline) break
            checked++
            currentCoroutineContext().ensureActive()
            val url=selection.first[i];if(url==PlaybackRoute.url) continue
            var count=0
            try {
                fun fetch(u: String): HttpFetch.Reply { if(PlaybackRoute.buffering) throw ScanDeferred("观看缓冲优先");if(++count>6) throw HttpFetch.UnverifiedMediaException();return HttpFetch.get(u,32*1024,sample=true).also { if(it.bytes.size==32*1024 && SourceTools.isHls(it.bytes.toString(Charsets.UTF_8))) throw HttpFetch.UnverifiedMediaException() } }
                val result=LiveStreamVerifier.check(url,fetch(url),{ if(url==PlaybackRoute.url) throw ScanDeferred("当前播放线路") },::fetch)
                if(result.passed) results.add(HttpFetch.Check(url,true,true,0))
                else if(result.conclusive) results.add(HttpFetch.Check(url,false,true,0))
            } catch(e: CancellationException) { throw e }
            catch(_: HttpFetch.UnverifiedMediaException) { }
            catch(_: ScanDeferred) { }
            catch(_: Exception) { results.add(HttpFetch.Check(url,false,true,0)) }
        }
        val evidence=results.any { it.failed } && HealthScanner.canRecordFailures(c,results.any { it.passed })
        results.filter { it.passed || evidence }.forEach { if(it.url!=PlaybackRoute.url) AgentRepository.recordProbe(c,it.url,it.passed,it.latency) }
        val reviewed=selection.first.take(3).filter { u -> priorHealth[u]?.let { it.failures in 1..2 && it.removedAt==0L && now-it.lastFailureAt>=HealthPolicy.RETRY_GAP_MS }==true }.toSet()
        val lastNew=selection.first.take(checked).lastOrNull { it !in reviewed }
        val next=if(checked==selection.first.size) selection.second else lastNew?.let { (urls.indexOf(it)+1)%urls.size } ?: state.getInt("lightHealthCursor",0)
        state.edit().putInt("lightHealthCursor",next).apply()
    }
}
