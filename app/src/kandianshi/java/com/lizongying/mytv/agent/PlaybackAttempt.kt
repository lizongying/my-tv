package com.lizongying.mytv.agent

/** Ignores stale video callbacks and counts each playback attempt only once. */
class PlaybackAttempt {
    private var generation = 0L
    var id = ""; private set
    var url = ""; private set
    var hasFirstFrame = false; private set
    var failed = false; private set
    private var successReported = false
    fun start(url: String): String {
        generation++; id = generation.toString(); this.url = url
        hasFirstFrame = false; failed = false; successReported = false
        return id
    }
    fun frame(requestId: String): Boolean {
        if (id != requestId || url.isEmpty() || failed) return false
        hasFirstFrame = true; return true
    }
    fun success(requestId: String): Boolean {
        if (id != requestId || !hasFirstFrame || failed || successReported) return false
        successReported = true; return true
    }
    fun fail(requestId: String): Boolean {
        if (id != requestId || url.isEmpty() || failed) return false
        failed = true; return true
    }
    fun clear() { start("") }
}
