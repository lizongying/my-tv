package com.lizongying.mytv.agent

/** Video progress, independent of transient READY/BUFFERING state changes. */
class FrameProgress(private val timeoutMs: Long=15_000) {
    init { require(timeoutMs>0) }
    private var active=false
    private var frames=0
    private var lastAdvance=0L
    fun start(rendered: Int,now: Long) { active=true;frames=rendered;lastAdvance=now }
    fun clear() { active=false }
    fun stalled(rendered: Int,now: Long): Boolean {
        if(!active) return false
        if(now<lastAdvance || rendered>frames) lastAdvance=now
        frames=rendered
        return now-lastAdvance>=timeoutMs
    }
}
