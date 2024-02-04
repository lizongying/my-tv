package com.lizongying.mytv

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.io.IOException

object Utils {
    fun getDateFormat(format: String): String {
        return SimpleDateFormat(format, Locale.CHINA).format(Date())
    }

    fun getDateTimestamp(): Long {
        return Date().time / 1000
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

    /**
     * 获取可读写的目录
     *
     * @param context 应用环境信息
     *
     * @return 可读写的目录
     *
     */
    fun getAppDirectory(context: Context): File {
        return context.filesDir
    }

    /**
     * 更新channels.json
     *
     * @param context 应用环境信息
     *
     * @return 无
     *
     * @throws IOException 网络请求失败
     */
    fun updateChannel(context: Context) {
        val client = okhttp3.OkHttpClient()
        val request = okhttp3.Request.Builder().url(getServerUrl(context)).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val body = response.body()
            //覆盖channels.json
            val file = File(getAppDirectory(context), "channels.json")
            if (!file.exists()) {
                file.createNewFile()
            }
            file.writeText(body!!.string())
        }
    }

    /**
     * 从res/values/server.xml获取服务器地址
     * @param context 应用环境信息
     * @return 服务器地址
     */
    fun getServerUrl(context: Context): String {
        return context.resources.getString(R.string.server_url)
    }
}