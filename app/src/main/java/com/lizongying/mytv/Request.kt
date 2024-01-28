package com.lizongying.mytv

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import com.lizongying.mytv.Utils.getDateFormat
import com.lizongying.mytv.api.ApiClient
import com.lizongying.mytv.api.Info
import com.lizongying.mytv.api.LiveInfo
import com.lizongying.mytv.api.LiveInfoRequest
import com.lizongying.mytv.api.YSP
import com.lizongying.mytv.api.YSPApiService
import com.lizongying.mytv.api.YSPBtraceService
import com.lizongying.mytv.api.YSPProtoService
import com.lizongying.mytv.api.YSPTokenService
import com.lizongying.mytv.models.TVViewModel
import com.lizongying.mytv.proto.Ysp.cn.yangshipin.oms.common.proto.pageModel
import com.lizongying.mytv.proto.Ysp.cn.yangshipin.omstv.common.proto.epgProgramModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class Request {
    private var yspTokenService: YSPTokenService = ApiClient().yspTokenService
    private var yspApiService: YSPApiService = ApiClient().yspApiService
    private var yspBtraceService: YSPBtraceService = ApiClient().yspBtraceService
    private var yspProtoService: YSPProtoService = ApiClient().yspProtoService
    private var ysp: YSP? = null
    private var token = ""

    // TODO onDestroy
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var btraceRunnable: BtraceRunnable
    private var tokenRunnable: TokenRunnable = TokenRunnable()

    private var mapping = mapOf(
        "CCTV4K" to "CCTV4K 超高清",
        "CCTV1" to "CCTV1 综合",
        "CCTV2" to "CCTV2 财经",
        "CCTV4" to "CCTV4 中文国际",
        "CCTV5" to "CCTV5 体育",
        "CCTV5+" to "CCTV5+ 体育赛事",
        "CCTV7" to "CCTV7 国防军事",
        "CCTV9" to "CCTV9 记录",
        "CCTV10" to "CCTV10 科教",
        "CCTV11" to "CCTV11 戏曲",
        "CCTV12" to "CCTV12 社会与法",
        "CCTV13" to "CCTV13 新闻",
        "CCTV14" to "CCTV14 少儿",
        "CCTV15" to "CCTV15 音乐",
        "CCTV16-HD" to "CCTV16 奥林匹克",
        "CCTV17" to "CCTV17 农业农村",
        "CGTN" to "CGTN",
        "CGTN法语频道" to "CGTN 法语频道",
        "CGTN俄语频道" to "CGTN 俄语频道",
        "CGTN阿拉伯语频道" to "CGTN 阿拉伯语频道",
        "CGTN西班牙语频道" to "CGTN 西班牙语频道",
        "CGTN外语纪录频道" to "CGTN 纪录频道",
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
        "天津卫视" to "天津卫视",
        "新疆卫视" to "新疆卫视",
    )

    init {
        handler.post(tokenRunnable)
    }

    fun initYSP(context: Context) {
        ysp = YSP(context)
    }

    var call: Call<LiveInfo>? = null

    fun fetchVideo(tvModel: TVViewModel, cookie: String) {
        call?.cancel()
        if (::btraceRunnable.isInitialized) {
            handler.removeCallbacks(btraceRunnable)
        }

        val title = tvModel.title.value

        tvModel.seq = 0
        val data = ysp?.switch(tvModel)
        val request = data?.let { LiveInfoRequest(it) }
        call = request?.let { yspApiService.getLiveInfo("guid=${ysp?.getGuid()}; $cookie", it) }

        call?.enqueue(object : Callback<LiveInfo> {
            override fun onResponse(call: Call<LiveInfo>, response: Response<LiveInfo>) {
                if (response.isSuccessful) {
                    val liveInfo = response.body()
                    if (liveInfo?.data?.playurl != null) {
                        val chanll = liveInfo.data.chanll
                        val decodedBytes = Base64.decode(
                            chanll.substring(9, chanll.length - 3),
                            Base64.DEFAULT
                        )
                        val decodedString = String(decodedBytes)
                        val regex = Regex("""des_key = "([^"]+).+var des_iv = "([^"]+)""")
                        val matchResult = regex.find(decodedString)
                        if (matchResult != null) {
                            val (key, iv) = matchResult.destructured
                            val keyBytes = Base64.decode(key, Base64.DEFAULT)
                            val ivBytes = Base64.decode(iv, Base64.DEFAULT)
                            val url = liveInfo.data.playurl + "&revoi=" + encryptTripleDES(
                                keyBytes + byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
                                ivBytes
                            ).uppercase()
                            Log.d(TAG, "$title url $url")
                            tvModel.addVideoUrl(url)
                            tvModel.allReady()
                            tvModel.retryTimes = 0
                            btraceRunnable = BtraceRunnable(tvModel)
                            handler.post(btraceRunnable)
                        } else {
                            Log.e(TAG, "$title key error")
                            if (tvModel.retryTimes < tvModel.retryMaxTimes) {
                                tvModel.retryTimes++
                                if (tvModel.needToken) {
                                    token = ""
                                    fetchVideo(tvModel)
                                } else {
                                    fetchVideo(tvModel, cookie)
                                }
                            }
                        }
                    } else {
                        if (liveInfo?.data?.errinfo != null && liveInfo.data.errinfo == "应版权方要求，暂停提供直播信号，请点击观看其他精彩节目") {
                            Log.e(TAG, "$title error ${liveInfo.data.errinfo}")
                            tvModel.setErrInfo(liveInfo.data.errinfo)
                        } else {
                            Log.e(TAG, "$title url error $request $liveInfo")
                            if (tvModel.retryTimes < tvModel.retryMaxTimes) {
                                tvModel.retryTimes++
                                if (tvModel.needToken) {
                                    token = ""
                                    fetchVideo(tvModel)
                                } else {
                                    fetchVideo(tvModel, cookie)
                                }
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "$title status error")
                    if (tvModel.retryTimes < tvModel.retryMaxTimes) {
                        tvModel.retryTimes++
                        if (tvModel.needToken) {
                            token = ""
                            fetchVideo(tvModel)
                        } else {
                            fetchVideo(tvModel, cookie)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<LiveInfo>, t: Throwable) {
                Log.e(TAG, "$title request error")
                if (tvModel.retryTimes < tvModel.retryMaxTimes) {
                    tvModel.retryTimes++
                    if (tvModel.needToken) {
                        token = ""
                        fetchVideo(tvModel)
                    } else {
                        fetchVideo(tvModel, cookie)
                    }
                }
            }
        })
    }

    fun fetchVideo(tvModel: TVViewModel) {
        if (token == "") {
            yspTokenService.getInfo()
                .enqueue(object : Callback<Info> {
                    override fun onResponse(call: Call<Info>, response: Response<Info>) {
                        if (response.isSuccessful) {
                            token = response.body()?.data?.token!!
                            Log.i(TAG, "info success $token")
                            val cookie =
                                "vplatform=109; yspopenid=vu0-8lgGV2LW9QjDeuBFsX8yMnzs37Q3_HZF6XyVDpGR_I; vusession=$token"
                            fetchVideo(tvModel, cookie)
                        } else {
                            Log.e(TAG, "info status error")
                            if (tvModel.retryTimes < tvModel.retryMaxTimes) {
                                tvModel.retryTimes++
                                fetchVideo(tvModel)
                            }
                        }
                    }

                    override fun onFailure(call: Call<Info>, t: Throwable) {
                        Log.e(TAG, "info request error $t")
                        if (tvModel.retryTimes < tvModel.retryMaxTimes) {
                            tvModel.retryTimes++
                            fetchVideo(tvModel)
                        }
                    }
                })
        } else {
            val cookie =
                "vplatform=109; yspopenid=vu0-8lgGV2LW9QjDeuBFsX8yMnzs37Q3_HZF6XyVDpGR_I; vusession=$token"
            fetchVideo(tvModel, cookie)
        }
    }

    fun fetchData(tvModel: TVViewModel) {
        if (tvModel.needToken) {
            fetchVideo(tvModel)
        } else {
            val cookie = "vplatform=109"
            fetchVideo(tvModel, cookie)
        }
    }

    inner class TokenRunnable : Runnable {
        override fun run() {
            fetchToken()
            handler.postDelayed(this, 600000)
        }
    }

    fun fetchToken() {
        yspTokenService.getInfo()
            .enqueue(object : Callback<Info> {
                override fun onResponse(call: Call<Info>, response: Response<Info>) {
                    if (response.isSuccessful) {
                        token = response.body()?.data?.token!!
                        Log.i(TAG, "info success $token")
                    } else {
                        Log.e(TAG, "token status error")
                    }
                }

                override fun onFailure(call: Call<Info>, t: Throwable) {
                    Log.e(TAG, "token request error $t")
                }
            })
    }

    inner class BtraceRunnable(private val tvModel: TVViewModel) : Runnable {
        override fun run() {
            fetchBtrace(tvModel)
            handler.postDelayed(this, 60000)
        }
    }

    fun fetchBtrace(tvModel: TVViewModel) {
        val title = tvModel.title.value

        val guid = ysp?.getGuid()!!
        val pid = tvModel.pid.value!!
        val sid = tvModel.sid.value!!
        yspBtraceService.kvcollect(
            c_timestamp = ysp?.generateGuid()!!,
            guid = guid,
            c_guid = guid,
            prog = sid,
            viewid = sid,
            fpid = pid,
            livepid = pid,
            sUrl = "https://www.yangshipin.cn/#/tv/home?pid=$pid",
            playno = ysp?.getRand()!!,
            ftime = getDateFormat("yyyy-MM-dd HH:mm:ss"),
            seq = tvModel.seq.toString(),
        )
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        //                        Log.d(TAG, "$title kvcollect success")
                    } else {
                        Log.e(TAG, "$title kvcollect status error")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Log.e(TAG, "$title kvcollect request error")
                }
            })
        tvModel.seq++
    }

    fun fetchPage() {
        yspProtoService.getPage().enqueue(object : Callback<pageModel.Response> {
            override fun onResponse(
                call: Call<pageModel.Response>,
                response: Response<pageModel.Response>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()

                    if (body?.data?.feedModuleListCount == 1) {
                        for (item in body.data?.feedModuleListList!![0]?.dataTvChannelListList!!) {
                            if (item.isVip && !item.isLimitedFree) {
                                continue
                            }
                            Log.i(
                                TAG,
                                "${item.channelName},${item.pid},${item.streamId}"
                            )
                            var channelType = "央视频道"
                            if (item?.channelType === "weishi") {
                                channelType = "地方频道"
                            }
                            if (!mapping.containsKey(item.channelName)) {
                                continue
                            }
                            val tv =
                                TVList.list[channelType]?.find { it.title == mapping[item.channelName] }
                            if (tv != null) {
                                tv.logo = item.tvLogo
                                tv.pid = item.pid
                                tv.sid = item.streamId
                            }
                        }
                    }
                }
            }

            override fun onFailure(call: Call<pageModel.Response>, t: Throwable) {
                Log.e(TAG, "Page request failed", t)
            }
        })
    }

    fun fetchProgram(tvViewModel: TVViewModel) {
        val title = tvViewModel.title.value
        yspProtoService.getProgram(tvViewModel.programId.value!!, getDateFormat("yyyyMMdd"))
            .enqueue(object : Callback<epgProgramModel.Response> {
                override fun onResponse(
                    call: Call<epgProgramModel.Response>,
                    response: Response<epgProgramModel.Response>
                ) {
                    if (response.isSuccessful) {
                        val program = response.body()
                        if (program != null) {
                            tvViewModel.addProgram(program.dataListList)
                            Log.i(TAG, "$title program ${program.dataListList.size}")
                        }
                    } else {
                        Log.w(TAG, "$title program error")
                    }
                }

                override fun onFailure(call: Call<epgProgramModel.Response>, t: Throwable) {
                    Log.e(TAG, "$title program request failed $t")
                }
            })
    }

    private fun encryptTripleDES(key: ByteArray, iv: ByteArray): String {
        val plaintext =
            """{"mver":"1","subver":"1.2","host":"www.yangshipin.cn/#/tv/home?pid=","referer":"","canvas":"YSPANGLE(Apple,AppleM1Pro,OpenGL4.1)"}"""
        return try {
            val keySpec = SecretKeySpec(key, "DESede")
            val ivSpec = IvParameterSpec(iv)
            val cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(plaintext.toByteArray())
            return encryptedBytes.let { it -> it.joinToString("") { "%02x".format(it) } }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    companion object {
        private const val TAG = "Request"
    }
}