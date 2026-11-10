package com.lizongying.mytv.agent

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.Locale

data class CrawlLink(val url: String, val title: String = "", val kind: String = "page")

/** A small, persistent, topic-focused web frontier. No search provider is needed. */
object StationCrawler {
    val entries = listOf(
        CrawlLink("https://tv.cctv.com/live/", "央视直播目录"),
        CrawlLink("https://www.iqilu.com/", "山东广播电视台"),
        CrawlLink("https://www.gdtv.cn/", "广东广播电视台"),
        CrawlLink("https://www.jstv.com/", "江苏广播电视台"),
        CrawlLink("https://www.zjstv.com/", "浙江广播电视台"),
        CrawlLink("https://www.hbtv.com.cn/", "湖北广播电视台")
    )
    const val MAX_DEPTH = 3
    const val MAX_LINKS = 24
    private val relevant = Regex("(?i)直播|频道|电视台|网络台|看电视|卫视|CCTV|CGTN|(?:^|[/_.-])(?:live|channel|tv)(?:[/_.?-]|$)")
    private val media = Regex("(?i)\\.(?:m3u8|flv)(?:$|[?])")
    private val quoted = Regex("[\"']([^\"'\\s<>]{1,2048})[\"']")
    private val absolute = Regex("(?i)https?://[^\\s<>\"'`\\\\]+")
    private val tag = Regex("(?is)<(a|iframe|script|video|source)\\b([^>]*?)/?>")
    private val attr = Regex("(?is)(href|src|title|aria-label)\\s*=\\s*[\"']([^\"']*)[\"']")

    fun decode(value: String): String = value.replace("\\/", "/").replace("&amp;", "&")
        .replace("&#38;", "&").replace("&quot;", "\"").replace("&#39;", "'")
        .replace(Regex("\\\\u([0-9a-fA-F]{4})")) { it.groupValues[1].toInt(16).toChar().toString() }

    fun resolve(base: String, value: String): String? = runCatching {
        DiscoveryPolicy.canonical(URI(base).resolve(decode(value).trim()).toString())
    }.getOrNull()
    fun isMedia(url: String): Boolean = media.containsMatchIn(url)
    fun isAsset(url: String): Boolean = Regex("(?i)\\.(?:png|jpe?g|gif|webp|svg|ico|woff2?|ttf|mp4|mp3|aac|ts|m4s|map)(?:$|[?])").containsMatchIn(url)
    fun enqueue(state: JSONObject, link: CrawlLink, root: String, depth: Int, origin: String, now: Long): Boolean {
        val url=DiscoveryPolicy.canonical(link.url) ?: return false
        val q=state.optJSONArray("queue") ?: JSONArray().also { state.put("queue",it) }
        val meta=state.optJSONObject("meta") ?: JSONObject().also { state.put("meta",it) }
        val queued=(0 until q.length()).map { q.getString(it) }
        if(url in queued) return false
        if(now-(state.optJSONObject("history")?.optLong(MemoryPolicy.id(url),0) ?: 0)<6*3_600_000L) return false
        // A full queue of article/script pages must not discard a newly observed live stream.
        if(link.kind in setOf("media","playlist") && q.length()>=DiscoveryPolicy.MAX_QUEUE) {
            val victim=queued.lastOrNull { meta.optJSONObject(it)?.optString("kind") !in setOf("media","playlist") } ?: return false
            state.put("queue",JSONArray(queued.filterNot { it==victim }));meta.remove(victim)
        }
        if(DiscoveryPolicy.add(state,listOf(url),now)==0) return false
        meta.put(url,JSONObject().put("root",root).put("title",link.title.take(160)).put("kind",link.kind)
            .put("depth",depth).put("origin",origin))
        return true
    }
    fun isPlayerPage(url: String, text: String): Boolean = relevant.containsMatchIn(URI(url).path.orEmpty()) ||
        Regex("(?i)<video\\b|<iframe\\b|hls\\.js|videojs|jwplayer|ckplayer|player\\s*[:=(]").containsMatchIn(text)

    /** Ambiguous directory pages must not label every captured stream as their first channel. */
    fun channelTitle(hint: String, names: Collection<String> = emptyList()): String? {
        val clean = hint.replace(Regex("<[^>]+>"), " ")
        val cctv = Regex("(?i)CCTV[- ]?\\d+(?:K|\\+|Plus)?|CGTN(?:[- ]?(?:English|Documentary))?")
            .findAll(clean).map { com.lizongying.mytv.PlaylistParser.normalize(it.value) }.distinct().toList()
        if(cctv.size > 1) return null
        if(cctv.size == 1) return cctv.single()
        val known = names.filter { it.length >= 4 && clean.contains(it) }.distinct().sortedByDescending { it.length }
        if(known.isNotEmpty()) return known.first().takeIf { first -> known.all { first.contains(it) } }
        return SourceTools.directTitle(clean)
    }

