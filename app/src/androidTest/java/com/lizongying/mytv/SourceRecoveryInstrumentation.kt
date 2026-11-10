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
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.net.URI

/** Simulated expired old routes, actual public feed fetch/admission and actual replacement decoding. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class SourceRecoveryInstrumentation: Instrumentation() {
 override fun onCreate(arguments: Bundle?) { super.onCreate(arguments);start() }
 override fun onStart() {
  val c=targetContext;val report=JSONObject();var resultCode=Activity.RESULT_CANCELED;val result=Bundle();var activity: MainActivity?=null
  val config=c.getSharedPreferences("agent_config",Context.MODE_PRIVATE);val previous=config.getString("config",null)
  val health=read(c,"agent-health.json");val validated=read(c,"agent-validated.m3u");val candidates=read(c,"agent-candidates.json")
  try {
   config.edit().putString("config","{\"enabled\":false,\"aiEnabled\":false}").commit();AgentScheduler.schedule(c)
   val before=PlaylistParser.parse(context.assets.open("recovery-baseline-v1142.m3u").bufferedReader().use { it.readText() },TVList.channelNames(c),64).first { it.title=="CCTV1" }.urls
   report.put("baseline_catalogue","bundled CCTV routes from 1.14.2").put("replacement_may_also_be_bundled_in_current_release",true)
   fun endpoint(url: String): String { val u=URI(url);return u.host+":"+u.port+u.path }
   val points=before.map(::endpoint).toSet()
   @Suppress("UNCHECKED_CAST")
   val sources=TVList::class.java.getDeclaredField("sources").apply { isAccessible=true }.get(TVList) as List<Any>
   @Suppress("UNCHECKED_CAST")
   val feeds=sources.first().javaClass.getDeclaredField("urls").apply { isAccessible=true }.get(sources.first()) as List<String>
   var usedFeed="";var discovered=0
   val attempts=JSONArray();report.put("feed_attempts",attempts)
   for(feed in feeds.sortedBy { !it.contains("best-fan/iptv-sources") }) {
    val attempt=JSONObject().put("feed",feed);attempts.put(attempt)
    try {
     val text=HttpFetch.text(feed);attempt.put("downloaded_chars",text.length)
     val found=PlaylistParser.parse(text,TVList.channelNames(c),64).firstOrNull { it.title=="CCTV1" } ?: continue
     val fresh=found.urls.filter { endpoint(it) !in points }.sortedBy { val path=URI(it).path;!path.contains("/hls/") && !path.contains("/tsfile/") }.take(32)
     attempt.put("different_endpoints",fresh.size)
     if(fresh.isEmpty()) continue
     write(c,"agent-candidates.json","{}")
     RecoveryRequests.shared.request("CCTV1",System.currentTimeMillis())
     config.edit().putString("config","{\"enabled\":true,\"aiEnabled\":false}").commit()
     CandidateAdmission.offer(c,listOf(found.copy(urls=fresh)))
     runBlocking {
      repeat(3) {
       if(AgentRepository.extraChannels(c).none { it.title=="CCTV1" && it.urls.any { url -> endpoint(url) !in points } }) CandidateAdmission.drain(c)
      }
     }
     if(AgentRepository.extraChannels(c).any { it.title=="CCTV1" && it.urls.any { url -> endpoint(url) !in points } }) { usedFeed=feed;discovered=fresh.size;break }
    } catch(e: Exception) { attempt.put("error_type",e.javaClass.simpleName).put("error",e.message?.take(140)) }
   }
   check(usedFeed.isNotEmpty()) { "公开订阅未找到可验证的不同端点替代源" }
   val good=AgentRepository.extraChannels(c).first { it.title=="CCTV1" }.urls.filter { endpoint(it) !in points }
   check(good.isNotEmpty())
   report.put("public_feed_fetched",usedFeed).put("new_candidates",discovered).put("candidate_admitted",good.size)
   // This expiry state is a fixture. The replacement itself must come from the network.
   val expired=JSONObject(health ?: "{}")
   before.forEach { expired.put(it,HealthCodec.write(HealthPolicy.manuallyRemove(null,System.currentTimeMillis(),"测试模拟旧源失效"))) }
   write(c,"agent-health.json",expired.toString())
   config.edit().putString("config","{\"enabled\":false,\"aiEnabled\":false}").commit()
   activity=startActivitySync(Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
   val a=activity;var main: MainFragment?=null;var video: PlayerFragment?=null
   await(30000) { onMain { main=a.supportFragmentManager.fragments.filterIsInstance<MainFragment>().firstOrNull();video=a.supportFragmentManager.fragments.filterIsInstance<PlayerFragment>().firstOrNull() };main?.tvListViewModel?.size()?.let { it>0 }==true }
   val m=main!!;val v=video!!;val field=PlayerFragment::class.java.getDeclaredField("player").apply { isAccessible=true };val attemptField=PlayerFragment::class.java.getDeclaredField("attempt").apply { isAccessible=true }
   onMain {
    val model=m.tvListViewModel.tvListViewModel.value.orEmpty().first { it.getTV().title=="CCTV1" }
    if(m.isHidden) a.supportFragmentManager.beginTransaction().show(m).commitNow()
    m.onItemViewClickedListener!!.onItemClicked(Presenter.ViewHolder(View(a)),model,RowPresenter.ViewHolder(View(a)),ListRow(HeaderItem(0,"央视"),ArrayObjectAdapter()))
   }
   await(60000) { var ok=false;onMain { val player=field.get(v) as ExoPlayer;ok=(attemptField.get(v) as PlaybackAttempt).hasFirstFrame && a.currentStreamUrl() in good && player.isPlaying && player.audioFormat!=null };ok }
   var initial=0;onMain { val player=field.get(v) as ExoPlayer;player.videoDecoderCounters?.ensureUpdated();initial=player.videoDecoderCounters?.renderedOutputBufferCount ?: 0 }
   SystemClock.sleep(12000)
   onMain {
    val player=field.get(v) as ExoPlayer;player.videoDecoderCounters?.ensureUpdated();player.audioDecoderCounters?.ensureUpdated()
    val count=(player.videoDecoderCounters?.renderedOutputBufferCount ?: 0)-initial
    report.put("actual_frames",count).put("playing",player.isPlaying).put("state",player.playbackState).put("audio_buffers",player.audioDecoderCounters?.renderedOutputBufferCount ?: 0).put("video_width",player.videoSize.width).put("route_host",URI(a.currentStreamUrl()).host)
    check(count>=30 && player.isPlaying && (player.audioDecoderCounters?.renderedOutputBufferCount ?: 0)>=10)
    report.put("replacement_frames_in_12s",count).put("replacement_host",URI(a.currentStreamUrl()).host).put("replacement_path",URI(a.currentStreamUrl()).path)
   }
   val image=checkNotNull(uiAutomation.takeScreenshot());try {File(c.getExternalFilesDir(null),"source-recovery.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }} finally {image.recycle()}
   report.put("success",true);result.putString("playback",report.toString());resultCode=Activity.RESULT_OK
  } catch(e: Throwable) { report.put("success",false).put("error",e.toString());result.putString("playback",report.toString());resultCode=Activity.RESULT_CANCELED }
  finally { activity?.let { a -> onMain {a.finish()} };write(c,"agent-health.json",health ?: "{}");write(c,"agent-validated.m3u",validated ?: "#EXTM3U\n");write(c,"agent-candidates.json",candidates ?: "{}");config.edit().putString("config",previous).commit();RecoveryRequests.shared.complete("CCTV1") }
  finish(resultCode,result)
 }
 private fun read(c: Context,name: String): String? = AgentRepository::class.java.declaredMethods.first { it.name.startsWith("read$") && it.parameterCount==2 }.apply { isAccessible=true }.invoke(AgentRepository,c,name) as String?
 private fun write(c: Context,name: String,value: String) { AgentRepository::class.java.declaredMethods.first { it.name.startsWith("write$") && it.parameterCount==3 }.apply { isAccessible=true }.invoke(AgentRepository,c,name,value) }
 private fun onMain(block: ()->Unit) {var error: Throwable?=null;runOnMainSync {try {block()} catch(e:Throwable) {error=e}};error?.let {throw it}}
 private fun await(ms:Long,test:()->Boolean) {val end=SystemClock.elapsedRealtime()+ms;while(!test()){check(SystemClock.elapsedRealtime()<end){"等待替代源出帧超时"};SystemClock.sleep(100)}}
}
