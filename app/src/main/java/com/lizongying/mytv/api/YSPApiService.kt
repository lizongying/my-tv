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

    @POST("v1/player/get_live_info")
    @Headers(
        "content-type: application/json;charset=UTF-8",
        "referer: https://www.yangshipin.cn/",
        "yspappid: 519748109",
        "cookie: guid=1; vplatform=109; yspopenid=vu0-8lgGV2LW9QjDeuBFsX8yMnzs37Q3_HZF6XyVDpGR_I; vusession=mTIVXnDeVUg0FcP6Xo4kaXd9fxlc70a558kySW4phQU"
    )
    fun getLiveInfo2(
        @Body request: LiveInfoRequest,
    ): Call<LiveInfo>
}