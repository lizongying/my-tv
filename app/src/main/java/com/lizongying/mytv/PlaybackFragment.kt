package com.lizongying.mytv

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.MediaPlayerAdapter
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.PlaybackControlsRow
import java.io.IOException

class PlaybackFragment : VideoSupportFragment() {

    private lateinit var mTransportControlGlue: PlaybackTransportControlGlue<MediaPlayerAdapter>
    private var playerAdapter: MediaPlayerAdapter? = null
    private var lastVideoUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playerAdapter = MediaPlayerAdapter(context)
        playerAdapter?.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE)

        view?.isFocusable = false
        view?.isFocusableInTouchMode = false
    }

    override fun showControlsOverlay(runAnimation: Boolean) {
    }

    fun play(tv: TV) {
        if (tv.videoUrl.isNullOrBlank()) {
            Log.e(TAG, "videoUrl is empty")
            return
        }

        if (tv.videoUrl.equals(lastVideoUrl)) {
            Log.e(TAG, "videoUrl is duplication")
            return
        }

        lastVideoUrl = tv.videoUrl!!

        playerAdapter?.reset()

        val glueHost = VideoSupportFragmentGlueHost(this@PlaybackFragment)
        mTransportControlGlue = PlaybackControlGlue(activity, playerAdapter)
        mTransportControlGlue.host = glueHost
        mTransportControlGlue.playWhenPrepared()

        try {
            playerAdapter?.setDataSource(Uri.parse(tv.videoUrl))
        } catch (e: IOException) {
            return
        }
        hideControlsOverlay(false)
    }

    override fun onDestroy() {
        if (playerAdapter?.mediaPlayer != null) {
            playerAdapter?.release()
            Log.d(TAG, "playerAdapter released")
        }

        super.onDestroy()
    }

    companion object {
        private const val TAG = "PlaybackVideoFragment"
    }
}