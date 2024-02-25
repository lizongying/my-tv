package com.lizongying.mytv.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST


interface YSPApiService {
    @POST("v1/player/get_live_info")
    @Headers(
        "content-type: application/json;charset=UTF-8",
        "referer: https://www.yangshipin.cn/",
        "yspappid: 519748109",
        "user-agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    )
    fun getLiveInfo(
        @Header("cookie") cookie: String,
        @Header("Yspplayertoken") token: String,
        @Body request: LiveInfoRequest,
    ): Call<LiveInfo>

    @POST("v1/player/auth")
    @Headers(
        "content-type: application/x-www-form-urlencoded;charset=UTF-8",
        "referer: https://www.yangshipin.cn/",
        "yspappid: 519748109",
        "user-agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    )
    fun getAuth(
        @Header("cookie") cookie: String,
        @Body request: AuthRequest,
    ): Call<Auth>
}