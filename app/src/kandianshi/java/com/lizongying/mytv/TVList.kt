package com.lizongying.mytv

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import com.lizongying.mytv.models.ProgramType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.json.JSONArray

object TVList {
    private data class Source(val asset: String, val urls: List<String>)
    private val sources = listOf(
        Source("iptv-org-cn.m3u", listOf("https://iptv-org.github.io/iptv/countries/cn.m3u")),
        Source("vbskycn-iptv4.m3u", listOf(
            "https://live.zbds.top/tv/iptv4.m3u",
            "https://raw.githubusercontent.com/vbskycn/iptv/master/tv/iptv4.m3u"
        )),
        Source("yuechan-gntv.m3u", listOf("https://raw.githubusercontent.com/YueChan/Live/main/GNTV.m3u"))
    )
    private val mutex = Mutex()
    private const val INTERVAL = 6 * 60 * 60 * 1000L
    data class RefreshResult(val channels: List<PlaylistChannel>, val updated: Int)

    fun channelNames(context: Context): Map<String, String> {
        val json = JSONObject(context.assets.open("channel_names.json").bufferedReader().use { it.readText() })
        return json.keys().asSequence().associateWith { json.getString(it) }
    }
    private fun read(context: Context, source: Source, map: Map<String, String>): List<PlaylistChannel> {
        val cache = AtomicFile(File(context.filesDir, source.asset))
        val cached = runCatching { cache.openRead().bufferedReader().use { it.readText() } }
            .getOrNull()?.let { PlaylistParser.parse(it, map) }
        return cached?.takeIf { it.size >= 5 } ?: context.assets.open(source.asset).bufferedReader().use {
            PlaylistParser.parse(it.readText(), map)
        }
    }
    fun load(context: Context, maxUrls: Int = 8, includeRemoved: Boolean = false): List<PlaylistChannel> {
        val map = channelNames(context)
        val merged = PlaylistParser.merge(sources.map { read(context, it, map) } + listOf(com.lizongying.mytv.agent.AgentRepository.extraChannels(context)), 64)
        val verified = JSONArray(context.assets.open("verified_urls.json").bufferedReader().use { it.readText() })
        val urls = (0 until verified.length()).map { verified.getString(it) }.toSet()
        return com.lizongying.mytv.agent.AgentRepository.rank(context, merged.map { it.copy(urls = it.urls.sortedBy { url -> url !in urls }) }, includeRemoved)
            .map { it.copy(urls = it.urls.take(maxUrls.coerceIn(1, 64))) }
    }

    suspend fun refresh(context: Context, force: Boolean = false): RefreshResult = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("playlist", Context.MODE_PRIVATE)
        if (!force && System.currentTimeMillis() - prefs.getLong("updated", 0) < INTERVAL) {
            return@withContext RefreshResult(load(context), 0)
        }
        if (!mutex.tryLock()) return@withContext RefreshResult(load(context), 0)
        try {
            val map = channelNames(context)
            var updated = 0
            for (source in sources) {
                for (url in source.urls) {
                    try {
                        val text = download(url)
                        require(PlaylistParser.parse(text, map).size >= 5) { "Empty or invalid playlist" }
                        val file = AtomicFile(File(context.filesDir, source.asset))
                        val output = file.startWrite()
                        try {
                            output.write(text.toByteArray(Charsets.UTF_8))
                            file.finishWrite(output)
                        } catch (error: Exception) { file.failWrite(output); throw error }
                        updated++
                        break
                    } catch (error: Exception) { Log.w("TVList", "Source refresh failed: " + url, error) }
                }
            }
            if (updated > 0) prefs.edit().putLong("updated", System.currentTimeMillis()).apply()
            RefreshResult(load(context), updated)
        } finally { mutex.unlock() }
    }

    private fun download(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 6000
        connection.readTimeout = 6000
        connection.setRequestProperty("User-Agent", "KanDianshi/1.0")
        try {
            require(connection.responseCode == 200) { "HTTP " + connection.responseCode }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val text = StringBuilder()
                val buffer = CharArray(8192)
                while (true) {
                    val size = reader.read(buffer)
                    if (size < 0) break
                    text.append(buffer, 0, size)
                    require(text.length <= 2 * 1024 * 1024) { "Playlist too large" }
                }
                text.toString()
            }
        } finally { connection.disconnect() }
    }

    fun grouped(channels: List<PlaylistChannel>): Map<String, List<TV>> {
        var id = 0
        return channels.groupBy { it.group }.mapValues { (group, list) ->
            list.map { TV(id++, it.title, it.title, it.urls, group,
                if (it.logo.isBlank()) R.drawable.logo else it.logo,
                programType = ProgramType.Y_PROTO) }
        }
    }
}
