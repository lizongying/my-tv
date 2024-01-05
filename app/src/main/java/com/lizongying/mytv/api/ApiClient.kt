package com.lizongying.mytv.api


import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class ApiClient {
    private val yspUrl = "https://player-api.yangshipin.cn/"
    private val myUrl = "https://lyrics.run/"
//    private val myUrl = "http://10.0.2.2:8081/"

    val yspApiService: YSPApiService by lazy {
        Retrofit.Builder()
            .baseUrl(yspUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(YSPApiService::class.java)
    }

    val yspTokenService: YSPTokenService by lazy {
        Retrofit.Builder()
            .baseUrl(myUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(YSPTokenService::class.java)
    }
}