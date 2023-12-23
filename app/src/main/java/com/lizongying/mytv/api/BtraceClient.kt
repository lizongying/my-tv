package com.lizongying.mytv.api


import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class BtraceClient {
    private val yspUrl = "https://btrace.yangshipin.cn/"

    val yspBtraceService: YSPBtraceService by lazy {
        Retrofit.Builder()
            .baseUrl(yspUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(YSPBtraceService::class.java)
    }
}