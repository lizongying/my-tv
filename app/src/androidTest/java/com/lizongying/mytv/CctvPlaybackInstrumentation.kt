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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Exercises the actual bundled catalogue and actual channel click callback. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class CctvPlaybackInstrumentation: Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments);start() }
    override fun onStart() {
        val result=Bundle();val report=JSONObject();val checks=JSONArray();report.put("channels",checks)
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
                    row.put("video_width",player.videoSize.width).put("video_height",player.videoSize.height).put("audio_mime",player.audioFormat?.sampleMimeType)
                    check(player.audioFormat!=null) { "未选择音频轨道" }
                }
                SystemClock.sleep(12000)
                onMain {
                    val player=pf.get(v) as ExoPlayer
                    check(player.isPlaying && a.currentStreamUrl()==route) { "连续播放失败" }
                    player.videoDecoderCounters?.ensureUpdated()
                    val advanced=(player.videoDecoderCounters?.renderedOutputBufferCount ?: 0)-frames
                    check(advanced>=30) { "视频未持续输出画面" };row.put("rendered_frames_in_12s",advanced)
                    player.audioDecoderCounters?.ensureUpdated()
                    val audioBuffers=player.audioDecoderCounters?.renderedOutputBufferCount ?: 0
                    check(audioBuffers>=10) { "音频没有持续解码输出" };row.put("decoded_audio_buffers",audioBuffers)
                }
                val image=checkNotNull(uiAutomation.takeScreenshot());val name="cctv-playback-"+round+".png"
                try { File(c.getExternalFilesDir(null),name).outputStream().use { check(image.compress(Bitmap.CompressFormat.PNG,100,it)) } } finally { image.recycle() }
                row.put("url_host",android.net.Uri.parse(route).host).put("url_path",android.net.Uri.parse(route).path).put("screenshot",name).put("passed",true)
            }
            report.put("success",true);result.putString("playback",report.toString());finish(Activity.RESULT_OK,result)
        } catch(e: Throwable) { report.put("success",false).put("error",e.toString());result.putString("playback",report.toString());finish(Activity.RESULT_CANCELED,result) }
        finally { activity?.let { a -> onMain { a.finish() } };prefs.edit().putString("config",old).commit() }
    }
    private fun onMain(block: ()->Unit) {
        var failure: Throwable?=null
        runOnMainSync { try { block() } catch(e: Throwable) { failure=e } }
        failure?.let { throw it }
    }
    private fun await(ms: Long,test: ()->Boolean) { val end=SystemClock.elapsedRealtime()+ms;while(!test()) { check(SystemClock.elapsedRealtime()<end) { "等待实际频道出帧超时" };SystemClock.sleep(100) } }
}
