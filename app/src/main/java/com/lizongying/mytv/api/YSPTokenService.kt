package com.lizongying.mytv.api

import retrofit2.Call
import retrofit2.http.GET


interface YSPTokenService {
    @GET("my-tv/v1/info")
    fun getInfo(
    ): Call<Info>
}