package com.lizongying.mytv.api

import com.lizongying.mytv.proto.Ysp.cn.yangshipin.oms.common.proto.pageModel
import com.lizongying.mytv.proto.Ysp.cn.yangshipin.omstv.common.proto.epgProgramModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

interface YSPProtoService {
    @GET("api/oms/pc/page/PG00000004")
    @Headers(
        "yspappid: 519748109",
        "platform: 109",
    )
    fun getPage(
    ): Call<pageModel.Response>

    @GET("api/yspepg/program/{livepid}/{date}")
    @Headers(
        "yspappid: 519748109",
        "platform: 109",
    )
    fun getProgram(
        @Path("livepid") livepid: String,
        @Path("date") date: String,
    ): Call<epgProgramModel.Response>
}