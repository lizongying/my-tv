package com.lizongying.mytv.agent

import org.json.JSONObject

object AutoVisionPolicy {
    const val RECHECK_MS=30*60*1000L
    const val REVIEW_MS=7*MemoryPolicy.DAY
    fun needsInspection(cached: JSONObject?, review: JSONObject?, now: Long): Boolean =
        cached == null || cached.optString("kind") == "suspect" &&
            (review == null || now < review.optLong("at") || now-review.optLong("at") >= RECHECK_MS)

    /** Only new model responses may enter here; cached conclusions are never another vote. */
    fun observe(book: JSONObject, ads: JSONObject, route: VisionRoute, verdict: VisionVerdict, now: Long, run: String, autoBlock: Boolean): Boolean {
        val key="r:"+MemoryPolicy.id(route.url)
        if (verdict.kind != "suspect") { book.remove(key); return false }
        val previous=book.optJSONObject(key)?.takeIf {
            it.optLong("firstAt") in 0..it.optLong("at") && now >= it.optLong("at") && now-it.optLong("firstAt") < REVIEW_MS
        }
        val second=previous != null && previous.optString("run") != run && now-previous.optLong("at") >= RECHECK_MS
        if (second && autoBlock) {
            MemoryPolicy.pin(ads,route.url,"自动两轮确认 · "+route.title,now,
                reason="两轮独立识别相隔至少 30 分钟；各轮三帧均无台标、明确广告，模型置信度至少 0.93。"+verdict.reason)
            book.remove(key); book.remove("v:"+MemoryPolicy.id(route.url)); return true
        }
        if (previous == null || second) {
            book.put(key,JSONObject().put("kind","auto-review").put("url",route.url).put("title",route.title.take(120))
                .put("firstAt",previous?.optLong("firstAt") ?: now).put("at",now).put("run",run.take(80)))
        }
        return false
    }
}

/** Reads RGBA OpenGL rows bottom-to-top; no cropping or loss of corner logos. */
object FramePixels {
    fun argb(rgba: ByteArray,width: Int,height: Int): IntArray {
        require(width in 1..960 && height in 1..960 && rgba.size == width*height*4)
        return IntArray(width*height) { index ->
            val x=index%width; val y=index/width; val p=((height-1-y)*width+x)*4
            ((rgba[p+3].toInt() and 255) shl 24) or ((rgba[p].toInt() and 255) shl 16) or
                ((rgba[p+1].toInt() and 255) shl 8) or (rgba[p+2].toInt() and 255)
        }
    }
}
