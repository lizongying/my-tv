package com.lizongying.mytv

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import com.lizongying.mytv.Utils.getDateFormat
import com.lizongying.mytv.api.ApiClient
import com.lizongying.mytv.api.Auth
import com.lizongying.mytv.api.AuthRequest
import com.lizongying.mytv.api.FAuth
import com.lizongying.mytv.api.FAuthService
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
    private var fAuthService: FAuthService = ApiClient().fAuthService
    private var ysp: YSP? = null
    private var token = ""

    private var needAuth = false

    // TODO onDestroy
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var btraceRunnable: BtraceRunnable
    private var tokenRunnable: TokenRunnable = TokenRunnable()

    private val regex = Regex("""des_key = "([^"]+).+var des_iv = "([^"]+)""")
    private val input =
        """{"mver":"1","subver":"1.2","host":"www.yangshipin.cn/#/tv/home?pid=","referer":"","canvas":"YSPANGLE(Apple,ANGLEMetalRenderer:AppleM1Pro,UnspecifiedVersion)"}""".toByteArray()

    init {
        handler.post(tokenRunnable)
    }

    fun initYSP(context: Context) {
        ysp = YSP(context)
    }

    var call: Call<LiveInfo>? = null
    private var callAuth: Call<Auth>? = null

    private fun fetchAuth(tvModel: TVViewModel, cookie: String) {
        callAuth?.cancel()

        val title = tvModel.title.value

        val data = ysp?.getAuthData(tvModel)
        val request = data?.let { AuthRequest(it) }
        callAuth = request?.let { yspApiService.getAuth("guid=${ysp?.getGuid()}; $cookie", it) }

        callAuth?.enqueue(object : Callback<Auth> {
            override fun onResponse(call: Call<Auth>, response: Response<Auth>) {
                if (response.isSuccessful) {
                    val liveInfo = response.body()

                    if (liveInfo?.data?.token != null) {
                        Log.i(TAG, "token ${liveInfo.data.token}")
                        ysp?.token = liveInfo.data.token
                        fetchVideo(tvModel, cookie)
                    } else {
                        Log.e(TAG, "$title token error")
                        if (tvModel.retryTimes < tvModel.retryMaxTimes) {
                            tvModel.retryTimes++
                            if (tvModel.getTV().needToken) {
                                if (tvModel.tokenRetryTimes == tvModel.tokenRetryMaxTimes) {
                                    if (!tvModel.getTV().mustToken) {
                                        fetchAuth(tvModel, cookie)
                                    }
                                } else {
                                    token = ""
                                    fetchAuth(tvModel)
                                }
                            } else {
                                fetchAuth(tvModel, cookie)
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "$title auth status error")
                    if (tvModel.retryTimes < tvModel.retryMaxTimes) {
                        tvModel.retryTimes++
                        if (tvModel.getTV().needToken) {
                            if (tvModel.tokenRetryTimes == tvModel.tokenRetryMaxTimes) {
                                if (!tvModel.getTV().mustToken) {
                                    fetchAuth(tvModel, cookie)
                                }
                            } else {
                                token = ""
                                fetchAuth(tvModel)
                            }
                        } else {
                            fetchAuth(tvModel, cookie)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<Auth>, t: Throwable) {
                Log.e(TAG, "$title auth request error $t")
                if (tvModel.retryTimes < tvModel.retryMaxTimes) {
                    tvModel.retryTimes++
                    if (tvModel.getTV().needToken) {
                        if (tvModel.tokenRetryTimes == tvModel.tokenRetryMaxTimes) {
                            if (!tvModel.getTV().mustToken) {
                                fetchAuth(tvModel, cookie)
                            }
                        } else {
                            token = ""
                            fetchAuth(tvModel)
                        }
                    } else {
                        fetchAuth(tvModel, cookie)
                    }
                }
            }
        })
    }

    fun fetchVideo(tvModel: TVViewModel, cookie: String) {
        call?.cancel()
        if (::btraceRunnable.isInitialized) {
            handler.removeCallbacks(btraceRunnable)
        }

        val title = tvModel.title.value

        tvModel.seq = 0
        val data = ysp?.switch(tvModel)
        val request = data?.let { LiveInfoRequest(it) }
        call = request?.let {
            yspApiService.getLiveInfo(
                "guid=${ysp?.getGuid()}; $cookie",
                ysp!!.token,
                it
            )
        }

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
                        val matchResult = regex.find(decodedString)
                        if (matchResult != null) {
                            val (key, iv) = matchResult.destructured
                            val keyBytes = Base64.decode(key, Base64.DEFAULT)
                            val ivBytes = Base64.decode(iv, Base64.DEFAULT)
                            val url = liveInfo.data.playurl + "&revoi=" + encryptTripleDES(
                                keyBytes + byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
                                ivBytes
                            ).uppercase() + liveInfo.data.extended_param
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
                                if (tvModel.getTV().needToken) {
                                    if (tvModel.tokenRetryTimes == tvModel.tokenRetryMaxTimes) {
                                        if (!tvModel.getTV().mustToken) {
                                            fetchVideo(tvModel, cookie)
//                                            fetchAuth(tvModel, cookie)
                                        }
                                    } else {
                                        token = ""
                                        fetchVideo(tvModel)
//                                        fetchAuth(tvModel)
                                    }
                                } else {
                                    fetchVideo(tvModel, cookie)
//                                    fetchAuth(tvModel, cookie)
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
                                if (tvModel.getTV().needToken) {
                                    if (tvModel.tokenRetryTimes == tvModel.tokenRetryMaxTimes) {
                                        if (!tvModel.getTV().mustToken) {
                                            fetchVideo(tvModel, cookie)
//                                            fetchAuth(tvModel, cookie)
                                        }
                                    } else {
                                        token = ""
                                        fetchVideo(tvModel)
//                                        fetchAuth(tvModel)
                                    }
                                } else {
                                    fetchVideo(tvModel, cookie)
//                                    fetchAuth(tvModel, cookie)
                                }
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "$title status error")
                    if (tvModel.retryTimes < tvModel.retryMaxTimes) {
                        tvModel.retryTimes++
                        if (tvModel.getTV().needToken) {
                            if (tvModel.tokenRetryTimes == tvModel.tokenRetryMaxTimes) {
                                if (!tvModel.getTV().mustToken) {
                                    fetchVideo(tvModel, cookie)
//                                    fetchAuth(tvModel, cookie)
                                }
                            } else {
                                token = ""
                                fetchVideo(tvModel)
//                                fetchAuth(tvModel)
                            }
                        } else {
                            fetchVideo(tvModel, cookie)
//                            fetchAuth(tvModel, cookie)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<LiveInfo>, t: Throwable) {
                Log.e(TAG, "$title fetchVideo request error $t")
                if (tvModel.retryTimes < tvModel.retryMaxTimes) {
                    tvModel.retryTimes++
                    if (tvModel.getTV().needToken) {
                        if (tvModel.tokenRetryTimes == tvModel.tokenRetryMaxTimes) {
                            if (!tvModel.getTV().mustToken) {
                                fetchVideo(tvModel, cookie)
                            }
                        } else {
                            token = ""
                            fetchVideo(tvModel)
                        }
                    } else {
                        fetchVideo(tvModel, cookie)
                    }
                }
            }
        })
    }

    fun fetchAuth(tvModel: TVViewModel) {
        if (token == "") {
            yspTokenService.getInfo("")
                .enqueue(object : Callback<Info> {
                    override fun onResponse(call: Call<Info>, response: Response<Info>) {
                        if (response.isSuccessful) {
                            token = response.body()?.data?.token!!
                            Log.i(TAG, "info success $token")
                            val cookie =
                                "versionName=99.99.99; versionCode=999999; vplatform=109; platformVersion=Chrome; deviceModel=120; appid=1400421205; yspappid=519748109;yspopenid=vu0-8lgGV2LW9QjDeuBFsX8yMnzs37Q3_HZF6XyVDpGR_I; vusession=$token"
                            fetchAuth(tvModel, cookie)
                        } else {
                            Log.e(TAG, "info status error")
                            if (tvModel.tokenRetryTimes < tvModel.tokenRetryMaxTimes) {
                                tvModel.tokenRetryTimes++
                                fetchAuth(tvModel)
                            } else {
                                if (!tvModel.getTV().mustToken) {
                                    val cookie =
                                        "versionName=99.99.99; versionCode=999999; vplatform=109; platformVersion=Chrome; deviceModel=120; appid=1400421205; yspappid=519748109"
                                    fetchAuth(tvModel, cookie)
                                }
                            }
                        }
                    }

                    override fun onFailure(call: Call<Info>, t: Throwable) {
                        Log.e(TAG, "info request error $t")
                        if (tvModel.tokenRetryTimes < tvModel.tokenRetryMaxTimes) {
                            tvModel.tokenRetryTimes++
                            fetchVideo(tvModel)
                        } else {
                            if (!tvModel.getTV().mustToken) {
                                val cookie =
                                    "versionName=99.99.99; versionCode=999999; vplatform=109; platformVersion=Chrome; deviceModel=120; appid=1400421205; yspappid=519748109"
                                fetchAuth(tvModel, cookie)
                            }
                        }
                    }
                })
        } else {
            val cookie =
                "versionName=99.99.99; versionCode=999999; vplatform=109; platformVersion=Chrome; deviceModel=120; appid=1400421205; yspappid=519748109;yspopenid=vu0-8lgGV2LW9QjDeuBFsX8yMnzs37Q3_HZF6XyVDpGR_I; vusession=$token"
            fetchAuth(tvModel, cookie)
        }
    }

    fun fetchVideo(tvModel: TVViewModel) {
        if (token == "") {
            yspTokenService.getInfo("")
                .enqueue(object : Callback<Info> {
                    override fun onResponse(call: Call<Info>, response: Response<Info>) {
                        if (response.isSuccessful && response.body()?.data?.token != null) {
                            token = response.body()?.data?.token!!
                            Log.i(TAG, "info success $token")
                            val cookie =
                                "versionName=99.99.99; versionCode=999999; vplatform=109; platformVersion=Chrome; deviceModel=120; appid=1400421205; yspappid=519748109; yspopenid=vu0-8lgGV2LW9QjDeuBFsX8yMnzs37Q3_HZF6XyVDpGR_I; vusession=$token"
                            fetchVideo(tvModel, cookie)
                        } else {
                            Log.e(TAG, "info status error")
                            if (tvModel.tokenRetryTimes < tvModel.tokenRetryMaxTimes) {
                                tvModel.tokenRetryTimes++
                                fetchVideo(tvModel)
                            } else {
                                if (!tvModel.getTV().mustToken) {
                                    val cookie =
                                        "versionName=99.99.99; versionCode=999999; vplatform=109; platformVersion=Chrome; deviceModel=120; appid=1400421205; yspappid=519748109"
                                    fetchVideo(tvModel, cookie)
                                }
                            }
                        }
                    }

                    override fun onFailure(call: Call<Info>, t: Throwable) {
                        Log.e(TAG, "info request error $t")
                        if (tvModel.tokenRetryTimes < tvModel.tokenRetryMaxTimes) {
                            tvModel.tokenRetryTimes++
                            fetchVideo(tvModel)
                        } else {
                            if (!tvModel.getTV().mustToken) {
                                val cookie =
                                    "versionName=99.99.99; versionCode=999999; vplatform=109; platformVersion=Chrome; deviceModel=120; appid=1400421205; yspappid=519748109"
                                fetchVideo(tvModel, cookie)
                            }
                        }
                    }
                })
        } else {
            val cookie =
                "versionName=99.99.99; versionCode=999999; vplatform=109; platformVersion=Chrome; deviceModel=120; appid=1400421205; yspappid=519748109; yspopenid=vu0-8lgGV2LW9QjDeuBFsX8yMnzs37Q3_HZF6XyVDpGR_I; vusession=$token"
            fetchVideo(tvModel, cookie)
        }
    }

    private var fAuth: Call<FAuth>? = null
    fun fetchFAuth(tvModel: TVViewModel) {
        call?.cancel()
        callAuth?.cancel()
        fAuth?.cancel()

        val title = tvModel.title.value

        fAuth = fAuthService.getAuth(tvModel.getTV().pid, "HD")
        fAuth?.enqueue(object : Callback<FAuth> {
            override fun onResponse(call: Call<FAuth>, response: Response<FAuth>) {
                if (response.isSuccessful && response.body()?.data?.live_url != null) {
                    val url = response.body()?.data?.live_url!!
//                    Log.d(TAG, "$title url $url")
                    tvModel.addVideoUrl(url)
                    tvModel.allReady()
                    tvModel.retryTimes = 0
                } else {
                    Log.e(TAG, "auth status error")
                    if (tvModel.tokenRetryTimes < tvModel.tokenRetryMaxTimes) {
                        tvModel.tokenRetryTimes++
                        fetchFAuth(tvModel)
                    }
                }
            }

            override fun onFailure(call: Call<FAuth>, t: Throwable) {
                Log.e(TAG, "auth request error $t")
                if (tvModel.tokenRetryTimes < tvModel.tokenRetryMaxTimes) {
                    tvModel.tokenRetryTimes++
                    fetchFAuth(tvModel)
                }
            }
        })
    }

    fun fetchData(tvModel: TVViewModel) {
        if (tvModel.getTV().channel == "港澳台") {
            fetchFAuth(tvModel)
            return
        }

        if (tvModel.getTV().needToken) {
            if (needAuth) {
                fetchAuth(tvModel)
            } else {
                fetchVideo(tvModel)
            }
        } else {
            val cookie =
                "versionName=99.99.99; versionCode=999999; vplatform=109; platformVersion=Chrome; deviceModel=120; appid=1400421205"
            if (needAuth) {
                fetchAuth(tvModel, cookie)
            } else {
                fetchVideo(tvModel, cookie)
            }
        }
    }

    inner class TokenRunnable : Runnable {
        override fun run() {
            fetchToken()
            handler.postDelayed(this, 600000)
        }
    }

    fun fetchToken() {
        yspTokenService.getInfo(token)
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
                            Log.d(
                                TAG,
                                "${item.channelName},${item.pid},${item.streamId}"
                            )

                            for ((_, v) in TVList.list) {
                                for (v2 in v) {
                                    if (v2.title == item.channelName || v2.alias == item.channelName) {
                                        v2.pid = item.pid
                                        v2.sid = item.streamId
                                    }
                                }
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
                            Log.d(TAG, "$title program ${program.dataListList.size}")
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
        return try {
            val keySpec = SecretKeySpec(key, "DESede")
            val ivSpec = IvParameterSpec(iv)
            val cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            return cipher.doFinal(input).let { it -> it.joinToString("") { "%02x".format(it) } }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    companion object {
        private const val TAG = "Request"
    }
}