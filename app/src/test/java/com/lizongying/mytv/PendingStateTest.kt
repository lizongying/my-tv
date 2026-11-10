package com.lizongying.mytv
import com.lizongying.mytv.agent.PendingState
import org.junit.Assert.*
import org.junit.Test

class PendingStateTest {
    @Test fun thousandUpdatesBecomeOneWrite() {
        val p=PendingState(); val out=mutableListOf<Pair<String,String>>()
        repeat(1000) { p.put("health",it.toString()) { k,v -> out.add(k to v) } }
        assertEquals("999",p.get("health")); assertTrue(out.isEmpty())
        p.flush { k,v -> out.add(k to v) }; assertEquals(listOf("health" to "999"),out)
    }
    @Test fun criticalDecisionCannotBeOverwrittenByOldCheckpoint() {
        val p=PendingState();val out=mutableListOf<String>()
        p.put("health","before") { _,v -> out.add(v) }
        p.immediate("health","removed") { _,v -> out.add(v) }
        p.flush { _,v -> out.add(v) };assertEquals(listOf("removed"),out)
    }
    @Test fun failedFlushRemainsRetryable() {
        val p=PendingState();p.put("plan","cursor-9") { _,_ -> }
        try { p.flush { _,_ -> error("disk full") };fail() } catch(_: IllegalStateException) {}
        assertEquals("cursor-9",p.get("plan"))
        p.flush { _,_ -> };assertNull(p.get("plan"))
    }
    @Test fun byteLimitIncludesUnicode() {
        val p=PendingState(4);val writes=mutableListOf<String>()
        p.put("a","央视") { _,v -> writes.add(v) };assertEquals(listOf("央视"),writes)
    }
    @Test fun differentFilesAreNotMergedTogether() {
        val p=PendingState();val writes=mutableMapOf<String,String>()
        p.put("health","ok") { _,_ -> };p.put("plan","7") { _,_ -> }
        p.flush { k,v -> writes[k]=v };assertEquals(mapOf("health" to "ok","plan" to "7"),writes)
        p.flush { _,_ -> fail("empty flush") }
    }
}
