package com.lizongying.mytv

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.work.WorkManager
import com.lizongying.mytv.agent.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Run on an offline emulator: no key, network search or live programme requests. */
class BackgroundHandoffInstrumentation: Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result=Bundle()
        val c=targetContext; val wm=WorkManager.getInstance(c)
        val prefs=c.getSharedPreferences("agent_config",Context.MODE_PRIVATE)
        val old=prefs.getString("config",null)
        val owner=Any()
        try {
            check(AgentConfigStore(c).apiKey().isBlank())
            DecoderGate.requestForeground(owner)
            val catalog=RepositoryCrawler.catalogue("https://jihulab.com/owen2000wy/owentv/-/raw/main/HP20230319.txt")!!
            val catalogueLinks=RepositoryCrawler.links(catalog,"[{\"type\":\"blob\",\"path\":\"HP20230319.txt\"},{\"type\":\"blob\",\"path\":\"Other.m3u\"}]")
            check(catalogueLinks.size==2 && catalogueLinks.all { it.kind=="playlist" })
            val cataloguePlan=JSONObject()
            catalogueLinks.forEach { check(StationCrawler.enqueue(cataloguePlan,it,catalog,2,catalog,System.currentTimeMillis())) }
            AgentRepository.write(c,"catalogue-fixture.json",DiscoveryPolicy.bound(cataloguePlan).toString())
            check(JSONObject(AgentRepository.read(c,"catalogue-fixture.json")!!).getJSONArray("queue").length()==2)
            check(SourceTools.parse("央视,#genre#\nCCTV1,https://example.org/live.m3u8\n山东卫视,https://example.org/other.m3u8").size==2)
            result.putString("catalogue","PLAYLIST_CATALOGUE_OK: raw URL discovers repository, other playlist files discovered, persisted queue and TXT parsing without AI")
            val logoNames=listOf("央视新闻", "北京卫视", "山东卫视")
            prefs.edit().putString("config",JSONObject().put("enabled",true).put("aiEnabled",false).put("logoLookupEnabled",true).put("logoImageMb",2).toString()).commit()
            RealLogoStore.enqueue(c,logoNames.map { it to null })
            check(RealLogoStore.pending(c).none { it.first in logoNames })
            val preview=android.graphics.Bitmap.createBitmap(900,120,android.graphics.Bitmap.Config.ARGB_8888)
            runOnMainSync {
                val canvas=android.graphics.Canvas(preview);canvas.drawColor(0xFF647580.toInt())
                val image=android.widget.ImageView(c)
                logoNames.forEachIndexed { i,name ->
                    val real=RealLogoStore.bitmap(c,name)!!;check(ChannelLogo.useful(real))
                    ChannelLogo.bind(image,name,R.drawable.logo)
                    check(image.drawable is android.graphics.drawable.BitmapDrawable && image.contentDescription==name)
                    check((image.drawable as android.graphics.drawable.BitmapDrawable).bitmap.sameAs(real))
                    image.drawable.setBounds(i*300,0,(i+1)*300,120);image.drawable.draw(canvas)
                    real.recycle()
                }
                val blank=android.graphics.Bitmap.createBitmap(64,32,android.graphics.Bitmap.Config.ARGB_8888)
                check(!ChannelLogo.useful(blank));blank.eraseColor(android.graphics.Color.WHITE);check(!ChannelLogo.useful(blank))
                android.graphics.Canvas(blank).drawRect(0f,0f,32f,32f,android.graphics.Paint().apply { color=android.graphics.Color.RED })
                check(ChannelLogo.useful(blank));blank.recycle()
                ChannelLogo.clear(image)
            }
            java.io.File(c.filesDir,"logo-preview.png").outputStream().use { preview.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };preview.recycle()
            val index=JSONObject(c.assets.open("real-logo-index.json").bufferedReader().use { it.readText() })
            val asset=index.getJSONObject("CCTV1").getString("asset")
            val png=c.assets.open(asset).use { it.readBytes() }
            check(RealLogoStore.decode("<html>not an image</html>".toByteArray())==null)
            check(RealLogoStore.decode(ByteArray(256*1024+1))==null)
            check(RealLogoStore.put(c,"下载台标示例","https://example.org/real.png",png))
            check(RealLogoStore.bitmap(c,"下载台标示例")!=null)
            check(AgentRepository.read(c,"channel-logos.json")!!.toByteArray().size<=128*1024)
            // Lowering the image cap clears the oldest downloads without touching packaged logos.
            val cache=java.io.File(c.filesDir,"channel-logos")
            for(i in 0..2) java.io.File(cache,"cache-fixture-$i.png").writeBytes(ByteArray(1024*1024))
            RealLogoStore.applyLimit(c)
            await { cache.listFiles().orEmpty().sumOf { it.length() }<=2*1024*1024 }
            check(RealLogoStore.bitmap(c,"央视新闻")!=null)
            lateinit var failedImage: android.widget.ImageView
            val failedUrl="https://example.org/offline-fixture.png"
            runOnMainSync { failedImage=android.widget.ImageView(c);ChannelLogo.bind(failedImage,"失效台标示例",failedUrl) }
            await { RealLogoStore.isFailed(c,failedUrl) }
            runOnMainSync {
                check(failedImage.drawable !is android.graphics.drawable.BitmapDrawable)
                ChannelLogo.bind(failedImage,"央视新闻",null)
                check(failedImage.contentDescription=="央视新闻" && failedImage.drawable is android.graphics.drawable.BitmapDrawable)
                ChannelLogo.bind(failedImage,"未知频道",null)
                check(failedImage.contentDescription=="未知频道" && failedImage.drawable !is android.graphics.drawable.BitmapDrawable)
                ChannelLogo.clear(failedImage)
            }
            LogoScheduler.schedule(c)
            check(wm.getWorkInfosForUniqueWork("channel-logo-periodic").get(15,TimeUnit.SECONDS).any { !it.state.isFinished })
            prefs.edit().putString("config",JSONObject().put("enabled",true).put("aiEnabled",false).put("logoLookupEnabled",false).toString()).commit()
            LogoScheduler.schedule(c)
            await { wm.getWorkInfosForUniqueWork("channel-logo-periodic").get(5,TimeUnit.SECONDS).all { it.state.isFinished } }
            result.putString("logos","REAL_CHANNEL_LOGOS_OK: genuine bundled PNG identity, UI rendering, image cache, invalid content rejection, cache cap, failed URL memory, recycled views, no-key background scheduling and disable")
            prefs.edit().putString("config",JSONObject().put("enabled",true).put("aiEnabled",false).toString()).commit()
            AgentScheduler.schedule(c)
            AgentScheduler.playbackStopped(c)
            val one=wm.getWorkInfosForUniqueWork("source-agent-idle").get(15,TimeUnit.SECONDS)
            check(one.any { !it.state.isFinished }) { "退出播放没有持久化找源任务" }
            AgentScheduler.playbackStopped(c)
            check(wm.getWorkInfosForUniqueWork("source-agent-idle").get(15,TimeUnit.SECONDS).map { it.id }==one.map { it.id }) { "重复退出创建了重复任务" }
            AgentScheduler.playbackStarted(c)
            await { wm.getWorkInfosForUniqueWork("source-agent-idle").get(5,TimeUnit.SECONDS).all { it.state.isFinished } }
            check(wm.getWorkInfosForUniqueWork("source-agent-periodic").get(15,TimeUnit.SECONDS).any { !it.state.isFinished })
            DecoderGate.releaseForeground(owner)
            val plan=JSONObject(); DiscoveryPolicy.add(plan,listOf("https://example.org/root","https://example.org/child.m3u"),System.currentTimeMillis())
            DiscoveryPolicy.done(plan,"https://example.org/root",System.currentTimeMillis())
            AgentRepository.write(c,"agent-discovery.json",DiscoveryPolicy.bound(plan).toString())
            val restored=JSONObject(AgentRepository.read(c,"agent-discovery.json")!!)
            check(DiscoveryPolicy.next(restored)=="https://example.org/child.m3u")
            prefs.edit().putString("config",JSONObject().put("enabled",false).toString()).commit()
            AgentScheduler.schedule(c)
            await { listOf("source-agent-periodic","source-agent-once","source-agent-idle").all { name ->
                wm.getWorkInfosForUniqueWork(name).get(5,TimeUnit.SECONDS).all { it.state.isFinished }
            } }
            result.putString("scheduler","BACKGROUND_SCHEDULING_OK: unique idle work, viewing cancellation, periodic persistence, disable cancels all, frontier checkpoint restored")
            // Exercise the actual PlayerFragment lifecycle through the main screen.
            var activity=startActivitySync(Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            waitForIdleSync(); await { DecoderGate.foregroundWanted() }
            runOnMainSync { activity.finish() }; waitForIdleSync()
            await { !DecoderGate.foregroundWanted() }
            prefs.edit().putString("config",JSONObject().put("enabled",true).put("aiEnabled",false).toString()).commit()
            activity=startActivitySync(Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            waitForIdleSync(); await { DecoderGate.foregroundWanted() }
            runOnMainSync { activity.finish() }; waitForIdleSync()
            await { !DecoderGate.foregroundWanted() }
            await { wm.getWorkInfosForUniqueWork("source-agent-idle").get(5,TimeUnit.SECONDS).any { !it.state.isFinished } }
            result.putString("lifecycle","PLAYER_TO_DISCOVERY_HANDOFF_OK: actual activity stop releases playback and enqueues idle discovery")
            prefs.edit().putString("config",JSONObject().put("enabled",false).put("aiEnabled",false).toString()).commit()
            AgentScheduler.schedule(c)
            runBlocking { withContext(Dispatchers.IO) {
                val calls=AtomicInteger()
                val page="https://station.example.org/watch"
                val stream="https://cdn.example.org/play?id=2&token=fixture-only"
                val html="""<html><head><title>CCTV-2 财经直播</title><script src="/player.js"></script></head><body><a href="/live/cctv3/">CCTV-3</a><script src="http://127.0.0.1/secret.js"></script></body></html>"""
                val captured=WebPageResolver(c).resolve(page,html,"CCTV2",{}, { url,headers ->
                    check(!url.contains("127.0.0.1")) { "不应请求本地地址" }
                    check(headers.keys.none { it.equals("Authorization",true) || it.equals("Cookie",true) })
                    calls.incrementAndGet()
                    val text=when {
                        url.endsWith("player.js") -> "fetch('/api/resolve').then(function(r){return r.json();}).then(function(d){fetch(d.address).then(function(){for(var i=0;i<60;i++){fetch('/ping/'+i);}});});"
                        url.endsWith("api/resolve") -> JSONObject().put("address",stream).toString()
                        url==stream -> "#EXTM3U\n#EXT-X-TARGETDURATION:2\n#EXTINF:2,\nsegment.ts\n"
                        else -> "{}"
                    }
                    HttpFetch.Reply(url,text.toByteArray(),if(url.endsWith(".js")) "application/javascript" else if(url==stream) "application/vnd.apple.mpegurl" else "application/json")
                },45_000)
                check(captured.any { it.url==stream && it.kind=="media" }) { "动态 GET 接口生成的无后缀 HLS 未捕获："+captured }
                check(captured.any { it.url.endsWith("/live/cctv3/") && it.title.contains("CCTV-3") })
                check(calls.get()<=40) { "浏览器请求上限失效" }
                val state=c.getSharedPreferences("agent_state",Context.MODE_PRIVATE)
                state.edit().putString("hlsRoutes",org.json.JSONArray().put(stream).toString()).commit()
                check(AgentRepository.mediaItem(c,stream).localConfiguration?.mimeType=="application/x-mpegURL")
                var priorityChecks=0
                try {
                    WebPageResolver(c).resolve(page,"<html><title>CCTV1</title></html>","CCTV1",{
                        if(++priorityChecks>=2) throw ScanDeferred("观看优先")
                    }, { _,_ -> error("no request") },10_000)
                    error("浏览器没有让出资源")
                } catch(_: ScanDeferred) { check(priorityChecks==2) }
                result.putString("crawler","DYNAMIC_CRAWLER_OK: executed JavaScript, GET API resolution, extensionless HLS capture, channel labels, request cap, private URL blocking, HLS playback MIME, foreground yield")
            } }
            finish(Activity.RESULT_OK,result)
        } catch(e: Throwable) {
            result.putString("stream","HANDOFF_FAILED: "+e.stackTraceToString())
            finish(Activity.RESULT_CANCELED,result)
        } finally {
            DecoderGate.releaseForeground(owner)
            listOf("source-agent-idle","source-agent-once","source-agent-periodic","channel-logo-idle","channel-logo-periodic").forEach(wm::cancelUniqueWork)
            if(old==null) prefs.edit().remove("config").commit() else prefs.edit().putString("config",old).commit()
        }
    }
    private fun await(condition: () -> Boolean) {
        val deadline=SystemClock.elapsedRealtime()+15_000
        while(!condition()) { check(SystemClock.elapsedRealtime()<deadline) { "等待状态超时" }; Thread.sleep(100) }
    }
}
