package com.lizongying.mytv

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 *@author LeGend
 *@date 2024/2/4 22:42
 */
object ChannelUtils {
    /**
     * 获取服务器channel版本
     *
     * @param context Context
     *
     * @return 服务器channel版本
     */
    suspend fun getServerVersion(context: Context): Int {
        return withContext(Dispatchers.IO) {
            val client = okhttp3.OkHttpClient.Builder().connectTimeout(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(1, java.util.concurrent.TimeUnit.SECONDS).build()
            client.newCall(okhttp3.Request.Builder().url(getServerVersionUrl(context)).build()).execute()
                .use { response ->
                    if (!response.isSuccessful) throw java.io.IOException("Unexpected code $response")
                    val body = response.body()
                    body?.string()?.toInt() ?: 0
                }
        }
    }

    /**
     * 获取服务器channel
     *
     * @param url String 服务器地址
     *
     * @return Array<TV> 服务器channel
     *
     * @throws java.io.IOException 网络请求失败
     */
    suspend fun getServerChannel(url: String): List<TV> {
        val result = withContext(Dispatchers.IO) {
            val client = okhttp3.OkHttpClient.Builder().connectTimeout(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(1, java.util.concurrent.TimeUnit.SECONDS).build()
            val request = okhttp3.Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw java.io.IOException("Unexpected code $response")
                val body = response.body()
                body?.string() ?: ""
            }
        }
        return withContext(Dispatchers.Default) {
            val type = object : com.google.gson.reflect.TypeToken<List<TV>>() {}.type
            com.google.gson.Gson().fromJson(result, type)
        }
    }

    /**
     * 获取服务器地址
     *
     * @param context Context
     *
     * @return 服务器地址
     */
    fun getServerUrl(context: Context): String {
        return context.resources.getString(R.string.server_url)
    }

    /**
     * 获取serverVersion的URL
     *
     * @param context Context
     *
     * @return serverVersionURL 服务器版本地址
     */
    suspend fun getServerVersionUrl(context: Context): String {
        return withContext(Dispatchers.IO) {
            context.resources.getString(R.string.server_version_url)
        }
    }


    /**
     * 获取本地channel版本
     *
     * @param context Context
     *
     * @return 本地channel
     */
    suspend fun getLocalVersion(context: Context): Int {
        return withContext(Dispatchers.IO) {
            val file = File(getAppDirectory(context), "channels")
            //检查本地是否已经有保存的channels.json,若无保存的Channel.json则从读取assert中文件
            val savedVersion =
                context.getSharedPreferences("saved_version", Context.MODE_PRIVATE).getInt("version", Integer.MIN_VALUE)
            if (!file.exists() || savedVersion == Integer.MIN_VALUE) {
                context.resources.getInteger(R.integer.local_channel_version)
            } else {
                savedVersion
            }
        }
    }

    /**
     * 获取本地可读取的目录
     * @param context Context
     *
     * @return 可读取的目录
     */
    private fun getAppDirectory(context: Context): File {
        return context.filesDir
    }

    /**
     * 获取本地channel
     *
     * @param context Context
     *
     * @return Array<TV> 本地channel
     */
    suspend fun getLocalChannel(context: Context): List<TV> {
        val str = withContext(Dispatchers.IO) {
            if (File(getAppDirectory(context), "channels").exists()) {
                File(getAppDirectory(context), "channels").readText()
            } else {
                context.resources.openRawResource(R.raw.channels).bufferedReader().use { it.readText() }
            }
        }
        return withContext(Dispatchers.Default) {
            val type = object : com.google.gson.reflect.TypeToken<List<TV>>() {}.type
            com.google.gson.Gson().fromJson(str, type)
        }
    }

    /**
     * 更新channels.json
     *
     * @param context Context
     *
     * @return 无
     *
     * @throws java.io.IOException 写入失败
     */
    suspend fun updateLocalChannel(context: Context, version: Int, channels: List<TV>) {
        withContext(Dispatchers.IO) {
            val file = File(getAppDirectory(context), "channels")
            if (!file.exists()) {
                file.createNewFile()
            }
            file.writeText(com.google.gson.Gson().toJson(channels))
            context.getSharedPreferences("saved_version", Context.MODE_PRIVATE).edit().putInt(
                "version", version
            ).apply()
        }
    }
}
