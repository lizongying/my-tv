package com.lizongying.mytv.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ReleaseService {
    @GET("my-tv/v2/release/{name}")
    fun getRelease(
        @Path("name") date: String = "1",
    ): Call<ReleaseV2>
}