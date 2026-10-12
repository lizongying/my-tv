package com.lizongying.mytv.agent

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

object HttpFetch {
    class UnverifiedMediaException : IllegalStateException("需要电视实际解码验证")
    data class Check(val url: String, val passed: Boolean, val conclusive: Boolean, val latency: Long, val checkedAt: Long = System.currentTimeMillis()) {
        val failed: Boolean get() = conclusive && !passed
    }
    fun check(url: String): Check = try { Check(url, true, true, probe(url)) }
        catch (_: UnverifiedMediaException) { Check(url, false, false, Long.MAX_VALUE) }
        catch (_: Exception) { Check(url, false, true, Long.MAX_VALUE) }
    fun encryptedPlaylist(text: String): Boolean = text.lineSequence().any {
        it.trimStart().startsWith("#EXT-X-KEY:") && !Regex("(?:^|[, :])METHOD=NONE(?:,|$)").containsMatchIn(it)
    }
    data class Reply(val url: String, val bytes: ByteArray, val type: String, val headers: Map<String,String> = emptyMap())
    fun get(url: String, maxBytes: Int = 512 * 1024, sample: Boolean = false, headers: Map<String,String> = emptyMap()): Reply {
        var address = url
        val deadline = System.nanoTime() + 10_000_000_000L
        repeat(4) {
            require(System.nanoTime() < deadline) { "请求超时" }
            NetworkPolicy.publicUrl(address)
            val connection = URL(address).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 4500; connection.readTimeout = 4500
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) KanDianshi/1.2")
            connection.setRequestProperty("Accept-Encoding", "identity")
            // Browser parsing never forwards cookies, authorization or the configured model API key.
            headers.filterKeys { it.lowercase() in setOf("accept","user-agent","referer","origin") }.forEach { (k,v) ->
                if(v.length<=2048 && v.none { it=='\r' || it=='\n' }) connection.setRequestProperty(k,v)
            }
            if (sample) connection.setRequestProperty("Range", "bytes=0-" + (maxBytes - 1))
            try {
                if (connection.responseCode in setOf(301, 302, 303, 307, 308)) {
                    address = URI(address).resolve(connection.getHeaderField("Location") ?: error("跳转地址缺失")).toString()
                } else {
                    require(connection.responseCode in setOf(200, 206)) { "HTTP " + connection.responseCode }
                    val result = ByteArrayOutputStream()
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(8192)
                        while (result.size() < maxBytes) {
                            require(System.nanoTime() < deadline) { "请求超时" }
                            val n = input.read(buffer, 0, minOf(buffer.size, maxBytes - result.size()))
                            if (n < 0) break
                            result.write(buffer, 0, n)
                        }
                        if (!sample) require(input.read() == -1) { "来源文件超过大小上限" }
                    }
                    return Reply(address, result.toByteArray(), connection.contentType.orEmpty(),
                        connection.headerFields.filterKeys { it!=null }.mapValues { it.value.firstOrNull().orEmpty() })
                }
            } finally { connection.disconnect() }
        }
        error("跳转次数过多")
    }

    fun text(url: String, maxBytes: Int = 2 * 1024 * 1024): String = get(url, maxBytes).bytes.toString(Charsets.UTF_8)

    fun probe(url: String, fetch: (String) -> Reply = { get(it, 128 * 1024, sample = true) }): Long {
        val start = System.nanoTime()
        var target = url
        for (depth in 0..3) {
            val response = fetch(target)
            val bytes = response.bytes
            require(bytes.isNotEmpty()) { "媒体数据为空" }
            if (bytes.size < 32) throw UnverifiedMediaException()
            val text = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF").trimStart()
            if (text.startsWith("#EXTM3U")) {
                if (encryptedPlaylist(text)) throw UnverifiedMediaException()
                val lines = text.lineSequence().map(String::trim).filter { it.isNotEmpty() }.toList()
                val child = lines.firstOrNull { !it.startsWith("#") } ?: error("清单没有媒体地址")
                target = URI(response.url).resolve(child).toString()
                continue
            }
            require(!text.startsWith("<") && !text.startsWith("{") && !response.type.contains("text/html")) { "返回网页或错误信息" }
            val ts = (0 until minOf(188, bytes.size)).any { i -> bytes[i].toInt() and 255 == 0x47 &&
                i + 188 < bytes.size && bytes[i + 188].toInt() and 255 == 0x47 }
            val mp4 = bytes.size > 12 && bytes.copyOfRange(4, 8).toString(Charsets.US_ASCII) in setOf("ftyp", "styp", "moof", "moov", "sidx")
            val flv = bytes.take(3).toByteArray().toString(Charsets.US_ASCII) == "FLV"
            if (!(ts || mp4 || flv)) throw UnverifiedMediaException()
            return (System.nanoTime() - start) / 1_000_000
        }
        throw UnverifiedMediaException()
    }
}
