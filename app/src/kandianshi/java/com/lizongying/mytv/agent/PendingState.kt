package com.lizongying.mytv.agent

/** Bounded, last-value-wins journal; the caller provides a durable atomic sink. */
class PendingState(private val limit: Int = 8 * 1024 * 1024) {
    private val pending = linkedMapOf<String,String>()
    @Synchronized fun get(key: String): String? = pending[key]
    @Synchronized fun put(key: String, value: String, sink: (String,String)->Unit) {
        pending[key]=value
        if(pending.values.sumOf { it.toByteArray(Charsets.UTF_8).size.toLong() } > limit) flush(sink)
    }
    @Synchronized fun immediate(key: String, value: String, sink: (String,String)->Unit) {
        sink(key,value); pending.remove(key)
    }
    @Synchronized fun flush(sink: (String,String)->Unit) {
        val it=pending.entries.iterator()
        while(it.hasNext()) { val e=it.next(); sink(e.key,e.value); it.remove() }
    }
}
