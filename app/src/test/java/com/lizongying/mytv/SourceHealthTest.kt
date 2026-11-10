package com.lizongying.mytv

import com.lizongying.mytv.agent.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SourceHealthTest {
    private val start = 1_000_000L
    private fun bad(old: HealthRecord?, time: Long, decoded: Boolean = false) =
        HealthPolicy.observe(old, false, decoded, time, Long.MAX_VALUE, if (decoded) "没有视频首帧" else "请求失败")
    private fun good(old: HealthRecord?, time: Long, decoded: Boolean = false) = HealthPolicy.observe(old, true, decoded, time, 10, "")
    private fun removed(decoded: Boolean = false): HealthRecord {
        val one = bad(null, start, decoded); val two = bad(one, start + 30_000, decoded)
        return bad(two, start + 60_000, decoded)
    }
    @Test fun threeSpacedFailuresRemoveButOneOrTwoDoNot() {
        val one = bad(null, start); val two = bad(one, start + 30_000); val three = bad(two, start + 60_000)
        assertEquals(0L, one.removedAt); assertEquals(0L, two.removedAt)
        assertEquals(start + 60_000, three.removedAt); assertEquals(3, three.failures)
    }
    @Test fun burstErrorsFromOneAttemptCannotDeleteAChannel() {
        var h: HealthRecord? = null
        for (i in 0..10) h = bad(h, start + i * 100)
        assertEquals(1, h!!.failures); assertEquals(0L, h.removedAt)
    }
    @Test fun unknownAndUntestedRoutesStayInThePlaybackList() {
        val a = "https://a.org/a"; val b = "https://b.org/b"
        assertEquals(listOf(b), HealthRanking.sort(listOf(a, b), mapOf(a to removed()), emptySet(), start))
        assertEquals(listOf(b, a), HealthRanking.sort(listOf(a, b), mapOf(a to removed()), emptySet(), start, true))
    }
    @Test fun aSuccessfulProbeResetsAnUnconfirmedFailureSequence() {
        val reset = good(bad(null, start), start + 35_000)
        assertEquals(0, reset.failures); assertEquals(1, bad(reset, start + 65_000).failures)
    }
    @Test fun recentRealVideoSuccessProtectsAgainstBadTransportProbes() {
        val played = good(null, start, true)
        assertEquals(played, bad(played, start + 60_000))
        assertEquals(1, bad(played, start + HealthPolicy.RECENT_PLAYBACK_MS + 1).failures)
    }
    @Test fun httpSuccessCannotHideMissingVideoFrames() {
        val noVideo = bad(null, start, true)
        val reachable = good(noVideo, start + 35_000)
        assertFalse(reachable.passed); assertTrue(reachable.playbackFailed); assertEquals(1, reachable.failures)
        assertEquals("没有视频首帧", reachable.reason)
    }
    @Test fun aDecoderFailureIsNotOverriddenByAnOlderSuccessfulPlayback() {
        val wasGood = good(null, start, true)
        val failsNow = bad(wasGood, start + 60_000, true)
        assertEquals(2, bad(failsNow, start + 90_000).failures)
    }
    @Test fun transportRemovedRoutesNeedTwoSpacedSuccessesToRecover() {
        val hidden = removed(); val one = good(hidden, start + 100_000)
        assertTrue(one.removedAt > 0)
        val rapid = good(one, start + 100_100); assertTrue(rapid.removedAt > 0)
        val two = good(rapid, start + 140_000); assertEquals(0L, two.removedAt); assertTrue(two.passed)
    }
    @Test fun transportRecoveryDoesNotRestoreDecoderIncompatibleRoutes() {
        val hidden = removed(true)
        val probed = good(good(hidden, start + 100_000), start + 150_000)
        assertTrue(probed.removedAt > 0)
        assertEquals(0L, good(probed, start + 200_000, true).removedAt)
    }
    @Test fun aManualBlackScreenReportStaysRemovedUntilExplicitlyRestored() {
        val manual = HealthPolicy.manuallyRemove(null, start)
        assertTrue(good(good(manual, start + 50_000), start + 100_000, true).manualRemoval)
        assertTrue(good(manual, start + 50_000, true).removedAt > 0)
    }
    @Test fun legacyUnspacedErrorsCannotRemoveRoutesDuringUpgrade() {
        val legacy = HealthCodec.read(JSONObject("{\"passed\":false,\"at\":1,\"latency\":9,\"failures\":20}"))
        val firstNewCheck = bad(legacy, start)
        assertEquals(1, firstNewCheck.failures); assertEquals(0L, firstNewCheck.removedAt)
    }
    @Test fun removalMetadataSurvivesSavingAndRestarting() {
        val h = removed(true)
        assertEquals(h, HealthCodec.read(JSONObject(HealthCodec.write(h).toString())))
    }
    @Test fun aBackwardClockStartsANewSequenceInsteadOfDeleting() {
        val one = bad(null, start)
        val reset = bad(one, start - 100)
        assertEquals(1, reset.failures); assertEquals(0L, reset.removedAt)
    }
    @Test fun wholeNetworkOutagesCannotCountAsDeadStreams() {
        var controls = 0
        assertFalse(NetworkEvidence.permitsFailures(false, true) { controls++; true })
        assertEquals(0, controls)
        assertFalse(NetworkEvidence.permitsFailures(true, false) { false })
        assertTrue(NetworkEvidence.permitsFailures(true, false) { true })
        assertTrue(NetworkEvidence.permitsFailures(true, true) { throw AssertionError("unnecessary control request") })
    }
    @Test fun audioPlayingWithoutVideoFramesIsNotSuccessfulPlayback() {
        val p = PlaybackAttempt(); val id = p.start("a")
        assertFalse(p.success(id)); assertTrue(p.frame(id)); assertTrue(p.success(id)); assertFalse(p.success(id))
    }
    @Test fun oldFramesAndErrorsCannotAffectTheNewChannel() {
        val p = PlaybackAttempt(); val old = p.start("old"); val next = p.start("new")
        assertFalse(p.frame(old)); assertFalse(p.fail(old)); assertFalse(p.hasFirstFrame)
        assertTrue(p.frame(next)); assertTrue(p.success(next))
    }
    @Test fun duplicateFailureCallbacksAreCountedOnlyOnce() {
        val p = PlaybackAttempt(); val id = p.start("a")
        assertTrue(p.fail(id)); assertFalse(p.fail(id)); assertFalse(p.frame(id)); assertFalse(p.success(id))
    }
    @Test fun aLaterStallCanBeReportedAfterInitiallySuccessfulVideo() {
        val p = PlaybackAttempt(); val id = p.start("a")
        p.frame(id); assertTrue(p.success(id)); assertTrue(p.fail(id))
        p.clear(); assertFalse(p.fail(id)); assertFalse(p.frame(id))
    }
    @Test fun scanOnlyRechecksFailedRoutesAndStopsAfterThreeRounds() {
        val p = ScanPlan.create(listOf("a", "b", "c"), start)
        p.accept(listOf("a" to true, "b" to false, "c" to false)); assertTrue(p.finishRound())
        assertEquals(listOf("b", "c"), p.pending())
        p.accept(listOf("b" to true, "c" to false)); assertTrue(p.finishRound())
        assertEquals(listOf("c"), p.pending()); p.accept(listOf("c" to false))
        assertFalse(p.finishRound()); assertTrue(p.done); assertEquals(3, p.round)
    }
    @Test fun scanCheckpointsResumeWithoutRetestingCommittedBatches() {
        val p = ScanPlan.create(listOf("a", "b", "c"), start); p.accept(listOf("a" to false))
        val restored = ScanPlan(JSONObject(p.state.toString()))
        assertEquals(listOf("b", "c"), restored.pending()); assertEquals(1, restored.cursor)
        restored.accept(listOf("b" to true, "c" to true)); restored.finishRound()
        assertEquals(listOf("a"), restored.pending())
    }
    @Test fun scanRejectsOutOfOrderOrDuplicatedBatchCommits() {
        val p = ScanPlan.create(listOf("a", "b"), start)
        assertTrue(runCatching { p.accept(listOf("b" to false)) }.isFailure)
        p.accept(listOf("a" to false))
        assertTrue(runCatching { p.accept(listOf("a" to false)) }.isFailure)
    }
    @Test fun emptyCataloguesFinishAndScanSnapshotsAreBounded() {
        val empty = ScanPlan.create(emptyList(), start); assertFalse(empty.finishRound()); assertTrue(empty.done)
        assertEquals(2000, ScanPlan.create((0..2100).map(Int::toString), start).queue.size)
        assertEquals(listOf("a"), ScanPlan.create(listOf("a", "a"), start).queue)
    }
}
