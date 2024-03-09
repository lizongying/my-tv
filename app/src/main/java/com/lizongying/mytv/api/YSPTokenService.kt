package com.lizongying.mytv.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query


interface YSPTokenService {
    @GET("my-tv/v1/info")
    fun getInfo(
        @Query("token") token: String = "",
    ): Call<Info>

    @GET("my-tv/v2/token")
    fun getToken(
        @Query("token") token: String = "",
    ): Call<Token>

    @GET("my-tv/v2/info")
    fun getInfoV2(
    ): Call<InfoV2>
}