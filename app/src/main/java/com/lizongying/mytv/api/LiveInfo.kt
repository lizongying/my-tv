package com.lizongying.mytv.api

import android.util.Log
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException

data class LiveInfo(
    val code: Int,
    val msg: String,
    val data: LiveInfoData,
)

data class LiveInfoData(
    val chanll: String,
    val playurl: String,
    val errinfo: String,
)

data class LiveInfoRequest(
    var data: String,
) : RequestBody() {
    override fun contentType(): MediaType? {
        return MediaType.parse("application/json;charset=UTF-8")
    }

    override fun writeTo(sink: BufferedSink) {
        try {
            sink.writeUtf8(data)
        } catch (e: IOException) {
            Log.e(TAG, "$e")
        }
    }

    companion object {
        private const val TAG = "LiveInfoRequest"
    }
}