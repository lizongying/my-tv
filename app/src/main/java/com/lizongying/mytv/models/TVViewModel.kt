package com.lizongying.mytv.models

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import com.lizongying.mytv.TV
import com.lizongying.mytv.api.FEPG
import com.lizongying.mytv.proto.Ysp.cn.yangshipin.omstv.common.proto.programModel.Program
import java.text.SimpleDateFormat
import java.util.TimeZone

class TVViewModel(private var tv: TV) : ViewModel() {

    private var rowPosition: Int = 0
    private var itemPosition: Int = 0

    var retryTimes = 0
    var retryMaxTimes = 8
    var tokenRetryTimes = 0
    var tokenRetryMaxTimes = 0
    var tokenFHRetryTimes = 0
    var tokenFHRetryMaxTimes = 2

    private val _errInfo = MutableLiveData<String>()
    val errInfo: LiveData<String>
        get() = _errInfo

    private val _programId = MutableLiveData<String>()
    val programId: LiveData<String>
        get() = _programId

    private var _epg = MutableLiveData<MutableList<EPG>>()
    val epg: LiveData<MutableList<EPG>>
        get() = _epg

    private val _id = MutableLiveData<Int>()
    val id: LiveData<Int>
        get() = _id

    private val _title = MutableLiveData<String>()
    val title: LiveData<String>
        get() = _title

    private val _videoUrl = MutableLiveData<List<String>>()
    val videoUrl: LiveData<List<String>>
        get() = _videoUrl

    private val _videoIndex = MutableLiveData<Int>()
    val videoIndex: LiveData<Int>
        get() = _videoIndex

    private val _logo = MutableLiveData<Any>()
    val logo: LiveData<Any>
        get() = _logo

    private val _pid = MutableLiveData<String>()
    val pid: LiveData<String>
        get() = _pid

    private val _sid = MutableLiveData<String>()
    val sid: LiveData<String>
        get() = _sid

    private val _change = MutableLiveData<Boolean>()
    val change: LiveData<Boolean>
        get() = _change

    private val _ready = MutableLiveData<Boolean>()
    val ready: LiveData<Boolean>
        get() = _ready

    var seq = 0

    fun addVideoUrl(url: String) {
        if (_videoUrl.value?.isNotEmpty() == true) {
            if (_videoUrl.value!!.last().contains("cctv.cn")) {
                tv.videoUrl = tv.videoUrl.subList(0, tv.videoUrl.lastIndex) + listOf(url)
            } else {
                tv.videoUrl = tv.videoUrl + listOf(url)
            }
        } else {
            tv.videoUrl = tv.videoUrl + listOf(url)
        }
        tv.videoIndex = tv.videoUrl.lastIndex
        _videoUrl.value = tv.videoUrl
        _videoIndex.value = tv.videoIndex
    }

    fun firstSource() {
        if (_videoUrl.value!!.isNotEmpty()) {
            setVideoIndex(0)
            allReady()
        } else {
            Log.e(TAG, "no first")
        }
    }

    fun changed() {
        _change.value = true
    }

    fun allReady() {
        _ready.value = true
    }

    fun setVideoIndex(videoIndex: Int) {
        _videoIndex.value = videoIndex
    }

    init {
        _id.value = tv.id
        _title.value = tv.title
        _videoUrl.value = tv.videoUrl
        _videoIndex.value = tv.videoIndex
        _logo.value = tv.logo
        _programId.value = tv.programId
        _pid.value = tv.pid
        _sid.value = tv.sid
    }

    fun getRowPosition(): Int {
        return rowPosition
    }

    fun getItemPosition(): Int {
        return itemPosition
    }

    fun setRowPosition(position: Int) {
        rowPosition = position
    }

    fun setItemPosition(position: Int) {
        itemPosition = position
    }

    fun setErrInfo(info: String) {
        _errInfo.value = info
    }

    fun update(t: TV) {
        tv = t
    }

    fun getTV(): TV {
        return tv
    }

    fun addYEPG(p: MutableList<Program>) {
        _epg.value = p.map { EPG(it.name, it.st.toInt()) }.toMutableList()
    }

    private fun formatFTime(s: String): Int {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = dateFormat.parse(s.substring(0, 19))
        if (date != null) {
            return (date.time / 1000).toInt()
        }
        return 0
    }

    fun addFEPG(p: List<FEPG>) {
        _epg.value = p.map { EPG(it.title, formatFTime(it.event_time)) }.toMutableList()
    }

    private var mHeaders: Map<String, String>? = mapOf()

    fun setHeaders(headers: Map<String, String>) {
        mHeaders = headers
    }

    /**
     * (playerView?.player as ExoPlayer).setMediaSource(tvViewModel.buildSource())
     */
    @OptIn(UnstableApi::class)
    fun buildSource(): HlsMediaSource {
        val httpDataSource = DefaultHttpDataSource.Factory()
        mHeaders?.let { httpDataSource.setDefaultRequestProperties(it) }

        return HlsMediaSource.Factory(httpDataSource).createMediaSource(
            MediaItem.fromUri(
                Uri.parse(getVideoUrlCurrent())
            )
        )
    }

    fun getVideoUrlCurrent(): String {
        return _videoUrl.value!![_videoIndex.value!!]
    }

    companion object {
        private const val TAG = "TVViewModel"
    }
}