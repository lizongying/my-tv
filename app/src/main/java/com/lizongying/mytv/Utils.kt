package com.lizongying.mytv

import android.content.res.Resources
import android.os.Build
import android.util.Log
import android.util.TypedValue
import com.google.gson.Gson
import com.lizongying.mytv.api.TimeResponse
import com.lizongying.mytv.requests.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {
    private var between: Long = 0

    private var listener: Request.RequestListener? = null

    fun getDateFormat(format: String): String {
        return SimpleDateFormat(
            format,
            Locale.CHINA
        ).format(Date(System.currentTimeMillis() - between))
    }

    fun getDateTimestamp(): Long {
        return (System.currentTimeMillis() - between) / 1000
    }

    fun setBetween(currentTimeMillis: Long) {
        between = System.currentTimeMillis() - currentTimeMillis
    }

    suspend fun init() {
        var currentTimeMillis: Long = 0
        try {
            currentTimeMillis = getTimestampFromServer()
        } catch (e: Exception) {
            println("Failed to retrieve timestamp from server: ${e.message}")
        }
        between = System.currentTimeMillis() - currentTimeMillis

        withContext(Dispatchers.Main) {
            listener?.onRequestFinished(null)
        }
    }

    class RetryInterceptor(private val maxRetry: Int) : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            var response = chain.proceed(request)
            var tryCount = 0
            while (!response.isSuccessful && tryCount < maxRetry) {
                tryCount++
                response = chain.proceed(request)
            }
            return response
        }
    }

    /**
     * 从服务器获取时间戳
     * @return Long 时间戳
     */
    private suspend fun getTimestampFromServer(): Long {
        return withContext(Dispatchers.IO) {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(1, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor(RetryInterceptor(3))
                .build()
            val request = okhttp3.Request.Builder()
                .url("https://api.m.taobao.com/rest/api3.do?api=mtop.common.getTimestamp")
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val string = response.body()?.string()
                    Gson().fromJson(string, TimeResponse::class.java).data.t.toLong()
                }
            } catch (e: IOException) {
                // Handle network errors
                throw IOException("Error during network request", e)
            }
        }
    }

    fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().displayMetrics
        ).toInt()
    }

    fun dpToPx(dp: Int): Int {
//        val density = Resources.getSystem().displayMetrics.density
//        return (dp * density + 0.5f).toInt()
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), Resources.getSystem().displayMetrics
        ).toInt()
    }

    fun pxToDp(px: Float): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (px / scale).toInt()
    }

    fun pxToDp(px: Int): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (px / scale).toInt()
    }

    fun isTmallDevice() = Build.MANUFACTURER.equals("Tmall", ignoreCase = true)

    fun setRequestListener(listener: Request.RequestListener) {
        this.listener = listener
    }
}