package com.lizongying.mytv.api

import retrofit2.Retrofit
import retrofit2.converter.protobuf.ProtoConverterFactory


class ProtoClient {
    private val protoUrl = "https://capi.yangshipin.cn/"

    val yspProtoService: YSPProtoService by lazy {
        Retrofit.Builder()
            .baseUrl(protoUrl)
            .addConverterFactory(ProtoConverterFactory.create())
            .build().create(YSPProtoService::class.java)
    }
}