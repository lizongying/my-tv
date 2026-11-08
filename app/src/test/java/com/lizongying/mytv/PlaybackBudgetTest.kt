package com.lizongying.mytv
import com.lizongying.mytv.agent.PlaybackBudget
import org.junit.Assert.*
import org.junit.Test
class PlaybackBudgetTest {
    @Test fun startsWithOneBoundedWindow() { val b=PlaybackBudget();b.select("CCTV1",1000);assertEquals(45000L,b.remaining(1000));assertEquals(0L,b.remaining(47000)) }
    @Test fun catalogueRefreshDoesNotResetDeadline() { val b=PlaybackBudget();b.select("CCTV1",1000);b.select("CCTV1",20000);assertEquals(26000L,b.remaining(20000)) }
    @Test fun exhaustedChannelStaysStoppedAcrossRefresh() { val b=PlaybackBudget();b.select("CCTV1",1000);b.finish();b.select("CCTV1",20000);assertTrue(b.exhausted);assertEquals(0L,b.remaining(20000)) }
    @Test fun explicitClickRestartsAnExhaustedChannel() { val b=PlaybackBudget();b.select("CCTV1",1000);b.finish();b.select("CCTV1",20000,true);assertFalse(b.exhausted);assertEquals(45000L,b.remaining(20000)) }
    @Test fun selectingAnotherChannelGetsItsOwnWindow() { val b=PlaybackBudget();b.select("CCTV1",1000);b.finish();b.select("CCTV6",20000);assertEquals(45000L,b.remaining(20000)) }
    @Test fun aLongPlayingStreamCanStartAFreshRecoveryWindow() { val b=PlaybackBudget();b.select("CCTV1",1000);b.recover(90000);assertEquals(45000L,b.remaining(90000)) }
    @Test fun retriesShareTheSameTotalWindow() { val b=PlaybackBudget();b.select("CCTV1",1000);for(time in listOf(16000L,31000L,46000L)) b.select("CCTV1",time);assertEquals(0L,b.remaining(46000)) }
    @Test fun invalidWindowIsRejected() { assertTrue(runCatching { PlaybackBudget(0) }.isFailure) }
}
