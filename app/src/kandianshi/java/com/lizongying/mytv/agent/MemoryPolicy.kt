package com.lizongying.mytv.agent

import org.json.JSONObject
import java.net.URI
import java.security.MessageDigest
import java.util.Locale

data class MemorySettings(val mib: Int = 1, val days: Int = 7) {
    init { require(mib in listOf(1,2,5) && days in listOf(1,7,30,90)) }
    val bytes: Int get() = mib * 1024 * 1024
}

/** Small structured records; never conversation transcripts or image data. */
object MemoryPolicy {
    const val DAY = 86_400_000L
    const val AD_BYTES = 2 * 1024 * 1024
    const val AD_COUNT = 5000
    const val PLAN_BYTES = 384 * 1024
    fun bytes(o: JSONObject): Int = o.toString().toByteArray(Charsets.UTF_8).size
    fun id(url: String): String {
        // Keep every query parameter: different channels often share a path.
        val normalized = runCatching {
            val u = URI(url.trim())
            val scheme = u.scheme.lowercase(Locale.ROOT)
            val host = u.host.lowercase(Locale.ROOT)
            val port = if (u.port == -1 || scheme == "https" && u.port == 443 || scheme == "http" && u.port == 80) "" else ":" + u.port
            scheme + "://" + (u.rawUserInfo?.let { "$it@" } ?: "") + host + port +
                u.rawPath.orEmpty().ifEmpty { "/" } + (u.rawQuery?.let { "?$it" } ?: "")
        }.getOrDefault(url.trim().substringBefore('#'))
        return MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
    fun ttl(kind: String, settings: MemorySettings): Long = minOf(settings.days * DAY, when(kind) {
        "tv", "kept" -> settings.days * DAY
        "suspect" -> DAY
        "auto-review" -> 7*DAY
        "uncertain", "unavailable", "excluded" -> 6 * 3_600_000L
        "source" -> settings.days * DAY
        else -> 0L
    })
    fun fresh(v: JSONObject, now: Long, settings: MemorySettings): Boolean {
        val at = v.optLong("at", -1)
        return at >= 0 && now >= at && now - at < ttl(v.optString("kind"),settings)
    }
    fun trim(book: JSONObject, now: Long, settings: MemorySettings, maxBytes: Int = settings.bytes): JSONObject {
        book.keys().asSequence().toList().forEach { key ->
            if (book.optJSONObject(key)?.let { fresh(it,now,settings) } != true) book.remove(key)
        }
        val keys = book.keys().asSequence().sortedBy { book.getJSONObject(it).optLong("at") }.toList()
        var size = bytes(book)
        var count = book.length()
        for (key in keys) {
            if (size <= maxBytes && count <= 4000) break
            val entrySize = JSONObject.quote(key).toByteArray(Charsets.UTF_8).size + 1 +
                bytes(book.getJSONObject(key)) + if (count > 1) 1 else 0
            book.remove(key); size -= entrySize; count--
        }
        return book
    }
    fun putVision(book: JSONObject, route: VisionRoute, verdict: VisionVerdict, now: Long) {
        require(route.url.length <= 8192 && verdict.kind in setOf("tv","kept","suspect","uncertain","unavailable","excluded"))
        book.put("v:" + id(route.url),JSONObject().put("url",route.url).put("title",route.title.take(120))
            .put("kind",verdict.kind).put("reason",verdict.reason.take(180)).put("at",now))
    }
    fun cached(book: JSONObject, url: String, now: Long, settings: MemorySettings): JSONObject? =
        book.optJSONObject("v:" + id(url))?.takeIf { fresh(it,now,settings) && it.optString("url") == url }
    fun failedSource(book: JSONObject, url: String, now: Long) {
        val key = "s:" + id(url)
        val failures = ((book.optJSONObject(key)?.optInt("failures",0) ?: 0) + 1).coerceAtMost(5)
        val hours = listOf(1,6,24,72,168)[failures-1]
        book.put(key,JSONObject().put("kind","source").put("at",now).put("failures",failures)
            .put("until",now + hours * 3_600_000L))
    }
    fun cooling(book: JSONObject, url: String, now: Long, settings: MemorySettings): Boolean =
        book.optJSONObject("s:" + id(url))?.let { fresh(it,now,settings) && it.optLong("until") > now } ?: false
    fun pin(ads: JSONObject, url: String, title: String, now: Long, maxBytes: Int = AD_BYTES, maxCount: Int = AD_COUNT, reason: String = "") {
        val key = id(url)
        if (ads.has(key)) return
        require(ads.length() < maxCount) { "永久屏蔽名单已满，请先在记忆管理中解除不需要的条目" }
        val entry = JSONObject().put("title",title.take(120)).put("display",NetworkPolicy.displayUrl(url).take(180)).put("at",now)
        if (reason.isNotBlank()) entry.put("reason",reason.take(240))
        ads.put(key,entry)
        if (bytes(ads) > maxBytes) {
            ads.remove(key)
            throw IllegalArgumentException("永久屏蔽记忆已达 2 MB，请先解除不需要的条目")
        }
    }
    fun resolve(book: JSONObject, ads: JSONObject, url: String, kind: String, expectedAt: Long, now: Long) {
        require(kind in setOf("kept","removed"))
        val key = "v:" + id(url)
        val result = book.optJSONObject(key) ?: error("识别结果已过期或被清理")
        require(result.optLong("at") == expectedAt && result.optString("kind") == "suspect") { "识别结果已变化，请重新查看" }
        if (kind == "removed") {
            pin(ads,url,result.optString("title"),now)
            book.remove(key)
        } else result.put("kind","kept").put("at",now)
    }
    fun importLegacy(ads: JSONObject, recent: JSONObject, health: Map<String,HealthRecord>, results: List<Pair<String,JSONObject>>) {
        health.filterValues { it.manualRemoval && it.reason.contains("纯广告") }.forEach { (url,h) ->
            pin(ads,url,"已确认纯广告线路",h.removedAt)
        }
        results.forEach { (url,v) ->
            if (v.optString("kind") == "removed") pin(ads,url,v.optString("title"),v.optLong("at"))
            else if (v.optString("kind") in setOf("tv","kept","suspect","uncertain","unavailable","excluded"))
                putVision(recent,VisionRoute(v.optString("title"),url),VisionVerdict(v.optString("kind"),v.optString("reason")),v.optLong("at"))
        }
    }
    fun boundedRoutes(routes: List<VisionRoute>, maxBytes: Int = PLAN_BYTES): List<VisionRoute> {
        var size = 100
        return VisionPolicy.queue(routes).map { it.copy(title=it.title.take(120)) }.takeWhile {
            size += bytes(JSONObject().put("url",it.url).put("title",it.title)) + 1
            size <= maxBytes
        }
    }
}
