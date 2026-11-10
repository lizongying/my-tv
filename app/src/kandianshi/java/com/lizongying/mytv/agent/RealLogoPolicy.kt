package com.lizongying.mytv.agent

import org.json.JSONObject
import java.util.Locale

/** Exact aliases, never a fuzzy guess between channels such as CCTV5 and CCTV5+. */
object RealLogoPolicy {
    const val RETRY_MS = 24 * 3_600_000L
    const val REFRESH_MS = 7 * RETRY_MS
    const val BATCH = 12
    fun key(title: String): String {
        val clean=title.trim().uppercase(Locale.ROOT).replace(Regex("[\\s_-]+"),"")
            .replace("高清","").replace(Regex("\\((?:HD|SD|\\d+[PI])\\)"),"")
            .replace(Regex("(?:HD|SD)$"),"")
        if(clean.startsWith("CCTV4")) listOf("美洲","欧洲","亚洲").firstOrNull { clean.contains(it) }?.let { return "CCTV4"+it }
        val cctv=Regex("^CCTV(\\d+)(PLUS|\\+|K)?(?:$|[^A-Z0-9+])").find(clean)
        if(cctv!=null) return "CCTV"+cctv.groupValues[1]+when(cctv.groupValues[2]) { "PLUS" -> "+"; else -> cctv.groupValues[2] }
        return clean.take(80)
    }
    fun safeUrl(value: String): String? = runCatching { NetworkPolicy.publicUrl(value,false).toASCIIString() }.getOrNull()
    fun candidates(title: String, original: String?, index: JSONObject): List<String> {
        val record=index.optJSONObject(key(title));val urls=record?.optJSONArray("urls")
        val fromIndex=if(urls==null) emptyList() else (0 until minOf(urls.length(),3)).map { urls.optString(it) }
        // Known direct-access mirrors before a possibly broken external playlist logo.
        return (fromIndex+listOfNotNull(original)).mapNotNull(::safeUrl).distinct().take(4)
    }
    fun ready(entry: JSONObject, hasFile: Boolean, now: Long): Boolean =
        (!hasFile || now-entry.optLong("saved",0)>=REFRESH_MS) &&
            (entry.optLong("attempt",0)==0L || now-entry.optLong("attempt")>=RETRY_MS)
    fun usable(url: String, failures: JSONObject, now: Long): Boolean {
        val at=failures.optLong(MemoryPolicy.id(url),0)
        return at==0L || now-at>=RETRY_MS
    }
    fun limitMb(value: Int) = value.takeIf { it in setOf(2,5,10) } ?: 5
    fun bound(state: JSONObject): JSONObject {
        val entries=state.optJSONObject("entries") ?: JSONObject().also { state.put("entries",it) }
        entries.keys().asSequence().sortedByDescending { entries.optJSONObject(it)?.optLong("at") ?: 0 }
            .drop(500).toList().forEach(entries::remove)
        val failed=state.optJSONObject("failed") ?: JSONObject().also { state.put("failed",it) }
        failed.keys().asSequence().sortedByDescending { failed.optLong(it) }.drop(500).toList().forEach(failed::remove)
        while(state.toString().toByteArray(Charsets.UTF_8).size>128*1024) {
            if(failed.length()>0) failed.remove(failed.keys().asSequence().minBy { failed.optLong(it) })
            else if(entries.length()>0) entries.remove(entries.keys().asSequence().minBy { entries.optJSONObject(it)?.optLong("at") ?: 0 })
            else break
        }
        return state
    }
}
