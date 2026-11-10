package com.lizongying.mytv
import com.lizongying.mytv.agent.FrameProgress
import org.junit.Assert.*
import org.junit.Test
class FrameProgressTest {
 @Test fun noWatchdogBeforeFirstFrame() { assertFalse(FrameProgress().stalled(0,999999)) }
 @Test fun frozenVideoTimesOutDespiteRepeatedPlayerReadyEvents() { val p=FrameProgress();p.start(2,100);assertFalse(p.stalled(2,15099));assertTrue(p.stalled(2,15100)) }
 @Test fun realNewFramesRenewTheDeadline() { val p=FrameProgress();p.start(1,0);assertFalse(p.stalled(200,14000));assertFalse(p.stalled(201,28000));assertTrue(p.stalled(201,43000)) }
 @Test fun decoderCounterResetAloneDoesNotRenewDeadline() { val p=FrameProgress();p.start(100,0);assertFalse(p.stalled(0,10000));assertTrue(p.stalled(0,15000)) }
 @Test fun clearedAndRestartedAttemptsHaveIndependentDeadlines() { val p=FrameProgress();p.start(1,0);p.clear();assertFalse(p.stalled(1,16000));p.start(1,20000);assertFalse(p.stalled(1,21000));assertTrue(p.stalled(1,35000)) }
 @Test fun monotonicClockRollbackDoesNotTriggerImmediateFailure() { val p=FrameProgress();p.start(1,1000);assertFalse(p.stalled(1,500));assertTrue(p.stalled(1,15500)) }
}
