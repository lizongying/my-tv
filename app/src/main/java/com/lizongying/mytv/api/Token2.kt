package com.lizongying.mytv.api

data class Token2(
    val ret: Int?,
    val msg: String?,
    val data: Data,
) {
    data class Data(
        val token: String,
    )
}
