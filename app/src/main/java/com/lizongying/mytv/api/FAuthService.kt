package com.lizongying.mytv.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface FAuthService {
    @GET("api/v3/hub/live/auth-url")
    fun getAuth(
        @Header("token") token: String,
        @Query("live_id") live_id: String = "",
        @Query("live_qa") live_qa: String = "",
    ): Call<FAuth>

    @GET("api/v3/live/{liveId}/resources")
    fun getEPG(
        @Path("liveId") liveId: String,
        @Query("date") date: String,
        @Query("dir") dir: String = "asc"
    ): Call<List<FEPG>>
}