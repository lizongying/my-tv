package com.lizongying.mytv

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
import com.lizongying.mytv.api.FEPG
import com.lizongying.mytv.api.Info
import com.lizongying.mytv.api.InfoV2
import com.lizongying.mytv.api.LiveInfo
import com.lizongying.mytv.api.LiveInfoRequest
import com.lizongying.mytv.api.Token
import com.lizongying.mytv.api.YSP
import com.lizongying.mytv.api.YSPApiService
import com.lizongying.mytv.api.YSPBtraceService
import com.lizongying.mytv.api.YSPProtoService
import com.lizongying.mytv.api.YSPTokenService
import com.lizongying.mytv.models.TVViewModel
import com.lizongying.mytv.proto.Ysp.cn.yangshipin.oms.common.proto.pageModel
import com.lizongying.mytv.proto.Ysp.cn.yangshipin.omstv.common.proto.epgProgramModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.max


object Request {
    private const val TAG = "Request"
    private var yspTokenService: YSPTokenService = ApiClient().yspTokenService
    private var yspApiService: YSPApiService = ApiClient().yspApiService
    private var yspBtraceService: YSPBtraceService = ApiClient().yspBtraceService
    private var yspProtoService: YSPProtoService = ApiClient().yspProtoService
    private var fAuthService: FAuthService = ApiClient().fAuthService

    private var token = ""
    private var tokenFH = ""

    private var needAuth = false

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var btraceRunnable: BtraceRunnable
    private var tokenRunnable: TokenRunnable = TokenRunnable()

    private val regex = Regex("""des_key = "([^"]+).+var des_iv = "([^"]+)""")
    private val input =
        """{"mver":"1","subver":"1.2","host":"www.yangshipin.cn/#/tv/home?pid=","referer":"","canvas":"YSPANGLE(Apple,ANGLEMetalRenderer:AppleM1Pro,UnspecifiedVersion)"}""".toByteArray()

    private var listener: RequestListener? = null

    private var initRetryTimes = 0
    private var initRetryMaxTimes = 0

    private var openid = "vu0-8lgGV2LW9QjDeucB9H12d_6HQWmTFgqCWg5o-VBQN4"

    fun onCreate() {
        Log.i(TAG, "onCreate")
        fetchInfoV2()
    }

    fun onDestroy() {
        Log.i(TAG, "onDestroy")
        handler.removeCallbacks(tokenRunnable)
    }

    private var call: Call<LiveInfo>? = null
    private var callAuth: Call<Auth>? = null
    private var callInfo: Call<Info>? = null
    private var callFAuth: Call<FAuth>? = null
    private var callPage: Call<pageModel.Response>? = null

    private fun cancelCall() {
        call?.cancel()
        callAuth?.cancel()
        callInfo?.cancel()
        callFAuth?.cancel()
        callPage?.cancel()
    }

