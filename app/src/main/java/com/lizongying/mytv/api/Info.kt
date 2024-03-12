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

data class Token(
    val e: Int?,
    val t: String?,
)

data class InfoV2(
    val f: String?,
    val t: String?,
    val e: Int?,
    val c: Int?,
)

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