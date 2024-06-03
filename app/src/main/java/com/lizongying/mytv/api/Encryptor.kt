package com.lizongying.mytv.api

import android.content.Context

class Encryptor {
    external fun init(context: Context)

    external fun encrypt(t: String, e: String, r: String, n: String, i: String): String

    external fun encrypt2(t: String): String

    external fun hash(data: ByteArray): ByteArray?

    external fun hash2(data: ByteArray): ByteArray?

    companion object {
        init {
            System.loadLibrary("native")
        }
    }
}