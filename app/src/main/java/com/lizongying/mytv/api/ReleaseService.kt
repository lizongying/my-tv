package com.lizongying.mytv.api

import retrofit2.Call
import retrofit2.http.GET


interface ReleaseService {
    @GET("my-tv/v1/release")
    fun getRelease(
    ): Call<Release>
}