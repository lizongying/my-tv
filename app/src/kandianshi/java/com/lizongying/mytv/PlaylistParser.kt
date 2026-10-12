package com.lizongying.mytv

import java.net.URI
import java.util.Locale

data class PlaylistChannel(
    val title: String, val group: String, val logo: String, val urls: List<String>
)

object PlaylistParser {
    private val attribute = Regex("""([\w-]+)="([^"]*)"""")
    private val cctv = Regex("""(?i)^CCTV[-\s]?(\d+)(\+|Plus)?""")
    private val decorations = Regex("""(?i)\s*(?:\(\d+(?:p|i)\)|\[(?:Geo-blocked|Not 24/7)\]|\bHD\b|\bSD\b)""")

    fun parse(text: String, names: Map<String, String> = emptyMap(), maxUrls: Int = 8): List<PlaylistChannel> {
        val out = mutableListOf<PlaylistChannel>()
        var attrs = emptyMap<String, String>()
        var title = ""
        for (raw in text.removePrefix("\uFEFF").lineSequence()) {
            val line = raw.trim()
            if (line.startsWith("#EXTINF:")) {
                attrs = attribute.findAll(line).associate { it.groupValues[1] to it.groupValues[2] }
                title = line.substringAfterLast(",").trim()
            } else if (line.isNotEmpty() && !line.startsWith("#") && title.isNotEmpty()) {
                val id = attrs["tvg-id"].orEmpty().substringBefore("@")
                val category = attrs["group-title"].orEmpty()
                val url = line.substringBefore("|").substringBefore("$").replace("&amp;", "&").trim()
                val cnId = id.endsWith(".cn")
                val domesticGroup = category.contains("央视") || category.contains("卫视") || category.contains("地方")
                val named = names[id] ?: if (domesticGroup && id.any { it in '\u4e00'..'\u9fff' }) id else title
                val skip = category in setOf("Religious", "Shop") ||
                    named.contains("支持作者") || named.contains("更新时间") ||
                    named.contains("购物") || named.contains("测试") ||
                    (id.contains(".") && !cnId)
                if (!skip && (domesticGroup || cnId) && validUrl(url)) {
                    val canonical = normalize(named)
                    val group = when {
                        canonical.startsWith("CCTV", true) || canonical.startsWith("CGTN", true) -> "央视"
                        canonical.contains("卫视") || category.contains("卫视") -> "卫视"
                        else -> "地方台"
                    }
                    out.add(PlaylistChannel(canonical, group, attrs["tvg-logo"].orEmpty(), listOf(url)))
                }
                title = ""
                attrs = emptyMap()
            }
        }
        return merge(listOf(out),maxUrls)
    }

    fun validUrl(url: String): Boolean = try {
        val uri = URI(url)
        val host = uri.host.orEmpty().lowercase(Locale.ROOT)
        val privateHost = host == "localhost" || host.startsWith("127.") || host.startsWith("10.") ||
            host.startsWith("192.168.") || host == "::1" || host == "[::1]" ||
            Regex("""172\.(1[6-9]|2\d|3[01])\..*""").matches(host)
        uri.scheme in setOf("http", "https") && host.isNotEmpty() && !privateHost &&
            !uri.path.orEmpty().lowercase(Locale.ROOT).endsWith(".mp4")
    } catch (_: Exception) { false }

    fun normalize(title: String): String {
        val value = decorations.replace(title, "").trim().replace(Regex("^央视[- ]?(\\d+)"), "CCTV$1")
        val match = cctv.find(value)
        if (match != null) {
            val number = match.groupValues[1]
            val suffix = if (match.groupValues[2].isNotEmpty()) "+" else ""
            if (number == "4") listOf("美洲", "欧洲", "亚洲").firstOrNull { value.contains(it) }?.let { return "CCTV4" + it }
            if (value.substring(match.range.last + 1).trim().startsWith("K", true)) return "CCTV" + number + "K"
            return "CCTV" + number + suffix
        }
        return value.replace("高清", "").trim()
    }

    fun merge(lists: List<List<PlaylistChannel>>, maxUrls: Int = 8): List<PlaylistChannel> {
        val map = linkedMapOf<String, PlaylistChannel>()
        for (item in lists.flatten()) {
            val key = item.title.lowercase(Locale.ROOT)
            val old = map[key]
            map[key] = if (old == null) item else old.copy(
                logo = old.logo.ifBlank { item.logo },
                urls = (old.urls + item.urls).distinct().take(maxUrls)
            )
        }
        return map.values.sortedWith(compareBy<PlaylistChannel>(
            { when (it.group) { "央视" -> 0; "卫视" -> 1; else -> 2 } },
            { if (it.group == "央视") cctv.find(it.title)?.groupValues?.get(1)?.toIntOrNull() ?: 100 else 0 },
            { it.title }
        ))
    }
}
