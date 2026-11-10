package com.lizongying.mytv.agent

import org.json.JSONObject

data class HealthRecord(
    val passed: Boolean,
    val checkedAt: Long,
    val latencyMs: Long,
    val failures: Int = 0,
    val firstFailureAt: Long = 0,
    val lastFailureAt: Long = 0,
    val removedAt: Long = 0,
    val playbackFailed: Boolean = false,
    val lastPlaybackSuccess: Long = 0,
    val recoveryHits: Int = 0,
    val manualRemoval: Boolean = false,
    val reason: String = ""
)

/** Transport checks cannot overrule a decoder failure or a user's black-screen report. */
object HealthPolicy {
    const val RETRY_GAP_MS = 30_000L
    const val FAILURE_LIMIT = 3
    const val RECENT_PLAYBACK_MS = 6 * 60 * 60 * 1000L

    fun observe(previous: HealthRecord?, passed: Boolean, decoded: Boolean, now: Long, latency: Long, reason: String): HealthRecord {
        val old = previous ?: HealthRecord(false, 0, Long.MAX_VALUE)
        if (old.manualRemoval) return old.copy(checkedAt = now, latencyMs = latency)
        if (passed && decoded) return HealthRecord(true, now, latency, lastPlaybackSuccess = now)
        if (passed) {
            if (old.playbackFailed) return old.copy(checkedAt = now, latencyMs = latency)
            if (old.removedAt > 0) {
                val hits = old.recoveryHits + if (old.recoveryHits == 0 || now - old.checkedAt >= RETRY_GAP_MS) 1 else 0
                if (hits < 2) return old.copy(checkedAt = now, latencyMs = latency, recoveryHits = hits)
            }
            return HealthRecord(true, now, latency, lastPlaybackSuccess = old.lastPlaybackSuccess)
        }
        if (!decoded && !old.playbackFailed && old.lastPlaybackSuccess > 0 && now - old.lastPlaybackSuccess in 0 until RECENT_PLAYBACK_MS)
            return old
        // Old installations did not record spaced attempts: start a fresh failure sequence.
        val fresh = old.firstFailureAt <= 0 || now < old.lastFailureAt
        val count = if (fresh) 1 else if (now - old.lastFailureAt >= RETRY_GAP_MS) minOf(20, old.failures + 1) else old.failures
        val first = if (fresh) now else old.firstFailureAt
        val last = if (fresh || count != old.failures) now else old.lastFailureAt
        val remove = old.removedAt.takeIf { it > 0 } ?: if (count >= FAILURE_LIMIT && now - first >= 2 * RETRY_GAP_MS) now else 0
        return HealthRecord(false, now, latency, count, first, last, remove,
            old.playbackFailed || decoded, old.lastPlaybackSuccess, 0, false,
            if (!decoded && old.playbackFailed) old.reason else reason.take(80))
    }

    fun manuallyRemove(previous: HealthRecord?, now: Long, reason: String = "手动报告黑屏 / 无效"): HealthRecord =
        (previous ?: HealthRecord(false, now, Long.MAX_VALUE)).copy(passed = false, checkedAt = now,
            removedAt = now, manualRemoval = true, reason = reason.take(80))
}

object HealthCodec {
    fun read(o: JSONObject): HealthRecord = HealthRecord(o.optBoolean("passed"), o.optLong("at"),
        o.optLong("latency", Long.MAX_VALUE), o.optInt("failures"), o.optLong("firstFailureAt"),
        o.optLong("lastFailureAt"), o.optLong("removedAt"), o.optBoolean("playbackFailed"),
        o.optLong("lastPlaybackSuccess", if (o.optString("kind") == "实际播放" && o.optBoolean("passed")) o.optLong("at") else 0),
        o.optInt("recoveryHits"), o.optBoolean("manualRemoval"), o.optString("reason"))

    fun write(h: HealthRecord): JSONObject = JSONObject().put("passed", h.passed).put("at", h.checkedAt)
        .put("latency", h.latencyMs).put("failures", h.failures).put("firstFailureAt", h.firstFailureAt)
        .put("lastFailureAt", h.lastFailureAt).put("removedAt", h.removedAt).put("playbackFailed", h.playbackFailed)
        .put("lastPlaybackSuccess", h.lastPlaybackSuccess).put("recoveryHits", h.recoveryHits)
        .put("manualRemoval", h.manualRemoval).put("reason", h.reason)
}

object NetworkEvidence {
    fun permitsFailures(connected: Boolean, batchHasSuccess: Boolean, controlProbe: () -> Boolean): Boolean =
        connected && (batchHasSuccess || controlProbe())
}
