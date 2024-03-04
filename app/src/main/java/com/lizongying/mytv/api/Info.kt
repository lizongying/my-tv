package com.lizongying.mytv.api


data class Info(
    val code: Int?,
    val msg: String?,
    val data: Data,
) {
    data class Data(
        val token: String,
    )
}

data class Release(
    val code: Int?,
    val msg: String?,
    val data: Data,
) {
    data class Data(
        val versionName: String,
        val versionCode: Int,
        val downloadUrl: String,
        val updateTime: Int,
    )
}

data class TimeResponse(
    val data: Time
) {
    data class Time(
        val t: String
    )
}