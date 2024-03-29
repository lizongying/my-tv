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
    val o: String?,
    val f: String?,
    val t: String?,
    val e: Int?,
    val c: Int?,
)

data class ReleaseV2(
    val n: String?,
    val u: String?,
    val d: String?,
    val c: Int?,
)

data class TimeResponse(
    val data: Time
) {
    data class Time(
        val t: String
    )
}