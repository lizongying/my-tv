package com.lizongying.mytv.api

data class Info(
    val code: Int?,
    val msg: String?,
    val data: InfoData,
)

data class InfoData(
    val token: String,
)

data class TimeResponse(
    val data: Time
) {
    data class Time(
        val t: String
    )
}