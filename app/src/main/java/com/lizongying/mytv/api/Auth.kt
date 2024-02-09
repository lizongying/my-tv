package com.lizongying.mytv.api

import android.util.Log
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException

data class Auth(
    val code: Int,
    val msg: String,
    val data: AuthData,
)

data class AuthData(
    val token: String,
)

data class AuthRequest(
    var data: String,
) : RequestBody() {
    override fun contentType(): MediaType? {
        return MediaType.parse("application/x-www-form-urlencoded;charset=UTF-8")
    }

    override fun writeTo(sink: BufferedSink) {
        try {
            sink.writeUtf8(data)
        } catch (e: IOException) {
            Log.e(TAG, "$e")
        }
    }

    companion object {
        private const val TAG = "AuthRequest"
    }
}