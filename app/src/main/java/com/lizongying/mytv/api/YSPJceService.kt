package com.lizongying.mytv.api

import com.tencent.videolite.android.datamodel.cctvjce.TVTimeShiftProgramRequest
import com.tencent.videolite.android.datamodel.cctvjce.TVTimeShiftProgramResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface YSPJceService {
    @POST("/")
    @Headers(
        "content-type: application/octet-stream",
        "user-agent: CCTVVideo/2.9.0 (iPad; iOS 17.2; Scale/2.00)",
    )
    fun getProgram(
        @Body body: TVTimeShiftProgramRequest
    ): Call<TVTimeShiftProgramResponse>
}