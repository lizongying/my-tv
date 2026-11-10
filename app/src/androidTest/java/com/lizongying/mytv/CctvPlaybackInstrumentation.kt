package com.lizongying.mytv
import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.leanback.widget.*
import androidx.media3.exoplayer.ExoPlayer
import com.lizongying.mytv.agent.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Exercises the actual bundled catalogue and actual channel click callback. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class CctvPlaybackInstrumentation: Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments);start() }
    override fun onStart() {
        var resultCode=Activity.RESULT_CANCELED;val result=Bundle();val report=JSONObject();val checks=JSONArray();report.put("channels",checks)
        val c=targetContext;val prefs=c.getSharedPreferences("agent_config",Context.MODE_PRIVATE);val old=prefs.getString("config",null)
        var activity: MainActivity?=null
        try {
            prefs.edit().putString("config",JSONObject().put("enabled",false).put("aiEnabled",false).toString()).commit();AgentScheduler.schedule(c)
            activity=startActivitySync(Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            val a=activity
            var main: MainFragment?=null;var video: PlayerFragment?=null
            await(30000) { onMain { main=a.supportFragmentManager.fragments.filterIsInstance<MainFragment>().firstOrNull();video=a.supportFragmentManager.fragments.filterIsInstance<PlayerFragment>().firstOrNull() };main?.tvListViewModel?.size()?.let { it>0 }==true && video!=null }
            val m=main!!;val v=video!!
            val pf=PlayerFragment::class.java.getDeclaredField("player").apply { isAccessible=true }
            val af=PlayerFragment::class.java.getDeclaredField("attempt").apply { isAccessible=true }
            for((round,title) in listOf("CCTV1","CCTV6","CCTV1").withIndex()) {
                val row=JSONObject().put("title",title);checks.put(row);val started=SystemClock.elapsedRealtime()
                onMain {
                    val model=m.tvListViewModel.tvListViewModel.value.orEmpty().first { it.getTV().title==title }
                    if(m.isHidden) a.supportFragmentManager.beginTransaction().show(m).commitNow()
                    m.onItemViewClickedListener!!.onItemClicked(Presenter.ViewHolder(View(a)),model,RowPresenter.ViewHolder(View(a)),ListRow(HeaderItem(0,"央视"),ArrayObjectAdapter()))
                }
                await(50000) { var ready=false;onMain { val attempt=af.get(v) as PlaybackAttempt;ready=attempt.hasFirstFrame && !attempt.failed && (pf.get(v) as ExoPlayer).isPlaying && (pf.get(v) as ExoPlayer).audioFormat!=null };ready }
                row.put("first_frame_ms",SystemClock.elapsedRealtime()-started)
                var frames=0;var route=""
                onMain {
                    val player=pf.get(v) as ExoPlayer
                    route=a.currentStreamUrl();player.videoDecoderCounters?.ensureUpdated();frames=player.videoDecoderCounters?.renderedOutputBufferCount ?: 0
                    check(route in m.tvListViewModel.tvListViewModel.value.orEmpty().first { it.getTV().title==title }.videoUrl.value.orEmpty())
                    row.put("url_host",android.net.Uri.parse(route).host).put("video_width",player.videoSize.width).put("video_height",player.videoSize.height).put("audio_mime",player.audioFormat?.sampleMimeType)
                    check(player.audioFormat!=null) { "未选择音频轨道" }
                }
                val samples=JSONArray();row.put("watch_samples",samples)
                var totalFrames=0;var previousCount=frames;var observedRoute=route;var switches=0;var playingSeconds=0;var gap=0;var longestGap=0
                repeat(30) {
                    SystemClock.sleep(1000)
                    onMain {
                        val player=pf.get(v) as ExoPlayer
                        player.videoDecoderCounters?.ensureUpdated()
                        val active=a.currentStreamUrl();val count=player.videoDecoderCounters?.renderedOutputBufferCount ?: 0
                        if(active!=observedRoute) { totalFrames+=count;switches++;observedRoute=active } else totalFrames+=(count-previousCount).coerceAtLeast(0)
                        previousCount=count
                        if(player.isPlaying) {
                            check(active in m.tvListViewModel.tvListViewModel.value.orEmpty().first { it.getTV().title==title }.videoUrl.value.orEmpty()) { "切源后频道不匹配" }
                            playingSeconds++;gap=0
                        } else { gap++;longestGap=maxOf(longestGap,gap) }
                        samples.put(JSONObject().put("second",it+1).put("playing",player.isPlaying).put("state",player.playbackState).put("when_ready",player.playWhenReady).put("route_changed",a.currentStreamUrl()!=route).put("frames",player.videoDecoderCounters?.renderedOutputBufferCount ?: 0))
                    }
                }
                onMain {
                    val player=pf.get(v) as ExoPlayer
                    row.put("playing_seconds",playingSeconds).put("longest_interruption_seconds",longestGap).put("route_switches",switches).put("initial_url_host",android.net.Uri.parse(route).host)
                    check(player.isPlaying && playingSeconds>=25 && longestGap<=5) { "观看中断过长" }
                    route=observedRoute
                    player.videoDecoderCounters?.ensureUpdated()
                    val advanced=totalFrames
                    check(advanced>=60) { "视频未持续输出画面" };row.put("rendered_frames_in_30s",advanced).put("watch_ms",30000)
                    player.audioDecoderCounters?.ensureUpdated()
                    val audioBuffers=player.audioDecoderCounters?.renderedOutputBufferCount ?: 0
                    check(audioBuffers>=10) { "音频没有持续解码输出" };row.put("decoded_audio_buffers",audioBuffers)
                }
                val image=checkNotNull(uiAutomation.takeScreenshot());val name="cctv-playback-"+round+".png"
                try { File(c.getExternalFilesDir(null),name).outputStream().use { check(image.compress(Bitmap.CompressFormat.PNG,100,it)) } } finally { image.recycle() }
                row.put("url_host",android.net.Uri.parse(route).host).put("url_path",android.net.Uri.parse(route).path).put("screenshot",name).put("passed",true)
            }
            // Act like a viewer switching their mind quickly; only the last choice wins.
            repeat(8) { index ->
                onMain {
                    val title=if(index%2==0) "CCTV6" else "CCTV1"
                    val model=m.tvListViewModel.tvListViewModel.value.orEmpty().first { it.getTV().title==title }
                    if(m.isHidden) a.supportFragmentManager.beginTransaction().show(m).commitNow()
                    m.onItemViewClickedListener!!.onItemClicked(Presenter.ViewHolder(View(a)),model,RowPresenter.ViewHolder(View(a)),ListRow(HeaderItem(0,"央视"),ArrayObjectAdapter()))
                }
                SystemClock.sleep(180)
            }
            await(50000) { var ok=false;onMain { ok=(af.get(v) as PlaybackAttempt).hasFirstFrame && (pf.get(v) as ExoPlayer).isPlaying };ok }
            val syncStarted=SystemClock.elapsedRealtime()
            val sync=runBlocking { TVList.refresh(c,true,true) }
            check(sync.updated>0) { "观看中联网刷新节目单未成功" }
            report.put("watching_subscription_sync",true).put("subscription_sync_updates",sync.updated).put("subscription_sync_ms",SystemClock.elapsedRealtime()-syncStarted)
            onMain {
                val url=a.currentStreamUrl()
                check(url in m.tvListViewModel.tvListViewModel.value.orEmpty().first { it.getTV().title=="CCTV1" }.videoUrl.value.orEmpty())
                MainFragment::class.java.getDeclaredMethod("reloadMaintainedChannels").apply { isAccessible=true }.invoke(m)
            }
            SystemClock.sleep(2000)
            onMain { check((pf.get(v) as ExoPlayer).isPlaying) }
            report.put("rapid_switch_last_choice_wins",true).put("catalogue_refresh_keeps_video",true)
            onMain { a.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            SystemClock.sleep(2000)
            onMain { check(!(pf.get(v) as ExoPlayer).isPlaying && !DecoderGate.foregroundWanted()) }
            onMain { c.startActivity(Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)) }
            await(50000) { var ok=false;onMain { ok=a.hasWindowFocus() && (af.get(v) as PlaybackAttempt).hasFirstFrame && (pf.get(v) as ExoPlayer).isPlaying };ok }
            report.put("desktop_stops_playback_and_return_resumes",true)
            report.put("success",true);result.putString("playback",report.toString());resultCode=Activity.RESULT_OK
        } catch(e: Throwable) { report.put("success",false).put("error",e.toString());result.putString("playback",report.toString());resultCode=Activity.RESULT_CANCELED }
        finally { activity?.let { a -> onMain { a.finish() } };prefs.edit().putString("config",old).commit() }
        finish(resultCode,result)
    }
    private fun onMain(block: ()->Unit) {
        var failure: Throwable?=null
        runOnMainSync { try { block() } catch(e: Throwable) { failure=e } }
        failure?.let { throw it }
    }
    private fun await(ms: Long,test: ()->Boolean) { val end=SystemClock.elapsedRealtime()+ms;while(!test()) { check(SystemClock.elapsedRealtime()<end) { "等待实际频道出帧超时" };SystemClock.sleep(100) } }
}
