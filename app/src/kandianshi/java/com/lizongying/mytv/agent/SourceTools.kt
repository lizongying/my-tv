package com.lizongying.mytv.agent

import com.lizongying.mytv.PlaylistChannel
import com.lizongying.mytv.PlaylistParser
import java.net.URI
import java.util.Locale

object SourceTools {
    private val chinese = Regex("[\\u4e00-\\u9fff]")
    private val promotion = Regex("博彩|娱乐城|投注|支持作者|成人|美女直播|广告测试")
    private val absolute = Regex("https?://[^\\s<>\"'`\\\\]+", RegexOption.IGNORE_CASE)
    private val href = Regex("""(?i)href\s*=\s*["']([^"']+)["']""")
    private val markdown = Regex("""\[[^\]]*]\(([^\s)]+)(?:\s[^)]*)?\)""")
    private val topicLink = Regex("(?is)<a[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>")

    fun isHls(text: String): Boolean = text.contains("#EXT-X-TARGETDURATION") || text.contains("#EXT-X-STREAM-INF")

    fun parse(text: String, names: Map<String, String> = emptyMap()): List<PlaylistChannel> {
        if (isHls(text) || text.trimStart().startsWith("<")) return emptyList()
        if (text.contains("#EXTINF:")) {
            val adapted = text.lineSequence().map { line ->
                if (line.startsWith("#EXTINF:") &&
                    (chinese.containsMatchIn(line.substringAfterLast(',')) || line.substringAfterLast(',').startsWith("CCTV", true))) {
                    if (line.contains("group-title=")) {
                        val existing = Regex("""group-title="([^"]*)"""" ).find(line)?.groupValues?.get(1).orEmpty()
                        if (existing.contains("央视") || existing.contains("卫视") || existing.contains("地方") || existing in setOf("Religious", "Shop")) line
                        else line.replace(Regex("""group-title="[^"]*""""), "group-title=\"地方台\"")
                    }
                    else {
                        val split = line.lastIndexOf(',')
                        line.substring(0, split) + " group-title=\"地方台\"" + line.substring(split)
                    }
                } else line
            }.joinToString("\n")
            return PlaylistParser.parse(adapted, names,64).filterNot { promotion.containsMatchIn(it.title) }
        }
        var group = "地方台"
        val out = StringBuilder("#EXTM3U\n")
        for (raw in text.removePrefix("\uFEFF").lineSequence().take(15000)) {
            val line = raw.trim(); val split = line.indexOf(',')
            if (split <= 0) continue
            var title = line.substring(0, split).trim()
            val value = line.substring(split + 1).trim()
            if (value.equals("#genre#", true)) { group = title.take(40); continue }
            if (promotion.containsMatchIn(title) || title.length > 100) continue
            title = title.replace(Regex("^央视[- ]?(\\d+)"), "CCTV$1")
            if (!(chinese.containsMatchIn(title) || title.startsWith("CCTV", true) || title.startsWith("CGTN", true))) continue
            if (!PlaylistParser.validUrl(value)) continue
            val domestic = when { title.startsWith("CCTV", true) || title.startsWith("CGTN", true) -> "央视"
                title.contains("卫视") -> "卫视"
                else -> "地方台" }
            out.append("#EXTINF:-1 group-title=\"").append(domestic).append("\",").append(title.replace("\n", " "))
                .append('\n').append(value).append('\n')
        }
        return PlaylistParser.parse(out.toString(), names,64)
    }

    fun pageLinks(base: String, text: String): List<String> {
        val content = text.replace("\\/", "/").replace("&amp;", "&")
        val relevant = topicLink.findAll(content).filter { Regex("(?i)直播|节目源|电视台|频道|iptv|m3u").containsMatchIn(it.groupValues[2]) }
            .map { it.groupValues[1] }.toSet()
        val links = (absolute.findAll(content).map { it.value } + href.findAll(content).map { it.groupValues[1] } +
            markdown.findAll(content).map { it.groupValues[1] })
        return links.mapNotNull { raw -> runCatching {
            val value = URI(base).resolve(raw.trimEnd(')', ']', ';', ',')).toString()
            DiscoveryPolicy.canonical(value)
        }.getOrNull() }.filter { value ->
            val lower = value.lowercase(Locale.ROOT)
            lower.contains(".m3u") || lower.substringBefore('?').endsWith(".txt") ||
                (URI(value).host.equals("raw.githubusercontent.com",true) && lower.substringBefore('?').endsWith(".md")) ||
                (URI(value).host.equals("github.com",true) && Regex("(?i)iptv|live|tv").containsMatchIn(URI(value).path)) ||
                relevant.any { raw -> runCatching { DiscoveryPolicy.canonical(URI(base).resolve(raw).toString()) == value }.getOrDefault(false) } ||
                (URI(value).host == URI(base).host && (lower.contains("iptv") || lower.contains("live") || lower.contains("直播")))
        }.filterNot { Regex("/(?:issues|pulls|actions|settings|network|forks)(?:/|$)").containsMatchIn(URI(it).path.orEmpty()) }
            .distinct().sortedBy { value ->
                val path = URI(value).path.orEmpty().lowercase(Locale.ROOT)
                if (path.endsWith(".m3u") || path.endsWith(".m3u8") || path.endsWith(".txt")) 0 else 1
            }.take(12).toList()
    }

    fun pageTitle(text: String): String = Regex("(?is)<title[^>]*>(.*?)</title>").find(text)
        ?.groupValues?.get(1)?.replace(Regex("<[^>]+>"), " ")?.trim()?.take(120).orEmpty()

    fun directTitle(hint: String): String? {
        Regex("(?i)CCTV[- ]?\\d+(?:\\+|Plus)?|CGTN(?:[- ]?(?:English|Documentary))?").find(hint)?.let {
            return PlaylistParser.normalize(it.value)
        }
        Regex("[\\u4e00-\\u9fff]{2,8}卫视").find(hint)?.let { return it.value }
        val title = hint.substringBefore('_').substringBefore('|').substringBefore(" - ").replace("直播", "").trim()
        if (title.length in 4..20 && title.contains("频道") && chinese.containsMatchIn(title) &&
            !Regex("全国|大全|列表|电视|直播源").containsMatchIn(title)) return title
        return null
    }

    fun encode(channels: List<PlaylistChannel>): String = buildString {
        append("#EXTM3U\n")
        for (channel in channels) for (url in channel.urls) {
            append("#EXTINF:-1 group-title=\"").append(channel.group.replace("\"", ""))
                .append("\"")
            // Keep the original logo through persistence; cached lists used to lose tvg-logo here.
            RealLogoPolicy.safeUrl(channel.logo)?.takeIf { !it.contains('"') && !it.contains('<') }?.let {
                append(" tvg-logo=\"").append(it).append("\"")
            }
            append(',').append(channel.title.replace('\n', ' ').replace('\r', ' '))
                .append('\n').append(url).append('\n')
        }
    }
}

object HealthRanking {
    fun sort(urls: List<String>, health: Map<String, HealthRecord>, blocked: Set<String>, now: Long, includeRemoved: Boolean = false): List<String> =
        urls.filterNot { url -> (!includeRemoved && health[url]?.removedAt?.let { it > 0 } == true) || url in blocked || runCatching { URI(url).host?.lowercase(Locale.ROOT) in blocked.map { it.lowercase(Locale.ROOT) }.toSet() }.getOrDefault(false) }
            .sortedWith(compareBy<String> { url ->
                val h = health[url]
                when {
                    h == null || now - h.checkedAt > 24 * 60 * 60 * 1000L -> 1
                    h.passed -> 0
                    h.failures < 2 -> 2
                    else -> 3
                }
            }.thenBy { health[it]?.takeIf { h -> h.passed }?.latencyMs ?: Long.MAX_VALUE })
}
