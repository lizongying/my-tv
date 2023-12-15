package com.lizongying.mytv.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lizongying.mytv.TV

class TVViewModel(private var tv: TV) : ViewModel() {
    private var mapping = mapOf(
        "CCTV4K" to "CCTV4K",
//        "CCTV1" to "CCTV1 综合",
        "CCTV2" to "CCTV2 财经",
        "CCTV4" to "CCTV4 中文国际",
        "CCTV5" to "CCTV5 体育",
        "CCTV5+" to "CCTV5+ 体育赛事",
        "CCTV7" to "CCTV7 国防军事",
        "CCTV9" to "CCTV9 记录",
        "CCTV10" to "CCTV10 科教",
        "CCTV11" to "CCTV11 戏曲",
        "CCTV12" to "CCTV12 社会与法",
//        "CCTV13" to "CCTV13",
        "CCTV14" to "CCTV14 少儿",
        "CCTV15" to "CCTV15 音乐",
        "CCTV16-HD" to "CCTV16 奥林匹克",
        "CCTV17" to "CCTV17 农业农村",
        "CGTN" to "CGTN",
        "CGTN法语频道" to "CGTN 法语频道",
        "CGTN俄语频道" to "CGTN 俄语频道",
        "CGTN阿拉伯语频道" to "CGTN 阿拉伯语频道",
        "CGTN西班牙语频道" to "CGTN 西班牙语频道",
//        "CGTN外语纪录频道" to "CGTN外语纪录频道",

        "东方卫视" to "东方卫视",
        "湖南卫视" to "湖南卫视",
        "湖北卫视" to "湖北卫视",
        "辽宁卫视" to "辽宁卫视",
        "江苏卫视" to "江苏卫视",
        "江西卫视" to "江西卫视",
        "山东卫视" to "山东卫视",
        "广东卫视" to "广东卫视",
        "广西卫视" to "广西卫视",
        "重庆卫视" to "重庆卫视",
        "河南卫视" to "河南卫视",
        "河北卫视" to "河北卫视",
        "贵州卫视" to "贵州卫视",
        "北京卫视" to "北京卫视",
        "黑龙江卫视" to "黑龙江卫视",
        "浙江卫视" to "浙江卫视",
        "安徽卫视" to "安徽卫视",
        "深圳卫视" to "深圳卫视",
        "四川卫视" to "四川卫视",
        "福建东南卫视" to "东南卫视",
        "海南卫视" to "海南卫视",
    ).entries.associate { (key, value) -> value to key }

    private var updateByYSP = false

    private val _videoUrl = MutableLiveData<List<String>>()
    val videoUrl: LiveData<List<String>>
        get() = _videoUrl

    private val _pid = MutableLiveData<String>()
    val pid: LiveData<String>
        get() = _pid

    private val _sid = MutableLiveData<String>()
    val sid: LiveData<String>
        get() = _sid

    private val _backgroundImage = MutableLiveData<String>()
    val backgroundImage: LiveData<String>
        get() = _backgroundImage

    // 方法用于更新背景图信息
    fun updateBackgroundImage(url: String) {
        _backgroundImage.value = url
    }

    fun updateVideoUrl(url: String) {
        tv.videoUrl = listOf(url)
        tv.videoIndex = 0
        _videoUrl.value = listOf(url)
    }

    fun updateVideoUrlByYSP(url: String) {
        updateByYSP = true
        updateVideoUrl(url)
    }

    init {
        _videoUrl.value = tv.videoUrl
        _pid.value = tv.pid
        _sid.value = tv.sid
    }

    fun updateByYSP(): Boolean {
        return updateByYSP
    }

    fun update(t: TV) {
        tv = t
    }

    fun getTV(): TV {
        return tv
    }

    fun ysp(): String? {
        return mapping[tv.title]
    }

    fun getBackgroundImage(): String {
        return tv.logo ?: ""
    }
}