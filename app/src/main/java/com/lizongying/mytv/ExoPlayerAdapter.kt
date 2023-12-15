package com.lizongying.mytv

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.view.SurfaceHolder
import androidx.annotation.OptIn
import androidx.leanback.media.PlaybackGlueHost
import androidx.leanback.media.PlayerAdapter
import androidx.leanback.media.SurfaceHolderGlueHost
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import java.io.IOException


open class ExoPlayerAdapter(private var mContext: Context?) : PlayerAdapter() {

    val mPlayer = mContext?.let { ExoPlayer.Builder(it).build() }
    var mSurfaceHolderGlueHost: SurfaceHolderGlueHost? = null
    val mRunnable: Runnable = object : Runnable {
        override fun run() {
            callback.onCurrentPositionChanged(this@ExoPlayerAdapter)
            mHandler.postDelayed(this, getProgressUpdatingInterval().toLong())
        }
    };
    val mHandler = Handler()
    var mInitialized = false // true when the MediaPlayer is prepared/initialized

    var mMediaSourceUri: Uri? = null
    var mHasDisplay = false
    var mBufferedProgress: Long = 0

    var mBufferingStart = false

    open fun notifyBufferingStartEnd() {
        callback.onBufferingStateChanged(
            this@ExoPlayerAdapter,
            mBufferingStart || !mInitialized
        )
    }

    override fun onAttachedToHost(host: PlaybackGlueHost?) {
        if (host is SurfaceHolderGlueHost) {
            mSurfaceHolderGlueHost = host
            mSurfaceHolderGlueHost!!.setSurfaceHolderCallback(VideoPlayerSurfaceHolderCallback(this))
        }
    }

    /**
     * Will reset the [MediaPlayer] and the glue such that a new file can be played. You are
     * not required to call this method before playing the first file. However you have to call it
     * before playing a second one.
     */
    open fun reset() {
        changeToUnitialized()
//        mPlayer.reset()
    }

    open fun changeToUnitialized() {
        if (mInitialized) {
            mInitialized = false
            notifyBufferingStartEnd()
            if (mHasDisplay) {
                callback.onPreparedStateChanged(this@ExoPlayerAdapter)
            }
        }
    }

    /**
     * Release internal MediaPlayer. Should not use the object after call release().
     */
    open fun release() {
        changeToUnitialized()
        mHasDisplay = false
        mPlayer?.release()
    }

    override fun onDetachedFromHost() {
        if (mSurfaceHolderGlueHost != null) {
            mSurfaceHolderGlueHost!!.setSurfaceHolderCallback(null)
            mSurfaceHolderGlueHost = null
        }
        reset()
        release()
    }

    /**
     * @see MediaPlayer.setDisplay
     */
    fun setDisplay(surfaceHolder: SurfaceHolder?) {
        val hadDisplay = mHasDisplay
        mHasDisplay = surfaceHolder != null
        if (hadDisplay == mHasDisplay) {
            return
        }
        mPlayer?.setVideoSurfaceHolder(surfaceHolder)
        if (mHasDisplay) {
            if (mInitialized) {
                callback.onPreparedStateChanged(this@ExoPlayerAdapter)
            }
        } else {
            if (mInitialized) {
                callback.onPreparedStateChanged(this@ExoPlayerAdapter)
            }
        }
    }

    override fun setProgressUpdatingEnabled(enabled: Boolean) {
        mHandler.removeCallbacks(mRunnable)
        if (!enabled) {
            return
        }
        mHandler.postDelayed(mRunnable, getProgressUpdatingInterval().toLong())
    }

    /**
     * Return updating interval of progress UI in milliseconds. Subclass may override.
     * @return Update interval of progress UI in milliseconds.
     */
    open fun getProgressUpdatingInterval(): Int {
        return 16
    }

    override fun isPlaying(): Boolean {
        return mInitialized && mPlayer?.isPlaying ?: false
    }

    override fun getDuration(): Long {
        if (mInitialized) {
            val duration = mPlayer?.duration
            if (duration != null) {
                return duration.toLong()
            }
        }
        return -1
    }

    override fun getCurrentPosition(): Long {
        if (mInitialized) {
            val currentPosition = mPlayer?.currentPosition
            if (currentPosition != null) {
                return currentPosition.toLong()
            }
        }
        return -1
    }

    override fun play() {
        if (!mInitialized || mPlayer?.isPlaying == true) {
            return
        }
        mPlayer?.play()
        callback.onPlayStateChanged(this@ExoPlayerAdapter)
        callback.onCurrentPositionChanged(this@ExoPlayerAdapter)
    }

    override fun pause() {
        if (isPlaying) {
            mPlayer?.pause()
            callback.onPlayStateChanged(this@ExoPlayerAdapter)
        }
    }

    override fun seekTo(newPosition: Long) {
        if (!mInitialized) {
            return
        }
        mPlayer?.seekTo(newPosition.toInt().toLong())
    }

    override fun getBufferedPosition(): Long {
        return mBufferedProgress
    }

    /**
     * Sets the media source of the player witha given URI.
     *
     * @return Returns `true` if uri represents a new media; `false`
     * otherwise.
     * @see MediaPlayer.setDataSource
     */
    open fun setDataSource(uri: Uri?): Boolean {
        if (if (mMediaSourceUri != null) mMediaSourceUri == uri else uri == null) {
            return false
        }
        mMediaSourceUri = uri
        prepareMediaForPlaying()
        return true
    }

    private var mHeaders: Map<String, String>? = mapOf()

    fun setHeaders(headers: Map<String, String>) {
        mHeaders = headers
    }

    @OptIn(UnstableApi::class)
    private fun prepareMediaForPlaying() {
        reset()
        try {
            if (mMediaSourceUri != null) {
                val httpDataSource = DefaultHttpDataSource.Factory()
                mHeaders?.let { httpDataSource.setDefaultRequestProperties(it) }

                val hlsMediaSource =
                    HlsMediaSource.Factory(httpDataSource).createMediaSource(
                        MediaItem.fromUri(
                            mMediaSourceUri!!
                        )
                    )
                mPlayer?.setMediaSource(hlsMediaSource)
            } else {
                return
            }
        } catch (e: IOException) {
            e.printStackTrace()
            throw RuntimeException(e)
        }
        mPlayer?.prepare()
        mPlayer?.playWhenReady = true
        callback.onPlayStateChanged(this@ExoPlayerAdapter)
    }

    /**
     * @return True if MediaPlayer OnPreparedListener is invoked and got a SurfaceHolder if
     * [PlaybackGlueHost] provides SurfaceHolder.
     */
    override fun isPrepared(): Boolean {
        return mInitialized && (mSurfaceHolderGlueHost == null || mHasDisplay)
    }
}

internal class VideoPlayerSurfaceHolderCallback(private val playerAdapter: ExoPlayerAdapter) :
    SurfaceHolder.Callback {

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        playerAdapter.setDisplay(surfaceHolder)
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {
        // Handle surface changes if needed
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
        playerAdapter.setDisplay(null)
    }
}
