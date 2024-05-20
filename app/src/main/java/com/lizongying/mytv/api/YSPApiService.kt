package com.lizongying.mytv.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query


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
        @Header("Yspsdkinput") Yspsdkinput: String,
        @Header("yspsdksign") yspsdksign: String,
        @Header("Seqid") Seqid: String,
        @Header("Request-Id") RequestId: String,
        @Header("Yspappid") Yspappid: String="519748109",
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

    @GET("web/open/token")
    @Headers(
        "content-type: application/json",
        "referer: https://www.yangshipin.cn/",
        "user-agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    )
    fun getToken(
        @Header("cookie") cookie: String,
        @Query("yspappid") yspappid: String = "519748109",
        @Query("guid") guid: String = "",
        @Query("vappid") vappid: String = "59306155",
        @Query("vsecret") vsecret: String = "b42702bf7309a179d102f3d51b1add2fda0bc7ada64cb801",
        @Query("raw") raw: String = "1",
        @Query("ts") ts: String = "",
    ): Call<Token2>
}