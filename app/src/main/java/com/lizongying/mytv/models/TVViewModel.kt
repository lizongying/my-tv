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
import com.lizongying.mytv.Utils.getDateTimestamp
import com.lizongying.mytv.proto.Ysp.cn.yangshipin.omstv.common.proto.programModel.Program

class TVViewModel(private var tv: TV) : ViewModel() {

    private var rowPosition: Int = 0
    private var itemPosition: Int = 0

    var retryTimes: Int = 0
    var retryMaxTimes: Int = 8
    var programUpdateTime: Long = 0

    private val _errInfo = MutableLiveData<String>()
    val errInfo: LiveData<String>
        get() = _errInfo

    private val _programId = MutableLiveData<String>()
    val programId: LiveData<String>
        get() = _programId

    private var _program = MutableLiveData<MutableList<Program>>()
    val program: LiveData<MutableList<Program>>
        get() = _program

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

    private val _logo = MutableLiveData<String>()
    val logo: LiveData<String>
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

    private var mMinimumLoadableRetryCount = 5

    var seq = 0

    var needToken = false

    private val channelsNeedToken = arrayOf(
        "CCTV4K 超高清",
        "CCTV2 财经",
        "CCTV5 体育",
        "CCTV5+ 体育赛事",
        "CCTV7 国防军事",
        "CCTV9 记录",
        "CCTV10 科教",
        "CCTV11 戏曲",
        "CCTV12 社会与法",
        "CCTV14 少儿",
        "CCTV15 音乐",
        "CCTV16 奥林匹克",
        "CCTV17 农业农村",

        "CCTV3 综艺",
        "CCTV6 电影",
        "CCTV8 电视剧",
        "风云剧场",
        "第一剧场",
        "怀旧剧场",
        "世界地理",
        "风云音乐",
        "兵器科技",
        "风云足球",
        "高尔夫网球",
        "女性时尚",
        "央视文化精品",
        "央视台球",
        "电视指南",
        "卫生健康",

        "东方卫视",
        "湖南卫视",
        "湖北卫视",
        "辽宁卫视",
        "江苏卫视",
        "江西卫视",
        "山东卫视",
        "广东卫视",
        "广西卫视",
        "重庆卫视",
        "河南卫视",
        "河北卫视",
        "贵州卫视",
        "北京卫视",
        "黑龙江卫视",
        "浙江卫视",
        "安徽卫视",
        "深圳卫视",
        "四川卫视",
        "东南卫视",
        "海南卫视",
        "天津卫视",
        "新疆卫视",
    )

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

    fun setLogo(url: String) {
        _logo.value = url
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
        _program.value = mutableListOf()

        if (tv.title in channelsNeedToken) {
            needToken = true
        }
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

    fun getProgramOne(): Program? {
        val programNew =
            (_program.value?.filter { it.et > getDateTimestamp() })?.toMutableList()
        if (programNew != null && _program.value != programNew) {
            _program.value = programNew
        }
        if (_program.value!!.isEmpty()) {
            return null
        }
        return _program.value?.first()
    }

    fun addProgram(p: MutableList<Program>) {
        val timestamp = getDateTimestamp()

        // after now & not empty & different
        val p1 = (p.filter { it.et > timestamp }).toMutableList()
        if (p1.isEmpty() || _program.value == p1) {
            return
        }

        if (_program.value!!.isEmpty()) {
            _program.value = p1
        } else {
            _program.value =
                ((_program.value?.filter { it.et > timestamp && it.st < p1.first().st })?.plus(
                    p1
                ))?.toMutableList()
        }
    }


    private var mHeaders: Map<String, String>? = mapOf()

    fun setHeaders(headers: Map<String, String>) {
        mHeaders = headers
    }

    fun setMinimumLoadableRetryCount(minimumLoadableRetryCount: Int) {
        mMinimumLoadableRetryCount = minimumLoadableRetryCount
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