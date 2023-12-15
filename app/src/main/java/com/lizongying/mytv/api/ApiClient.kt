package com.lizongying.mytv.api


import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class ApiClient {
    private val yspUrl = "https://player-api.yangshipin.cn/"

    val yspApiService: YSPApiService by lazy {
        Retrofit.Builder()
            .baseUrl(yspUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(YSPApiService::class.java)
    }
}