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

    private var a: String = ""
    var b: String = ""
    private var c: String = ""

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
        try {
            val currentTimeMillis = getTimestampFromServer()
            if (currentTimeMillis > 0) {
                between = System.currentTimeMillis() - currentTimeMillis
            }
        } catch (e: Exception) {
            println("Failed to retrieve timestamp from server: ${e.message}")
        }

        var x = ""
        try {
            x = getNothing()
        } catch (e: Exception) {
            x = ""
            println("a ${e.message}")
        }

        if (x != "") {
            try {
                x = getNothing2(x)
            } catch (e: Exception) {
                x = ""
                println("b ${e.message}")
            }
        }

//        if (x != "") {
//            try {
//                getNothing3(x)
//            } catch (e: Exception) {
//                println("b ${e.message}")
//            }
//        }

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
                throw IOException("Error during network request", e)
            }
        }
    }

    private val regex = Regex("""chunk-vendors\.([^.]+)\.js""")

    private suspend fun getNothing(): String {
        return withContext(Dispatchers.IO) {
            val client = okhttp3.OkHttpClient.Builder()
                .addInterceptor(RetryInterceptor(3))
                .build()
            val request = okhttp3.Request.Builder()
                .url("https://www.yangshipin.cn")
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val string = response.body()?.string()

                    val matchResult = string?.let { regex.find(it) }
                    var x = ""
                    if (matchResult != null) {
                        x = matchResult.groupValues[1]
                    }
                    x
                }
            } catch (e: IOException) {
                throw IOException("Error during network request", e)
            }
        }
    }

    private val regex2 = Regex(""""ysp_tx"[^:]+:"([^"]+)[^:]+:"([^"]+)""")

    private val regex3 = Regex(""""(https[^"]+wasm([^"]+))""")

    private suspend fun getNothing2(x: String): String {
        return withContext(Dispatchers.IO) {
            val client = okhttp3.OkHttpClient.Builder()
                .addInterceptor(RetryInterceptor(3))
                .build()

            val request = okhttp3.Request.Builder()
                .url("https://www.yangshipin.cn/js/chunk-vendors.$x.js")
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val string = response.body()?.string()

                    val matches = string?.let { regex2.findAll(it) }

                    var matchResult = matches?.lastOrNull()

                    if (matchResult != null) {
                        val (aa, bb) = matchResult.destructured
                        a = aa
                        b = bb
                    }

                    matchResult = string?.let { regex3.find(it) }

                    if (matchResult != null) {
                        val (cc) = matchResult.destructured
                        c = cc
                        Log.i("", "ccccc $c")
                    }
                    c
                }
            } catch (e: IOException) {
                throw IOException("Error during network request", e)
            }
        }
    }

    private suspend fun getNothing3(x: String): String {
        return withContext(Dispatchers.IO) {
            val client = okhttp3.OkHttpClient.Builder()
                .addInterceptor(RetryInterceptor(3))
                .build()

            val request = okhttp3.Request.Builder()
                .url("https://www.yangshipin.cn/js/chunk-vendors.$x.js")
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val string = response.body()?.string()

                    val matches = string?.let { regex2.findAll(it) }

                    var matchResult = matches?.lastOrNull()

                    if (matchResult != null) {
                        val (aa, bb) = matchResult.destructured
                        a = aa
                        b = bb
                    }

                    matchResult = string?.let { regex3.find(it) }

                    if (matchResult != null) {
                        val (cc) = matchResult.destructured
                        c = cc
                    }
                    c
                }
            } catch (e: IOException) {
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