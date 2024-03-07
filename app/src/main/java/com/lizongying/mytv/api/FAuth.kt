package com.lizongying.mytv.api


data class FAuth(
    val data: Data,
) {
    data class Data(
        val live_url: String,
    )
}