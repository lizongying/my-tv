package com.lizongying.mytv
import com.lizongying.mytv.agent.DecoderGate
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class DecoderGateTest {
    @Test fun foregroundRequestImmediatelyStopsBackgroundPermission()=runBlocking {
        val bg=Any(); val fg=Any()
        try {
            assertTrue(DecoderGate.tryBackground(bg)); assertTrue(DecoderGate.backgroundAllowed(bg))
            DecoderGate.requestForeground(fg)
            assertFalse(DecoderGate.backgroundAllowed(bg)); assertFalse(DecoderGate.tryBackground(Any()))
        } finally { DecoderGate.releaseForeground(fg); DecoderGate.releaseLease(bg) }
    }
    @Test fun decoderIsNotSharedAndForegroundWaitsForRelease()=runBlocking {
        val bg=Any(); val fg=Any()
        try {
            assertTrue(DecoderGate.tryBackground(bg))
            val waiter=async { DecoderGate.acquireForeground(fg) }
            yield(); assertFalse(waiter.isCompleted); assertFalse(DecoderGate.backgroundAllowed(bg))
            DecoderGate.releaseLease(bg); waiter.await()
            assertTrue(DecoderGate.owns(fg)); assertFalse(DecoderGate.tryBackground(bg))
        } finally { DecoderGate.releaseForeground(fg); DecoderGate.releaseLease(bg) }
    }
    @Test fun wrongOwnerCannotUnlockDecoder()=runBlocking {
        val owner=Any(); val other=Any()
        try {
            DecoderGate.acquireForeground(owner); DecoderGate.releaseForeground(other); DecoderGate.releaseLease(other)
            assertTrue(DecoderGate.owns(owner))
        } finally { DecoderGate.releaseForeground(owner); DecoderGate.releaseForeground(other) }
    }
    @Test fun releasingLeaseKeepsVisibleForegroundPriority()=runBlocking {
        val fg=Any(); val bg=Any()
        try {
            DecoderGate.acquireForeground(fg); DecoderGate.releaseLease(fg)
            assertTrue(DecoderGate.foregroundWanted()); assertFalse(DecoderGate.tryBackground(bg))
            DecoderGate.releaseForeground(fg); assertTrue(DecoderGate.tryBackground(bg))
        } finally { DecoderGate.releaseForeground(fg); DecoderGate.releaseLease(bg) }
    }
    @Test fun cancelledForegroundWaiterDoesNotStealBackgroundLease()=runBlocking {
        val bg=Any(); val fg=Any()
        try {
            assertTrue(DecoderGate.tryBackground(bg))
            val waiting=launch { DecoderGate.acquireForeground(fg) }
            yield(); waiting.cancelAndJoin(); DecoderGate.releaseForeground(fg)
            assertTrue(DecoderGate.owns(bg)); assertTrue(DecoderGate.backgroundAllowed(bg))
        } finally { DecoderGate.releaseForeground(fg); DecoderGate.releaseLease(bg) }
    }
    @Test fun departedWaiterCannotAcquireAndLeakDecoder()=runBlocking {
        val bg=Any(); val fg=Any()
        try {
            assertTrue(DecoderGate.tryBackground(bg))
            val waiting=async { runCatching { DecoderGate.acquireForeground(fg) }.exceptionOrNull() }
            yield(); DecoderGate.releaseForeground(fg); DecoderGate.releaseLease(bg)
            assertTrue(waiting.await() is CancellationException)
            assertFalse(DecoderGate.owns(fg)); assertTrue(DecoderGate.tryBackground(bg))
        } finally { DecoderGate.releaseForeground(fg); DecoderGate.releaseLease(bg) }
    }
    @Test fun repeatedForegroundAcquireBySameOwnerIsSafe()=runBlocking {
        val fg=Any()
        try { DecoderGate.acquireForeground(fg); DecoderGate.acquireForeground(fg); assertTrue(DecoderGate.owns(fg)) }
        finally { DecoderGate.releaseForeground(fg) }
    }
    @Test fun oneDepartedForegroundDoesNotClearOtherForegroundPriority()=runBlocking {
        val one=Any(); val two=Any()
        try {
            DecoderGate.requestForeground(one); DecoderGate.requestForeground(two); DecoderGate.releaseForeground(one)
            assertTrue(DecoderGate.foregroundWanted()); assertFalse(DecoderGate.tryBackground(Any()))
        } finally { DecoderGate.releaseForeground(one); DecoderGate.releaseForeground(two) }
    }
}
