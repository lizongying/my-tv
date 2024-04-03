package com.lizongying.mytv.models

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lizongying.mytv.TV
import com.lizongying.mytv.api.FEPG
import com.lizongying.mytv.proto.Ysp.cn.yangshipin.omstv.common.proto.programModel.Program
import com.tencent.videolite.android.datamodel.cctvjce.TVProgram
import java.text.SimpleDateFormat
import java.util.TimeZone

class TVViewModel(private var tv: TV) : ViewModel() {

    private var rowPosition: Int = 0
    private var itemPosition: Int = 0

    var retryTimes = 0
    var retryMaxTimes = 8
    var tokenYSPRetryTimes = 0
    var tokenYSPRetryMaxTimes = 0
    var tokenFHRetryTimes = 0
    var tokenFHRetryMaxTimes = 8

    var needGetToken = false

    private val _errInfo = MutableLiveData<String>()
    val errInfo: LiveData<String>
        get() = _errInfo

    private var _epg = MutableLiveData<MutableList<EPG>>()
    val epg: LiveData<MutableList<EPG>>
        get() = _epg

    private val _videoUrl = MutableLiveData<List<String>>()
    val videoUrl: LiveData<List<String>>
        get() = _videoUrl

    private val _videoIndex = MutableLiveData<Int>()
    val videoIndex: LiveData<Int>
        get() = _videoIndex

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
        _videoUrl.value = tv.videoUrl
        _videoIndex.value = tv.videoUrl.lastIndex
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
        _videoUrl.value = tv.videoUrl
        _videoIndex.value = tv.videoUrl.lastIndex
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

    fun getTV(): TV {
        return tv
    }

    fun addYJceEPG(p: MutableList<TVProgram>) {
        _epg.value = p.map { EPG(it.name, it.start_time_stamp.toInt()) }.toMutableList()
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

    fun getVideoUrlCurrent(): String {
        return _videoUrl.value!![_videoIndex.value!!]
    }

    companion object {
        private const val TAG = "TVViewModel"
    }
}