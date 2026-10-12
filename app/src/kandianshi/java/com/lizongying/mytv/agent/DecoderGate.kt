package com.lizongying.mytv.agent

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex

/** Foreground playback has priority; both kinds of players share one decoder lease. */
object DecoderGate {
    private val guard=Any()
    private val wanted=mutableSetOf<Any>()
    private val decoder=Mutex()
    fun requestForeground(owner: Any) = synchronized(guard) { wanted.add(owner); Unit }
    suspend fun acquireForeground(owner: Any) {
        requestForeground(owner)
        if (!decoder.holdsLock(owner)) decoder.lock(owner)
        synchronized(guard) {
            if (owner !in wanted) { if (decoder.holdsLock(owner)) decoder.unlock(owner); throw CancellationException("播放器已离开") }
        }
    }
    fun releaseLease(owner: Any) = synchronized(guard) { if (decoder.holdsLock(owner)) decoder.unlock(owner) }
    fun releaseForeground(owner: Any) = synchronized(guard) {
        wanted.remove(owner); if (decoder.holdsLock(owner)) decoder.unlock(owner)
    }
    fun owns(owner: Any): Boolean=decoder.holdsLock(owner)
    fun foregroundWanted(): Boolean=synchronized(guard) { wanted.isNotEmpty() }
    fun tryBackground(owner: Any): Boolean=synchronized(guard) { wanted.isEmpty() && decoder.tryLock(owner) }
    fun backgroundAllowed(owner: Any): Boolean=synchronized(guard) { wanted.isEmpty() && decoder.holdsLock(owner) }
}

class ScanDeferred(message: String): Exception(message)
