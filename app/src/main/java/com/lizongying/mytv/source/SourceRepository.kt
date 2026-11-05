package com.lizongying.mytv.source

import android.content.Context
import com.google.gson.Gson
import com.lizongying.mytv.TV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request as HttpRequest
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

class SourceRepository(private val context: Context) {

    data class SourceStatus(
        val id: String,
        val name: String,
        val ok: Boolean,
        val count: Int,
        val error: String,
    )

    data class RefreshResult(
        val rows: Map<String, List<TV>>,
        val statuses: List<SourceStatus>,
    )

    private sealed class Fetched {
        class Ok(val channels: List<M3uChannel>) : Fetched()
        class Err(val message: String) : Fetched()
    }

    private data class CacheEntry(
        val channels: List<M3uChannel>? = null,
    )

    private data class CacheFile(
        val updatedAt: Long = 0L,
        val sources: Map<String, CacheEntry?>? = null,
    )

    private val gson = Gson()
    private val cacheFile = File(context.filesDir, CACHE_NAME)
    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    fun loadCachedRows(): Map<String, List<TV>>? {
        val cached = readCache() ?: return null
        val sources = SourceStore.enabled()
        if (sources.isEmpty()) {
            return null
        }
        return ChannelListBuilder.build(sources, resolveChannels(sources, cached.sources))
    }

    suspend fun refresh(): RefreshResult = withContext(Dispatchers.IO) {
        val sources = SourceStore.enabled()
        if (sources.isEmpty()) {
            return@withContext RefreshResult(emptyMap(), emptyList())
        }

        val cached = readCache() ?: CacheFile()
        val fetched = coroutineScope {
            sources.map { source -> async { source to fetch(source) } }.awaitAll()
        }

        val entries = cached.sources.orEmpty().toMutableMap()
        val statuses = mutableListOf<SourceStatus>()
        for ((source, result) in fetched) {
            val previous = entries[source.id]?.channels.orEmpty()
            when (result) {
                is Fetched.Ok -> {
                    entries[source.id] = CacheEntry(result.channels)
                    statuses.add(
                        SourceStatus(source.id, source.name, true, result.channels.size, "")
                    )
                }

                is Fetched.Err -> {
                    entries[source.id] = CacheEntry(previous)
                    statuses.add(
                        SourceStatus(source.id, source.name, false, previous.size, result.message)
                    )
                }
            }
        }
        entries.keys.retainAll(SourceStore.list().map { it.id }.toSet())
        writeCache(CacheFile(System.currentTimeMillis(), entries))

        val rows = ChannelListBuilder.build(sources, resolveChannels(sources, entries))
        RefreshResult(rows, statuses)
    }

    private fun resolveChannels(
        sources: List<Source>,
        cached: Map<String, CacheEntry?>?,
    ): Map<String, List<M3uChannel>> {
        val result = mutableMapOf<String, List<M3uChannel>>()
        for (source in sources) {
            val channels = cached?.get(source.id)?.channels ?: continue
            if (channels.isNotEmpty()) {
                result[source.id] = channels
            }
        }
        return result
    }

    private fun fetch(source: Source): Fetched {
        return try {
            client.newCall(HttpRequest.Builder().url(source.url).build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return Fetched.Err("HTTP ${response.code()}")
                }
                val body = response.body() ?: return Fetched.Err("响应为空")
                val text = readBody(body) ?: return Fetched.Err("内容过大")
                val channels = M3uParser.parse(text)
                if (channels.isEmpty()) {
                    Fetched.Err("未解析到频道")
                } else {
                    Fetched.Ok(channels)
                }
            }
        } catch (e: Exception) {
            Fetched.Err(errorOf(e))
        }
    }

    private fun readBody(body: okhttp3.ResponseBody): String? {
        val stream = body.byteStream()
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(8 * 1024)
        while (true) {
            val read = stream.read(chunk)
            if (read < 0) {
                break
            }
            buffer.write(chunk, 0, read)
            if (buffer.size() > MAX_BYTES) {
                return null
            }
        }
        val bytes = buffer.toByteArray()
        val textCharset = body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
        var text = try {
            String(bytes, textCharset)
        } catch (e: Exception) {
            String(bytes, Charsets.UTF_8)
        }
        if (text.contains('\uFFFD')) {
            val fallback = try {
                String(bytes, charset("GB18030"))
            } catch (e: Exception) {
                null
            }
            if (fallback != null && !fallback.contains('\uFFFD')) {
                text = fallback
            }
        }
        return text
    }

    private fun errorOf(e: Exception): String {
        return when (e) {
            is java.net.SocketTimeoutException -> "请求超时"
            is java.net.UnknownHostException -> "域名解析失败"
            is java.net.ConnectException -> "连接失败"
            else -> e.message?.take(60) ?: "请求失败"
        }
    }

    private fun readCache(): CacheFile? {
        if (!cacheFile.exists()) {
            return null
        }
        return try {
            gson.fromJson(cacheFile.readText(), CacheFile::class.java)
        } catch (e: Exception) {
            cacheFile.delete()
            null
        }
    }

    private fun writeCache(cache: CacheFile) {
        try {
            cacheFile.writeText(gson.toJson(cache))
        } catch (e: Exception) {
            // 缓存写失败不影响本次展示
        }
    }

    companion object {
        private const val CACHE_NAME = "channels.json"
        private const val CONNECT_TIMEOUT_SECONDS = 10L
        private const val READ_TIMEOUT_SECONDS = 15L
        private const val MAX_BYTES = 5 * 1024 * 1024
    }
}
