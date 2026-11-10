package com.lizongying.mytv.agent

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.AtomicFile
import java.io.File

/** Noncritical progress may lose up to ten minutes after an abrupt process kill. */
object BufferedState {
    private val journal=PendingState()
    private val handler=Handler(HandlerThread("maintenance-storage").apply { start() }.looper)
    private val lock=Any()
    private var scheduled=false
    fun read(c: Context,name: String): String? = synchronized(lock) {
        journal.get(File(c.filesDir,name).absolutePath) ?: runCatching {
            AtomicFile(File(c.filesDir,name)).openRead().bufferedReader().use { it.readText() }
        }.getOrNull()
    }
    private fun sink(path: String,value: String) {
        val f=AtomicFile(File(path))
        if(runCatching { f.openRead().bufferedReader().use { it.readText() } }.getOrNull()==value) return
        val out=f.startWrite()
        try { out.write(value.toByteArray(Charsets.UTF_8)); f.finishWrite(out) }
        catch(e: Exception) { f.failWrite(out); throw e }
    }
    fun write(c: Context,name: String,value: String,deferred: Boolean=false): Unit = synchronized(lock) {
        val path=File(c.filesDir,name).absolutePath
        if(deferred) {
            journal.put(path,value,::sink)
            if(!scheduled) { scheduled=true; handler.postDelayed({ flush() },600_000) }
        } else journal.immediate(path,value,::sink)
    }
    fun flush(): Unit = synchronized(lock) {
        scheduled=false
        try { journal.flush(::sink) } catch(_: Exception) {
            scheduled=true; handler.postDelayed({ flush() },600_000)
        }
    }
}
