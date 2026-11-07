package com.lizongying.mytv.agent
import com.lizongying.mytv.PlaylistChannel
import org.json.JSONObject

/** Unverified routes never become visible channels. Retry metadata stays bounded. */
object CandidatePolicy {
    const val LIMIT=1024
    const val MAX_BYTES=1024*1024
    fun add(book: JSONObject,channels: List<PlaylistChannel>,now: Long) {
        var used=book.toString().toByteArray(Charsets.UTF_8).size
        // One route per channel per round, rather than exhausting a single channel's mirrors.
        for(i in 0 until 8) for(ch in channels) {
            val url=ch.urls.getOrNull(i) ?: continue
            if(DiscoveryPolicy.canonical(url)==null) continue
            val id=MemoryPolicy.id(url)
            if(book.has(id)) continue
            if(book.length()>=LIMIT) {
                val evict=book.keys().asSequence().firstOrNull { book.getJSONObject(it).optLong("next")>now } ?: return
                book.remove(evict);used=book.toString().toByteArray(Charsets.UTF_8).size
            }
            val entry=JSONObject().put("url",url).put("title",ch.title).put("group",ch.group).put("logo",ch.logo).put("at",now).put("next",0).put("order",book.length())
            val extra=JSONObject().put(id,entry).toString().toByteArray(Charsets.UTF_8).size-2+if(book.length()>0) 1 else 0
            if(used+extra>MAX_BYTES) return
            book.put(id,entry);used+=extra
        }
    }
    fun due(book: JSONObject,now: Long,excluded: String): List<String> = book.keys().asSequence()
        .filter { book.getJSONObject(it).optLong("next")<=now && book.getJSONObject(it).optString("url")!=excluded }
        .sortedWith(compareBy<String> { book.getJSONObject(it).optLong("at") }.thenBy { book.getJSONObject(it).optInt("order") }).toList()
    fun failed(book: JSONObject,id: String,now: Long) {
        val entry=book.optJSONObject(id) ?: return
        val attempts=entry.optInt("attempts")+1
        if(attempts>=3) book.remove(id) else entry.put("attempts",attempts).put("next",now+6*3_600_000L).put("at",now)
    }
}
object PlaybackRoute { @Volatile var url: String=""; @Volatile var buffering: Boolean=false }

object LightHealthSelection {
    fun select(urls: List<String>,health: Map<String,HealthRecord>,cursor: Int,active: String,now: Long,limit: Int=6): Pair<List<String>,Int> {
        if(urls.isEmpty()) return emptyList<String>() to 0
        val review=urls.filter { u -> u!=active && health[u]?.let { it.failures in 1..2 && it.removedAt==0L && now-it.lastFailureAt>=HealthPolicy.RETRY_GAP_MS }==true }
            .sortedBy { health[it]!!.lastFailureAt }.take(limit/2)
        val chosen=review.toMutableList();var visited=0;val start=cursor.mod(urls.size)
        while(chosen.size<limit && visited<urls.size) {
            val u=urls[(start+visited)%urls.size];visited++
            if(u!=active && u !in chosen) chosen.add(u)
        }
        return chosen to ((start+visited)%urls.size)
    }
}

object ChannelSelection {
    fun choose(rows: List<Pair<String,List<String>>>,active: String,title: String,fallback: Int): Int {
        val byRoute=rows.indexOfFirst { active.isNotBlank() && active in it.second }
        if(byRoute>=0) return byRoute
        val byTitle=rows.indexOfFirst { it.first==title }
        return if(byTitle>=0) byTitle else fallback.coerceIn(0,rows.lastIndex.coerceAtLeast(0))
    }
}
