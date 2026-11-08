package com.lizongying.mytv

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.cancelChildren
import androidx.leanback.widget.*
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.lizongying.mytv.agent.*
import fi.iki.elonen.NanoHTTPD
import java.io.File
import org.json.JSONObject

/** Tests actual click callbacks, HTTP decoding and rendered video; no AI key needed. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackInstrumentation : Instrumentation() {
    private var args=Bundle()
    override fun onCreate(arguments: Bundle?) { args=arguments ?: Bundle(); super.onCreate(arguments); start() }
    override fun onStart() {
        val output=JSONObject(); val c=targetContext
        val prefs=c.getSharedPreferences("agent_config",Context.MODE_PRIVATE)
        val previous=prefs.getString("config",null)
        var activity: MainActivity?=null
        var server: NanoHTTPD?=null
        val file=File(c.cacheDir,"playback-original-test.mp4")
        val result=Bundle()
        try {
            prefs.edit().putString("config",JSONObject().put("enabled",false).put("aiEnabled",false).toString()).commit()
            AgentScheduler.schedule(c)
            SamplerInstrumentation().fixture(file)
            server=object: NanoHTTPD("127.0.0.1",0) {
                override fun serve(session: IHTTPSession): Response {
                    if(session.uri=="/bad.m3u8") SystemClock.sleep(700)
                    return if(session.uri.endsWith(".mp4")) newFixedLengthResponse(Response.Status.OK,"video/mp4",file.inputStream(),file.length())
                    else newFixedLengthResponse(Response.Status.NOT_FOUND,"text/plain","Not a stream")
                }
            }.apply { start(NanoHTTPD.SOCKET_READ_TIMEOUT,false) }
            val base="http://127.0.0.1:"+server.listeningPort
            activity=startActivitySync(Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            val a=activity
            var main: MainFragment?=null; var video: PlayerFragment?=null
            await(30000) {
                runOnMainSync {
                    main=a.supportFragmentManager.fragments.filterIsInstance<MainFragment>().firstOrNull()
                    video=a.supportFragmentManager.fragments.filterIsInstance<PlayerFragment>().firstOrNull()
                }
                main?.tvListViewModel?.size()?.let { it>0 }==true && video!=null
            }
            val m=main!!; val v=video!!
            output.put("default_catalog_channels",m.tvListViewModel.size())
            val playerField=PlayerFragment::class.java.getDeclaredField("player").apply { isAccessible=true }
            val attemptField=PlayerFragment::class.java.getDeclaredField("attempt").apply { isAccessible=true }
            fun attempt()=attemptField.get(v) as PlaybackAttempt
            val routes=listOf(
                PlaylistChannel("CCTV1","央视","",listOf(base+"/a.mp4")),
                PlaylistChannel("CCTV2","央视","",listOf(base+"/b.mp4")),
                PlaylistChannel("CCTV3","央视","",listOf(base+"/offline.m3u8")),
                PlaylistChannel("CCTV4","央视","",listOf(base+"/bad.m3u8",base+"/fallback.mp4"))
            )
            runOnMainSync {
                // Freeze catalogue polling only in this instrumentation fixture. Otherwise
                // the real catalogue correctly replaces URLs that exist only in the test.
                m.viewLifecycleOwner.lifecycleScope.coroutineContext.cancelChildren()
                a.clearPlayback()
                MainFragment::class.java.getDeclaredMethod("loadRows",List::class.java).apply { isAccessible=true }.invoke(m,routes)
                MainFragment::class.java.getDeclaredMethod("observeChannels").apply { isAccessible=true }.invoke(m)
                (playerField.get(v) as ExoPlayer).repeatMode=Player.REPEAT_MODE_ONE
            }
            fun click(index: Int) { runOnMainSync {
                if(m.isHidden) a.supportFragmentManager.beginTransaction().show(m).commitNow()
                val model=m.tvListViewModel.getTVViewModel(index)!!
                m.onItemViewClickedListener!!.onItemClicked(Presenter.ViewHolder(View(a)),model,RowPresenter.ViewHolder(View(a)),ListRow(HeaderItem(0,"央视"),ArrayObjectAdapter()))
            } }
            fun framed(url: String,timeout: Long=15000) { await(timeout) {
                var ok=false;runOnMainSync { ok=attempt().url==url && attempt().hasFirstFrame && !attempt().failed };ok
            } }
            // Start a controlled HTTP video before exercising real menu click callbacks.
            runOnMainSync { m.play(0) }
            framed(base+"/a.mp4")
            output.put("http_video_first_frame",true)
            SystemClock.sleep(600)
            uiAutomation.takeScreenshot()?.let { image ->
                check(android.graphics.Color.red(image.getPixel(image.width/2,image.height/2))>12) { "首帧回调后可见画面仍是黑色" }
                output.put("visible_http_video_pixels",true)
                File(c.filesDir,"playback-local.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; image.recycle()
            }
            click(1);framed(base+"/b.mp4")
            output.put("click_switches_while_address_nonempty",true)
            click(2);await(12000) { var failed=false;runOnMainSync { failed=attempt().url==base+"/offline.m3u8" && attempt().failed };failed }
            click(0);framed(base+"/a.mp4")
            output.put("click_recovers_from_failed_channel",true)
            click(3)
            // A catalogue rebuild removes the current model's observers.
            runOnMainSync { m.tvListViewModel.getTVViewModel(3)!!.change.removeObservers(m.viewLifecycleOwner) }
            framed(base+"/fallback.mp4",20000)
            output.put("failed_route_automatically_uses_backup",true)
            output.put("failover_without_catalogue_observer",true)
            click(2);await(12000) { var failed=false;runOnMainSync { failed=attempt().failed };failed }
            val failedId=attempt().id
            click(2);await(10000) { attempt().id!=failedId }
            output.put("same_channel_explicit_retry",true)
            val live=args.getString("liveUrl").orEmpty()
            if(live.isNotBlank()) {
                runOnMainSync {
                    (playerField.get(v) as ExoPlayer).repeatMode=Player.REPEAT_MODE_OFF
                    a.resetPlaybackFailures()
                    v.play(com.lizongying.mytv.models.TVViewModel(TV(99,"实际直播","实际直播",listOf(live),"地方台",R.drawable.logo,programType=com.lizongying.mytv.models.ProgramType.Y_PROTO)))
                }
                framed(live,40000)
                output.put("real_live_hls_first_frame",true)
                SystemClock.sleep(3500)
                uiAutomation.takeScreenshot()?.let { image ->
                    File(c.filesDir,"playback-live.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; image.recycle()
                }
                runOnMainSync { output.put("real_live_still_playing",(playerField.get(v) as ExoPlayer).isPlaying) }
            }
            c.getExternalFilesDir(null)?.let { folder ->
                listOf("playback-local.png","playback-live.png").forEach { name ->
                    val image=File(c.filesDir,name)
                    if(image.exists()) image.copyTo(File(folder,name),overwrite=true)
                }
            }
            output.put("success",true)
            result.putString("playback",output.toString())
            finish(Activity.RESULT_OK,result)
        } catch(e: Throwable) {
            output.put("success",false).put("error",e.toString())
            activity?.let { a -> runOnMainSync {
                val v=a.supportFragmentManager.fragments.filterIsInstance<PlayerFragment>().firstOrNull()
                if(v!=null) {
                    val field=PlayerFragment::class.java.getDeclaredField("attempt").apply { isAccessible=true }
                    val state=field.get(v) as PlaybackAttempt
                    val pf=PlayerFragment::class.java.getDeclaredField("player").apply { isAccessible=true }
                    val player=pf.get(v) as? ExoPlayer
                    output.put("attempt_id",state.id).put("has_frame",state.hasFirstFrame).put("failed",state.failed)
                        .put("player_state",player?.playbackState).put("play_when_ready",player?.playWhenReady)
                        .put("player_error",player?.playerError?.errorCodeName).put("foreground_wanted",DecoderGate.foregroundWanted())
                        .put("attempt_host",android.net.Uri.parse(state.url).host).put("attempt_path",android.net.Uri.parse(state.url).path)
                }
            } }
            result.putString("playback",output.toString());finish(Activity.RESULT_CANCELED,result)
        } finally {
            activity?.let { a -> runOnMainSync { a.finish() } }
            server?.stop();file.delete()
            prefs.edit().putString("config",previous).commit()
            File(c.filesDir,"playback-validation.json").writeText(output.toString(2))
        }
    }
    private fun await(timeout: Long,condition: ()->Boolean) {
        val deadline=SystemClock.elapsedRealtime()+timeout
        while(!condition()) { check(SystemClock.elapsedRealtime()<deadline) { "等待播放状态超时" };SystemClock.sleep(100) }
    }
}
