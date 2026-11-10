package com.lizongying.mytv.agent
/** Continue forward through all backups, including after a bounded loading round. */
object PlaybackRoutes {
    fun replacement(urls: List<String>,failed: Set<String>,health: Map<String,HealthRecord>): Int? = urls.indices.firstOrNull { urls[it] !in failed && health[urls[it]]?.let { h -> h.passed && h.removedAt==0L && !h.manualRemoval }==true }
    fun after(urls: List<String>,current: String,failed: Set<String>): Int? {
        if(urls.isEmpty()) return null
        val at=urls.indexOf(current)
        return (1..urls.size).asSequence().map { (at+it).mod(urls.size) }.firstOrNull { urls[it] !in failed }
    }
}
