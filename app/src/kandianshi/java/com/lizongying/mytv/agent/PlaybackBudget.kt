package com.lizongying.mytv.agent

/** A catalogue refresh never renews a failed channel's loading deadline. */
class PlaybackBudget(private val windowMs: Long=45_000) {
    init { require(windowMs>0) }
    private var channel=""
    private var deadline=0L
    var exhausted=false; private set
    fun select(channel: String,now: Long,explicit: Boolean=false) {
        if(this.channel!=channel || explicit || deadline==0L) {
            this.channel=channel;deadline=now+windowMs;exhausted=false
        }
    }
    fun remaining(now: Long): Long=if(exhausted) 0 else (deadline-now).coerceAtLeast(0)
    fun finish() { exhausted=true }
    fun recover(now: Long) { deadline=now+windowMs;exhausted=false }
}
