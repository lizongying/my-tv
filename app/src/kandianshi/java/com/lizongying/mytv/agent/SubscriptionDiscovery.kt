package com.lizongying.mytv.agent

import java.net.URI
import java.util.Locale

/** Data-only compatibility with IPTV collector subscription formats; no downloaded code runs. */
object SubscriptionDiscovery {
    const val MAX_LINKS = 24
    private const val MAX_TEXT = 512 * 1024
    private val configNames = setOf("config.ini", "user_config.ini", "subscribe.txt", "user_subscribe.txt")
    private val proxyHosts = setOf("ghproxy.net", "github.moeyy.xyz", "gh-proxy.com")
    private val channelLine = Regex("""^[^,=]+,\s*(?:https?://|#genre#)""")
    private val entry = Regex("""(?i)^(https?://[^\s]+)(?:\s+UA\s*=\s*"[^"\r\n]*")?\s*$""")
    val entries = listOf(
        CrawlLink("https://raw.githubusercontent.com/testtff/TV/master/config/config.ini", "TV 采集工具的订阅入口", "subscription"),
        CrawlLink("https://raw.githubusercontent.com/Guovin/iptv-api/master/config/subscribe.txt", "IPTV-API 订阅入口", "subscription")
    )
    fun isConfiguration(url: String): Boolean = runCatching {
        URI(url).path.substringAfterLast('/').lowercase(Locale.ROOT) in configNames
    }.getOrDefault(false)
    private fun lines(text: String) = text.removePrefix("\uFEFF").lineSequence().take(2000)
        .map(String::trim).filter { it.isNotEmpty() && !it.startsWith('#') && !it.startsWith(';') }.toList()
    private fun section(line: String) = line.startsWith('[') && line.endsWith(']')
    fun handles(url: String, text: String): Boolean {
        if (text.length > MAX_TEXT || text.toByteArray(Charsets.UTF_8).size > MAX_TEXT) return false
        // Comment-prefixed M3U metadata must be examined before stripping index comments.
        if (text.lineSequence().take(2000).any {
            val line=it.trimStart()
            line.startsWith("#EXTM3U") || line.startsWith("#EXTINF:") || line.startsWith("#EXT-X-") ||
                channelLine.containsMatchIn(line)
        }) return false
        if (isConfiguration(url)) return true
        val rows = lines(text)
        return rows.isNotEmpty() && rows.any { !section(it) } &&
            rows.all { section(it) || entry.matches(it) }
    }
    /** Only explicit public URLs are accepted; per-fetch DNS and redirect checks still apply. */
    fun address(raw: String): String? = runCatching {
        var value = entry.matchEntire(raw.trim())?.groupValues?.get(1) ?: return null
        val u = NetworkPolicy.publicUrl(value, false)
        if (u.host.lowercase(Locale.ROOT) in proxyHosts && u.port in setOf(-1, 443) && u.rawQuery == null) {
            val inner = u.rawPath.removePrefix("/")
            val wrapped = runCatching { NetworkPolicy.publicUrl(inner, false) }.getOrNull()
            if (wrapped?.host?.lowercase(Locale.ROOT) in setOf("github.com", "raw.githubusercontent.com")) value = inner
        }
        DiscoveryPolicy.canonical(DiscoveryPolicy.fetchAddress(value))
    }.getOrNull()
    fun links(url: String, text: String): List<CrawlLink> {
        if (!handles(url, text)) return emptyList()
        val rows = lines(text)
        val ini = runCatching { URI(url).path.endsWith(".ini", true) }.getOrDefault(false)
        val values = mutableListOf<String>()
        if (ini) {
            var settings = false
            var collecting = false
            var enabled = true
            for (line in rows) {
                if (section(line)) {
                    settings = line.equals("[Settings]", true); collecting = false; continue
                }
                if (!settings) continue
                val key = line.substringBefore('=').trim().lowercase(Locale.ROOT)
                if (key == "open_subscribe") {
                    enabled = line.substringAfter('=').trim().substringBefore(' ').lowercase(Locale.ROOT) !in setOf("false", "0", "no", "off")
                    collecting = false
                } else if (key == "subscribe_urls") {
                    collecting = true; values.add(line.substringAfter('=').trim().trimEnd(','))
                } else if (collecting && line.startsWith("http", true)) values.add(line.trimEnd(','))
                else collecting = false
            }
            if (!enabled) return emptyList()
        } else values.addAll(rows.filterNot(::section))
        return values.asSequence().flatMap { it.split(Regex(""",\s*(?=https?://)""", RegexOption.IGNORE_CASE)).asSequence() }
            .mapNotNull(::address).filter { it != DiscoveryPolicy.canonical(url) }.distinct().take(MAX_LINKS)
            .map { CrawlLink(it, "自动发现的订阅节目单", "playlist") }.toList()
    }
}
