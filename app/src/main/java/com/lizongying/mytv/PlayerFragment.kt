package com.lizongying.mytv

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.lizongying.mytv.databinding.PlayerBinding
import com.lizongying.mytv.models.TVViewModel


class PlayerFragment : Fragment() {

    private var lastVideoUrl: String = ""

    private var _binding: PlayerBinding? = null
    private var playerView: PlayerView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlayerBinding.inflate(inflater, container, false)
        playerView = _binding!!.playerView
        return _binding!!.root
    }

    @OptIn(UnstableApi::class)
    fun play(tvModel: TVViewModel) {
        val videoUrl = tvModel.videoIndex.value?.let { tvModel.videoUrl.value?.get(it) }
        if (videoUrl == null || videoUrl == "") {
            Log.e(TAG, "${tvModel.title.value} videoUrl is empty")
            return
        }

        if (videoUrl == lastVideoUrl) {
            Log.e(TAG, "videoUrl is duplication")
            return
        }

        lastVideoUrl = videoUrl

        if (playerView!!.player == null) {
            playerView!!.player = activity?.let {
                ExoPlayer.Builder(it)
                    .build()
            }
            playerView!!.player?.playWhenReady = true
            playerView!!.player?.addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    val aspectRatio = 16f / 9f
                    val layoutParams = playerView?.layoutParams
                    layoutParams?.width = (playerView?.measuredHeight?.times(aspectRatio))?.toInt()
                    playerView?.layoutParams = layoutParams
                }
            })
        }

        playerView!!.player?.run {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
        }
    }

    companion object {
        private const val TAG = "PlaybackVideoFragment"
    }
}