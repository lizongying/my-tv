package com.lizongying.mytv

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.lizongying.mytv.databinding.PlayerBinding
import com.lizongying.mytv.models.TVViewModel


class PlayerFragment : Fragment() {

    private var _binding: PlayerBinding? = null
    private var playerView: PlayerView? = null
    private var videoUrl: String? = null
    private var tvViewModel: TVViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlayerBinding.inflate(inflater, container, false)
        playerView = _binding!!.playerView
        (activity as MainActivity).playerFragment = this
        playerView?.viewTreeObserver?.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                playerView!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
                playerView!!.player = activity?.let {
                    ExoPlayer.Builder(it)
                        .build()
                }
                playerView!!.player?.playWhenReady = true
                playerView!!.player?.addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        val aspectRatio = 16f / 9f
                        val layoutParams = playerView?.layoutParams
                        layoutParams?.width =
                            (playerView?.measuredHeight?.times(aspectRatio))?.toInt()
                        playerView?.layoutParams = layoutParams
                    }
//
//                    override fun onPlayerError(error: PlaybackException) {
//                            super.onPlayerError(error)
//                    }
                })
                if (videoUrl !== null) {
                    playerView!!.player?.run {
                        videoUrl?.let { MediaItem.fromUri(it) }?.let { setMediaItem(it) }
                        prepare()
                    }
                    videoUrl = null
                }
            }
        })
        Log.i(TAG, "PlayerFragment onCreateView")
        return _binding!!.root
    }

    @OptIn(UnstableApi::class)
    fun play(tvViewModel: TVViewModel) {
        this.tvViewModel = tvViewModel
        val videoUrlCurrent =
            tvViewModel.videoIndex.value?.let { tvViewModel.videoUrl.value?.get(it) }
        if (playerView == null || playerView?.player == null) {
            Log.i(TAG, "playerView not ready $view}")
            videoUrl = videoUrlCurrent
        } else {
            Log.i(TAG, "playerView ok")
            playerView?.player?.run {
                val mediaItem = MediaItem.Builder()
                tvViewModel.id.value?.let { mediaItem.setMediaId(it.toString()) }
                videoUrlCurrent?.let { mediaItem.setUri(it) }
                setMediaItem(mediaItem.build())
                prepare()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (playerView != null) {
            playerView!!.player?.stop()
        }
    }

    override fun onResume() {
        super.onResume()
        if (playerView != null) {
            playerView!!.player?.play()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (playerView != null) {
            playerView!!.player?.release()
        }
    }

    companion object {
        private const val TAG = "PlaybackVideoFragment"
    }
}