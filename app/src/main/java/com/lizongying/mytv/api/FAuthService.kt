package com.lizongying.mytv.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface FAuthService {
    @GET("api/v3/hub/live/auth-url")
    fun getAuth(
        @Query("live_id") live_id: String = "",
        @Query("live_qa") live_qa: String = "",
    ): Call<FAuth>
}