    private fun fetchAuth(tvModel: TVViewModel, cookie: String) {
        cancelCall()

        val title = tvModel.getTV().title

        val data = YSP.getAuthData(tvModel)
        val request = AuthRequest(data)
        callAuth = request.let { yspApiService.getAuth("guid=${YSP.getGuid()}; $cookie", it) }
        callAuth?.enqueue(object : Callback<Auth> {
            override fun onResponse(call: Call<Auth>, response: Response<Auth>) {
                if (response.isSuccessful) {
                    val liveInfo = response.body()

                    if (liveInfo?.data?.token != null) {
                        Log.i(TAG, "token ${liveInfo.data.token}")
                        YSP.token = liveInfo.data.token
                        fetchVideo(tvModel, cookie)
                    } else {
                        Log.e(TAG, "$title token error")
                        if (tvModel.retryTimes < tvModel.retryMaxTimes) {
                            tvModel.retryTimes++
                            if (tvModel.getTV().needToken) {
                                if (tvModel.tokenYSPRetryTimes == tvModel.tokenYSPRetryMaxTimes) {
                                    if (!tvModel.getTV().mustToken) {
                                        fetchAuth(tvModel, cookie)
                                    }
                                } else {
                                    tvModel.needGetToken = true
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
                            if (tvModel.tokenYSPRetryTimes == tvModel.tokenYSPRetryMaxTimes) {
                                if (!tvModel.getTV().mustToken) {
                                    fetchAuth(tvModel, cookie)
                                }
                            } else {
                                tvModel.needGetToken = true
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
                        if (tvModel.tokenYSPRetryTimes == tvModel.tokenYSPRetryMaxTimes) {
                            if (!tvModel.getTV().mustToken) {
                                fetchAuth(tvModel, cookie)
                            }
                        } else {
                            tvModel.needGetToken = true
                            fetchAuth(tvModel)
                        }
                    } else {
                        fetchAuth(tvModel, cookie)
                    }
                }
            }
        })
    }

    private fun fetchVideo(tvModel: TVViewModel, cookie: String) {
        cancelCall()

        if (::btraceRunnable.isInitialized) {
            handler.removeCallbacks(btraceRunnable)
        }

        val title = tvModel.getTV().title

        tvModel.seq = 0
        val data = YSP.switch(tvModel)
        val request = LiveInfoRequest(data)
        call = request.let {
            if (token == "") {
                yspApiService.getLiveInfoV2(
                    "guid=${YSP.getGuid()}; $cookie",
                    it,
                )
            } else {
                yspApiService.getLiveInfo(
                    "guid=${YSP.getGuid()}; $cookie",
                    YSP.token,
                    it
                )
            }
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
                                    if (tvModel.tokenYSPRetryTimes == tvModel.tokenYSPRetryMaxTimes) {
                                        if (!tvModel.getTV().mustToken) {
                                            fetchVideo(tvModel, cookie)
//                                            fetchAuth(tvModel, cookie)
                                        }
                                    } else {
                                        tvModel.needGetToken = true
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
                                    if (tvModel.tokenYSPRetryTimes == tvModel.tokenYSPRetryMaxTimes) {
                                        if (!tvModel.getTV().mustToken) {
                                            fetchVideo(tvModel, cookie)
//                                            fetchAuth(tvModel, cookie)
                                        }
                                    } else {
                                        tvModel.needGetToken = true
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
                    Log.e(TAG, "$title status error $data")
                    if (tvModel.retryTimes < tvModel.retryMaxTimes) {
                        tvModel.retryTimes++
                        if (tvModel.getTV().needToken) {
                            if (tvModel.tokenYSPRetryTimes == tvModel.tokenYSPRetryMaxTimes) {
                                if (!tvModel.getTV().mustToken) {
                                    fetchVideo(tvModel, cookie)
//                                    fetchAuth(tvModel, cookie)
                                }
                            } else {
                                tvModel.needGetToken = true
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
                        if (tvModel.tokenYSPRetryTimes == tvModel.tokenYSPRetryMaxTimes) {
                            if (!tvModel.getTV().mustToken) {
                                fetchVideo(tvModel, cookie)
                            }
                        } else {
                            tvModel.needGetToken = true
                            fetchVideo(tvModel)
                        }
                    } else {
                        fetchVideo(tvModel, cookie)
                    }
                }
            }
        })
    }

    private fun fetchAuth(tvModel: TVViewModel) {
        cancelCall()
        if (tvModel.needGetToken) {
            callInfo = yspTokenService.getInfo("")
            callInfo?.enqueue(object : Callback<Info> {
                override fun onResponse(call: Call<Info>, response: Response<Info>) {
                    if (response.isSuccessful && response.body()?.data?.token != null) {
                        token = response.body()?.data?.token!!
                        Log.i(TAG, "info success $token")
                        tvModel.needGetToken = false
                        tvModel.tokenYSPRetryTimes = 0
                        val cookie =
                            "versionName=99.99.99; versionCode=999999; vplatform=109; platformVersion=Chrome; deviceModel=120; appid=1400421205; yspappid=519748109;yspopenid=$openid; vusession=$token"
                        fetchAuth(tvModel, cookie)
                    } else if (response.code() == 304) {
                        tvModel.needGetToken = false
                        tvModel.tokenYSPRetryTimes = 0
                        val cookie =
                            "versionName=99.99.99; versionCode=999999; vplatform=109; platformVersion=Chrome; deviceModel=120; appid=1400421205; yspappid=519748109; yspopenid=$openid; vusession=$token"
                        fetchVideo(tvModel, cookie)
                    } else {
                        Log.e(TAG, "info status error")
                        if (tvModel.tokenYSPRetryTimes < tvModel.tokenYSPRetryMaxTimes) {
                            tvModel.tokenYSPRetryTimes++
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
                    if (tvModel.tokenYSPRetryTimes < tvModel.tokenYSPRetryMaxTimes) {
                        tvModel.tokenYSPRetryTimes++
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
                "versionName=99.99.99; versionCode=999999; vplatform=109; platformVersion=Chrome; deviceModel=120; appid=1400421205; yspappid=519748109;yspopenid=$openid; vusession=$token"
            fetchAuth(tvModel, cookie)
        }
    }

    private fun fetchVideo(tvModel: TVViewModel) {
        cancelCall()
        Log.d(TAG, "fetchVideo")
        if (tvModel.needGetToken) {
            callInfo = yspTokenService.getInfo("")
            callInfo?.enqueue(object : Callback<Info> {
                override fun onResponse(call: Call<Info>, response: Response<Info>) {
                    if (response.isSuccessful && response.body()?.data?.token != null) {
                        token = response.body()?.data?.token!!
                        Log.i(TAG, "info success $token")
                        tvModel.needGetToken = false
                        tvModel.tokenYSPRetryTimes = 0
                        val cookie =
                            "versionName=99.99.99; versionCode=999999; vplatform=109; platformVersion=Chrome; deviceModel=120; appid=1400421205; yspappid=519748109; yspopenid=$openid; vusession=$token"
                        fetchVideo(tvModel, cookie)
                    } else if (response.code() == 304) {
                        tvModel.needGetToken = false
                        tvModel.tokenYSPRetryTimes = 0
                        val cookie =
                            "versionName=99.99.99; versionCode=999999; vplatform=109; platformVersion=Chrome; deviceModel=120; appid=1400421205; yspappid=519748109; yspopenid=$openid; vusession=$token"
                        fetchVideo(tvModel, cookie)
                    } else {
                        Log.e(TAG, "info status error")
                        if (tvModel.tokenYSPRetryTimes < tvModel.tokenYSPRetryMaxTimes) {
                            tvModel.tokenYSPRetryTimes++
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
                    if (tvModel.tokenYSPRetryTimes < tvModel.tokenYSPRetryMaxTimes) {
                        tvModel.tokenYSPRetryTimes++
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
                "versionName=99.99.99; versionCode=999999; vplatform=109; platformVersion=Chrome; deviceModel=120; appid=1400421205; yspappid=519748109; yspopenid=$openid; vusession=$token"
            fetchVideo(tvModel, cookie)
        }
    }

    private fun fetchFAuth(tvModel: TVViewModel) {
        cancelCall()

        val title = tvModel.getTV().title

        var qa = "HD"
        if (tokenFH != "") {
            qa = "FHD"
        }

        callFAuth = fAuthService.getAuth(tokenFH, tvModel.getTV().pid, qa)
        callFAuth?.enqueue(object : Callback<FAuth> {
            override fun onResponse(call: Call<FAuth>, response: Response<FAuth>) {
                if (response.isSuccessful && response.body()?.data?.live_url != null) {
                    val url = response.body()?.data?.live_url!!
//                    Log.d(TAG, "$title url $url")
                    tvModel.addVideoUrl(url)
                    tvModel.allReady()
                    tvModel.tokenFHRetryTimes = 0
                } else {
                    Log.e(TAG, "auth status error ${response.code()}")
                    if (tvModel.tokenFHRetryTimes < tvModel.tokenFHRetryMaxTimes) {
                        tvModel.tokenFHRetryTimes++
                        fetchFAuth(tvModel)
                    }
                }
            }

            override fun onFailure(call: Call<FAuth>, t: Throwable) {
                Log.e(TAG, "auth request error $t")
                if (tvModel.tokenFHRetryTimes < tvModel.tokenFHRetryMaxTimes) {
                    tvModel.tokenFHRetryTimes++
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
            if (token == "") {
                tvModel.needGetToken = true
            }
            if (needAuth) {
                fetchAuth(tvModel)
            } else {
                fetchVideo(tvModel)
            }
        } else {
            val cookie =
                "versionName=99.99.99; versionCode=999999; vplatform=109; platformVersion=Chrome; updateProtocol=1; deviceModel=120"
            if (needAuth) {
                fetchAuth(tvModel, cookie)
            } else {
                fetchVideo(tvModel, cookie)
            }
        }
    }

    class TokenRunnable : Runnable {
        override fun run() {
//            fetchToken()
        }
    }

    private fun fetchInfoV2() {
        yspTokenService.getInfoV2()
            .enqueue(object : Callback<InfoV2> {
                override fun onResponse(call: Call<InfoV2>, response: Response<InfoV2>) {
                    if (response.isSuccessful) {
                        val o = response.body()?.o
                        val t = response.body()?.t
                        val f = response.body()?.f
                        val e = response.body()?.e
                        val c = response.body()?.c
                        if (!o.isNullOrEmpty()) {
                            openid = o
                        }
                        if (!t.isNullOrEmpty()) {
                            token = t
                            Log.i(TAG, "token success $token")
                            if (e != null) {
                                handler.postDelayed(
                                    tokenRunnable,
                                    max(30 * 60 * 1000, (e - 1500) * 1000).toLong()
                                )
                            } else {
                                Log.e(TAG, "e empty")
                                handler.postDelayed(
                                    tokenRunnable,
                                    30 * 60 * 1000
                                )
                            }
                        } else {
                            Log.e(TAG, "token empty")
                            handler.postDelayed(
                                tokenRunnable,
                                30 * 60 * 1000
                            )
                        }
                        if (!f.isNullOrEmpty()) {
                            tokenFH = f
                            Log.i(TAG, "tokenFH success $tokenFH")
                        }
                        if (c != null) {
                            Utils.setBetween(c * 1000L)
                            Log.i(TAG, "current time $c")
                        }
                        listener?.onRequestFinished(null)
                    } else {
                        Log.e(TAG, "token status error")
                        handler.postDelayed(
                            tokenRunnable,
                            30 * 60 * 1000
                        )
                        if (initRetryTimes < initRetryMaxTimes) {
                            initRetryTimes++
                            fetchInfoV2()
                        } else {
                            listener?.onRequestFinished("状态错误")
                        }
                    }
                }

                override fun onFailure(call: Call<InfoV2>, t: Throwable) {
                    Log.e(TAG, "token request error $t")
                    handler.postDelayed(
                        tokenRunnable,
                        30 * 60 * 1000
                    )

                    if (initRetryTimes < initRetryMaxTimes) {
                        initRetryTimes++
                        fetchInfoV2()
                    } else {
                        listener?.onRequestFinished("网络错误")
                    }
                }
            })
    }

    fun fetchToken() {
        yspTokenService.getToken(token)
            .enqueue(object : Callback<Token> {
                override fun onResponse(call: Call<Token>, response: Response<Token>) {
                    if (response.isSuccessful) {
                        val t = response.body()?.t
                        val e = response.body()?.e
                        if (!t.isNullOrEmpty()) {
                            token = t
                            Log.e(TAG, "token success $token")
                            if (e != null) {
                                handler.postDelayed(
                                    tokenRunnable,
                                    max(30 * 60 * 1000, (e - 1500) * 1000).toLong()
                                )
                            } else {
                                Log.e(TAG, "e empty")
                                handler.postDelayed(
                                    tokenRunnable,
                                    30 * 60 * 1000
                                )
                            }
                        } else {
                            Log.e(TAG, "token empty")
                            handler.postDelayed(
                                tokenRunnable,
                                30 * 60 * 1000
                            )
                        }
                    } else {
                        Log.e(TAG, "token status error")
                        handler.postDelayed(
                            tokenRunnable,
                            30 * 60 * 1000
                        )
                    }
                }

                override fun onFailure(call: Call<Token>, t: Throwable) {
                    Log.e(TAG, "token request error $t")
                    handler.postDelayed(
                        tokenRunnable,
                        30 * 60 * 1000
                    )
                }
            })
    }

    class BtraceRunnable(private val tvModel: TVViewModel) : Runnable {
        override fun run() {
            fetchBtrace(tvModel)
            handler.postDelayed(this, 60000)
        }
    }

    fun fetchBtrace(tvModel: TVViewModel) {
        val title = tvModel.getTV().title

        val guid = YSP.getGuid()
        val pid = tvModel.getTV().pid
        val sid = tvModel.getTV().sid
        yspBtraceService.kvcollect(
            c_timestamp = YSP.generateGuid(),
            guid = guid,
            c_guid = guid,
            prog = sid,
            viewid = sid,
            fpid = pid,
            livepid = pid,
            sUrl = "https://www.yangshipin.cn/#/tv/home?pid=$pid",
            playno = YSP.getRand(),
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
        callPage = yspProtoService.getPage()
        callPage?.enqueue(object : Callback<pageModel.Response> {
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

    fun fetchYEPG(tvViewModel: TVViewModel) {
        val title = tvViewModel.getTV().title
        yspProtoService.getProgram(tvViewModel.getTV().programId, getDateFormat("yyyyMMdd"))
            .enqueue(object : Callback<epgProgramModel.Response> {
                override fun onResponse(
                    call: Call<epgProgramModel.Response>,
                    response: Response<epgProgramModel.Response>
                ) {
                    if (response.isSuccessful) {
                        val program = response.body()
                        if (program != null) {
                            tvViewModel.addYEPG(program.dataListList)
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

    fun fetchFEPG(tvViewModel: TVViewModel) {
        val title = tvViewModel.getTV().title
        fAuthService.getEPG(tvViewModel.getTV().pid, getDateFormat("yyyyMMdd"))
            .enqueue(object : Callback<List<FEPG>> {
                override fun onResponse(
                    call: Call<List<FEPG>>,
                    response: Response<List<FEPG>>
                ) {
                    if (response.isSuccessful) {
                        val program = response.body()
                        if (program != null) {
                            tvViewModel.addFEPG(program)
                            Log.d(TAG, "$title program ${program.size}")
                        }
                    } else {
                        Log.w(TAG, "$title program error")
                    }
                }

                override fun onFailure(call: Call<List<FEPG>>, t: Throwable) {
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

    interface RequestListener {
        fun onRequestFinished(message: String?)
    }

    fun setRequestListener(listener: RequestListener) {
        this.listener = listener
    }
}