    fun links(base: String, text: String, fallbackTitle: String = ""): List<CrawlLink> {
        val content = decode(text)
        val found = mutableListOf<CrawlLink>()
        for(m in tag.findAll(content)) {
            val type = m.groupValues[1].lowercase(Locale.ROOT)
            val attributes = attr.findAll(m.groupValues[2]).associate { it.groupValues[1].lowercase(Locale.ROOT) to it.groupValues[2] }
            val raw = attributes[if(type == "a") "href" else "src"] ?: continue
            val url = resolve(base, raw) ?: continue
            val start=m.range.last+1
            val end=if(type=="a") content.indexOf("</a",start,ignoreCase=true) else -1
            val body=if(end-start in 1..2000) content.substring(start,end) else ""
            val label = (attributes["title"] ?: attributes["aria-label"] ?: body)
                .replace(Regex("<[^>]+>"), " ").trim().take(120).ifBlank { fallbackTitle }
            if(isAsset(url)) continue
            val kind = when {
                isMedia(url) -> "media"
                type == "script" -> "script"
                type == "iframe" -> "page"
                relevant.containsMatchIn(label + " " + URI(url).path) -> "page"
                else -> continue
            }
            found.add(CrawlLink(url,label,kind))
        }
        // JSON/JavaScript player configuration, including protocol-relative and escaped addresses.
        for(raw in (quoted.findAll(content).map { it.groupValues[1] } + absolute.findAll(content).map { it.value })) {
            val url = resolve(base,raw.trimEnd(')',']',';',',')) ?: continue
            if(isMedia(url)) found.add(CrawlLink(url,fallbackTitle,"media"))
        }
        return found.distinctBy { it.url }.sortedBy { when(it.kind) { "media" -> 0; "page" -> 1; else -> 2 } }.take(MAX_LINKS)
    }

    /** Bounded JSON APIs may return extensionless play URLs beside a channel name. */
    fun jsonStreams(base: String, text: String, hint: String = ""): List<CrawlLink> {
        val out = mutableListOf<CrawlLink>(); var visited = 0
        fun walk(value: Any?, inherited: String, depth: Int) {
            if(depth > 8 || ++visited > 1500 || out.size >= MAX_LINKS) return
            if(value is JSONObject) {
                val title = listOf("channel_name","channelName","tvg_name","name","title").map { value.optString(it) }
                    .firstOrNull { channelTitle(it) != null } ?: inherited
                value.keys().asSequence().forEach { key ->
                    val item = value.opt(key)
                    if(item is String && Regex("(?i)^(?:play_?url|stream_?url|hls_?url|flv_?url|m3u8|hls|url|src)$").matches(key)) {
                        val url = resolve(base,item)
                        if(url != null && (isMedia(url) || Regex("(?i)play|stream|hls|flv|m3u8").containsMatchIn(key)))
                            out.add(CrawlLink(url,title,"media"))
                    } else if(item is JSONObject || item is JSONArray) walk(item,title,depth+1)
                }
            } else if(value is JSONArray) for(i in 0 until minOf(value.length(),500)) walk(value.opt(i),inherited,depth+1)
        }
        runCatching { val t=text.trim(); walk(if(t.startsWith("[")) JSONArray(t) else JSONObject(t),hint,0) }
        return out.distinctBy { it.url }.take(MAX_LINKS)
    }
}

data class HlsSnapshot(val ended: Boolean, val sequence: Long, val segments: List<String>, val targetSeconds: Int,
    val variant: String?, val encrypted: Boolean, val unsupported: Boolean)

object HlsInspection {
    fun parse(base: String, text: String): HlsSnapshot? {
        val lines=text.removePrefix("\uFEFF").lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        if(lines.firstOrNull()!="#EXTM3U" || !SourceTools.isHls(text)) return null
        var variant: String?=null
        val segments=mutableListOf<String>(); var segment=false; var child=false
        for(line in lines) {
            if(line.startsWith("#EXT-X-STREAM-INF:")) child=true
            else if(line.startsWith("#EXTINF:")) segment=true
            else if(!line.startsWith("#")) {
                val url=StationCrawler.resolve(base,line)
                if(child && variant==null) variant=url
                else if(segment && url!=null) segments.add(url)
                child=false; segment=false
            }
        }
        val seq=lines.firstOrNull { it.startsWith("#EXT-X-MEDIA-SEQUENCE:") }?.substringAfter(':')?.toLongOrNull() ?: 0
        val target=lines.firstOrNull { it.startsWith("#EXT-X-TARGETDURATION:") }?.substringAfter(':')?.toIntOrNull() ?: 6
        return HlsSnapshot(lines.any { it=="#EXT-X-ENDLIST" || it=="#EXT-X-PLAYLIST-TYPE:VOD" },seq,segments,target,
            variant,HttpFetch.encryptedPlaylist(text),lines.any { it.startsWith("#EXT-X-BYTERANGE") || it=="#EXT-X-I-FRAMES-ONLY" })
    }
    fun progressed(a: HlsSnapshot, b: HlsSnapshot): Boolean = !a.ended && !b.ended && b.segments.isNotEmpty() &&
        (b.sequence>a.sequence || b.segments.last()!=a.segments.lastOrNull())
}
