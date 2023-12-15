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
        "cookie: guid=1; vplatform=109"
    )
    fun getLiveInfo(
        @Body request: LiveInfoRequest,
    ): Call<LiveInfo>
}