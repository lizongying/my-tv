package com.lizongying.mytv.api


import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.protobuf.ProtoConverterFactory


class ApiClient {
    private val yspUrl = "https://player-api.yangshipin.cn/"
    private val myUrl = "https://lyrics.run/"
    private val protoUrl = "https://capi.yangshipin.cn/"
    private val traceUrl = "https://btrace.yangshipin.cn/"

    private var okHttpClient = OkHttpClient.Builder()
        .dns(DnsCache())
        .build()

    val yspApiService: YSPApiService by lazy {
        Retrofit.Builder()
            .baseUrl(yspUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(YSPApiService::class.java)
    }

    val yspTokenService: YSPTokenService by lazy {
        Retrofit.Builder()
            .baseUrl(myUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(YSPTokenService::class.java)
    }

    val yspProtoService: YSPProtoService by lazy {
        Retrofit.Builder()
            .baseUrl(protoUrl)
            .client(okHttpClient)
            .addConverterFactory(ProtoConverterFactory.create())
            .build().create(YSPProtoService::class.java)
    }

    val yspBtraceService: YSPBtraceService by lazy {
        Retrofit.Builder()
            .baseUrl(traceUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(YSPBtraceService::class.java)
    }
}