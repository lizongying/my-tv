package com.lizongying.mytv

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.media.PlayerAdapter
import androidx.leanback.widget.PlaybackControlsRow
import com.lizongying.mytv.models.TVViewModel
import java.io.IOException

class PlaybackFragment : VideoSupportFragment() {

    private lateinit var mTransportControlGlue: PlaybackTransportControlGlue<PlayerAdapter>
    private var playerAdapter: ExoPlayerAdapter? = null
    private var lastVideoUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playerAdapter = ExoPlayerAdapter(context)
        playerAdapter?.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE)

        view?.isFocusable = false
        view?.isFocusableInTouchMode = false

        val glueHost = VideoSupportFragmentGlueHost(this@PlaybackFragment)
        mTransportControlGlue = PlaybackControlGlue(activity, playerAdapter)
        mTransportControlGlue.host = glueHost
        mTransportControlGlue.playWhenPrepared()
    }

    override fun showControlsOverlay(runAnimation: Boolean) {
    }

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

        playerAdapter?.callback = PlayerCallback(tvModel)
        if (tvModel.ysp() != null) {
            playerAdapter?.setMinimumLoadableRetryCount(0)
        }
        try {
            playerAdapter?.setDataSource(Uri.parse(videoUrl))
        } catch (e: IOException) {
            Log.e(TAG, "error $e")
            return
        }
        hideControlsOverlay(false)
    }

    private inner class PlayerCallback(private var tvModel: TVViewModel) :
        PlayerAdapter.Callback() {
        override fun onError(adapter: PlayerAdapter?, errorCode: Int, errorMessage: String?) {
            Log.e(TAG, "on error: $errorMessage")
            if (tvModel.ysp() != null && tvModel.videoIndex.value!! > 0 && errorMessage == "Source error") {
                tvModel.changed()
            }
        }
    }

    companion object {
        private const val TAG = "PlaybackVideoFragment"
    }
}