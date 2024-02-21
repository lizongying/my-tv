package com.lizongying.mytv.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query


interface YSPTokenService {
    @GET("my-tv/v1/info")
    fun getInfo(
        @Query("token") token: String = "",
    ): Call<Info>
}