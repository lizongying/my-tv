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
    val api: String, val v: String, val ret: List<String>, val data: Time
) {
    data class Time(
        val t: String
    )
}