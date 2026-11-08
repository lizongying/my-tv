package com.lizongying.mytv

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.lizongying.mytv.databinding.PlayerBinding
import com.lizongying.mytv.models.TVViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.lizongying.mytv.agent.DecoderGate
import com.lizongying.mytv.agent.AutoVisionScheduler
import com.lizongying.mytv.agent.AgentRepository
import com.lizongying.mytv.agent.AgentScheduler
import com.lizongying.mytv.agent.PlaybackAttempt

@OptIn(UnstableApi::class)
class PlayerFragment : Fragment() {
    private var binding: PlayerBinding? = null
    private var player: ExoPlayer? = null
    private var current: TVViewModel? = null
    private val decoderOwner=Any()
    private var visible=false
    private var prepareJob: Job?=null
    private val refreshedDynamic = mutableSetOf<String>()
    private val failedUrls = mutableSetOf<String>()
    private val attempt = PlaybackAttempt()
    private val handler = Handler(Looper.getMainLooper())
    private val stalled = Runnable { tryNextSource("视频缓冲超过 15 秒") }
    private val noFrame = Runnable { tryNextSource("20 秒未显示视频首帧") }
    private val playingFeedback = Runnable {
        if (player?.isPlaying == true && attempt.success(attempt.id)) {
            val url = attempt.url
            context?.applicationContext?.let { app -> lifecycleScope.launch { AgentRepository.recordPlayback(app, url, true) } }
        }
    }
    fun currentStreamUrl(): String = attempt.url
    fun clearPlayback() {
        com.lizongying.mytv.agent.PlaybackRoute.buffering=false
        com.lizongying.mytv.agent.PlaybackRoute.url=""
        prepareJob?.cancel()
        cancelTimers()
        current = null
        failedUrls.clear()
        attempt.clear()
        player?.stop()
        player?.clearMediaItems()
    }
    private fun cancelTimers() {
        handler.removeCallbacks(stalled); handler.removeCallbacks(noFrame); handler.removeCallbacks(playingFeedback)
    }
    private fun requestId(eventTime: AnalyticsListener.EventTime): String = runCatching {
        eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem.mediaId
    }.getOrDefault("")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = PlayerBinding.inflate(inflater, container, false)
        binding = view
        view.surfaceView.visibility = View.GONE
        val http = com.lizongying.mytv.agent.ChannelHttpFactory.create()
        player = ExoPlayer.Builder(requireContext())
            .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(requireContext(), http)))
            .build()
        view.playerView.player = player
        player?.playWhenReady = true
        player?.addAnalyticsListener(object : AnalyticsListener {
            override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime, output: Any, renderTimeMs: Long) {
                if (attempt.frame(requestId(eventTime))) {
                    handler.removeCallbacks(noFrame)
                    if (player?.isPlaying == true) {
                        handler.postDelayed(playingFeedback, 3000)
                        (activity as? MainActivity)?.isPlaying()
                    }
                }
            }
            override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) {
                if (requestId(eventTime) == attempt.id) tryNextSource("播放器解码 / 请求报错")
            }
        })
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                com.lizongying.mytv.agent.PlaybackRoute.buffering=visible && state==Player.STATE_BUFFERING
                handler.removeCallbacks(stalled)
                if (state == Player.STATE_BUFFERING && attempt.hasFirstFrame) handler.postDelayed(stalled, 15000)
                if (state == Player.STATE_ENDED && current != null) {
                    val endedId = attempt.id
                    handler.post { if (attempt.id == endedId) tryNextSource("直播提前结束") }
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                handler.removeCallbacks(playingFeedback)
                if (isPlaying && attempt.hasFirstFrame) {
                    handler.postDelayed(playingFeedback, 3000)
                    handler.removeCallbacks(stalled)
                    (activity as? MainActivity)?.isPlaying()
                }
            }
        })
        (activity as MainActivity).fragmentReady("PlayerFragment")
        return view.root
    }
    fun play(tvViewModel: TVViewModel) {
        val url = tvViewModel.getVideoUrlCurrent()
        if (current?.getTV()?.title != tvViewModel.getTV().title) { failedUrls.clear(); refreshedDynamic.clear() }
        current = tvViewModel
        if(!visible) return
        // Keep a healthy video playing when the catalogue is refreshed by cleanup.
        if (attempt.url == url && !attempt.failed && player?.playbackState in listOf(Player.STATE_READY, Player.STATE_BUFFERING)) return
        cancelTimers()
        if (url.isBlank()) { clearPlayback(); return }
        prepareJob?.cancel()
        prepareJob=viewLifecycleOwner.lifecycleScope.launch {
            DecoderGate.acquireForeground(decoderOwner)
            if(!visible || current !== tvViewModel || tvViewModel.getVideoUrlCurrent()!=url) return@launch
            com.lizongying.mytv.agent.PlaybackRoute.url=url
            val id = attempt.start(url)
            player?.setMediaItem(AgentRepository.mediaItem(requireContext(),url,id))
            player?.prepare()
            player?.playWhenReady = true
            handler.postDelayed(noFrame, 20000)
        }
    }
    private fun tryNextSource(reason: String) {
        val model = current ?: return
        if (!attempt.fail(attempt.id)) return
        cancelTimers()
        val failedUrl = attempt.url
        if(com.lizongying.mytv.agent.CwjdProtocol.matches(failedUrl) && refreshedDynamic.add(failedUrl)) {
            com.lizongying.mytv.agent.CwjdResolver.invalidate()
            player?.stop(); play(model); return
        }
        context?.applicationContext?.let { app -> lifecycleScope.launch { AgentRepository.recordPlayback(app, failedUrl, false, reason) } }
        failedUrls.add(failedUrl)
        com.lizongying.mytv.agent.PlaybackRoute.url=""
        player?.stop()
        val urls = model.videoUrl.value.orEmpty()
        val next = urls.indices.firstOrNull { urls[it] !in failedUrls }
        if (next == null) {
            player?.stop()
            Toast.makeText(context, "此频道暂无可用线路；可到设置检测并移除无效线路", Toast.LENGTH_LONG).show()
            return
        }
        model.setVideoIndex(next)
        model.changed()
    }
    override fun onStart() {
        super.onStart()
        visible=true; DecoderGate.requestForeground(decoderOwner)
        AgentScheduler.playbackStarted(requireContext().applicationContext)
        current?.let { failedUrls.clear(); refreshedDynamic.clear(); play(it) }
    }
    override fun onStop() {
        com.lizongying.mytv.agent.PlaybackRoute.url=""
        com.lizongying.mytv.agent.PlaybackRoute.buffering=false
        visible=false; prepareJob?.cancel(); cancelTimers()
        player?.stop()
        DecoderGate.releaseForeground(decoderOwner)
        context?.applicationContext?.let { AgentScheduler.playbackStopped(it); AutoVisionScheduler.nudge(it) }
        super.onStop()
    }
    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        binding?.playerView?.player = null
        player?.release()
        DecoderGate.releaseForeground(decoderOwner)
        player = null
        binding = null
        attempt.clear()
        super.onDestroyView()
    }
}
