package com.lizongying.mytv

import android.content.res.Resources
import android.util.TypedValue
import com.google.gson.Gson
import com.lizongying.mytv.api.TimeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

object Utils {
    fun getDateFormat(format: String): String {
        return SimpleDateFormat(format, Locale.CHINA).format(Date())
    }

    suspend fun getDateTimestamp(): Long {
        return getTimestampFromServer() / 1000
    }

    /**
     * 从服务器获取时间戳
     * @return Long 时间戳
     */
    private suspend fun getTimestampFromServer(): Long {
        return withContext(Dispatchers.IO) {
            val client = okhttp3.OkHttpClient.Builder().connectTimeout(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(1, java.util.concurrent.TimeUnit.SECONDS).build()
            client.newCall(
                okhttp3.Request.Builder().url("https://api.m.taobao.com/rest/api3.do?api=mtop.common.getTimestamp")
                    .build()
            ).execute().use { response ->
                if (!response.isSuccessful) throw java.io.IOException("Unexpected code $response")
                val body = response.body()
                val string = body?.toString()
                Gson().fromJson(string, TimeResponse::class.java).data.t.toLong()
            }
        }
    }

    fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().displayMetrics
        ).toInt()
    }

    fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), Resources.getSystem().displayMetrics
        ).toInt()
    }
}