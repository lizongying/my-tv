package com.lizongying.mytv
import com.lizongying.mytv.agent.*
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
class RecoveryRequestsTest {
 @Test fun pendingRepairDoesNotForceNetworkRefreshEveryServiceTick() { val r=RecoveryRequests();r.request("CCTV1",1000);assertTrue(r.refreshDue(1000));r.refreshed(1000);r.request("CCTV6",2000);assertFalse(r.refreshDue(2000));assertTrue(r.refreshDue(901000)) }
 @Test fun completedRepairDoesNotRequestAnotherRefresh() { val r=RecoveryRequests();r.request("CCTV1",1000);r.complete("CCTV1");assertFalse(r.refreshDue(2000)) }

 @Test fun onlyNewVerifiedUnremovedRoutesCanRecoverPlayback() { val h=mapOf("a" to HealthRecord(true,1,1),"b" to HealthRecord(true,1,1));assertEquals(1,PlaybackRoutes.replacement(listOf("a","b"),setOf("a"),h)) }
 @Test fun untestedOrRemovedRoutesCannotRestartLoading() { assertNull(PlaybackRoutes.replacement(listOf("a","b"),emptySet(),mapOf("b" to HealthRecord(true,1,1,removedAt=2)))) }

 @Test fun failedChannelIsQueued() { val r=RecoveryRequests();assertTrue(r.request("CCTV1",1000));assertEquals(setOf("CCTV1"),r.pending(1001)) }
 @Test fun repeatFailuresAreThrottled() { val r=RecoveryRequests();r.request("CCTV1",1000);assertFalse(r.request("CCTV1",2000));assertTrue(r.request("CCTV1",901000)) }
 @Test fun playbackSuccessClearsPriorityButKeepsCooldown() { val r=RecoveryRequests();r.request("CCTV1",1000);r.complete("CCTV1");assertTrue(r.pending(1001).isEmpty());assertFalse(r.request("CCTV1",2000)) }
 @Test fun queueIsBounded() { val r=RecoveryRequests();repeat(30){r.request("channel"+it,1000L+it)};assertEquals(16,r.pending(1100).size) }
 @Test fun expiredAndBlankRequestsAreDiscarded() { val r=RecoveryRequests();assertFalse(r.request(" ",0));r.request("CCTV1",1000);assertTrue(r.pending(21601001).isEmpty()) }
 private fun book()=JSONObject().put("old",JSONObject().put("title","other").put("url","https://example.org/a").put("at",1)).put("repair",JSONObject().put("title","CCTV1").put("url","https://example.org/b").put("at",2))
 @Test fun missingChannelCandidatesComeFirst() { assertEquals("repair",CandidatePolicy.due(book(),100,"",setOf("CCTV1")).first()) }
 @Test fun recoveryNeverBypassesBackoffOrCurrentPlayback() { val b=book();b.getJSONObject("repair").put("next",200);assertEquals(listOf("old"),CandidatePolicy.due(b,100,"",setOf("CCTV1")));assertEquals(listOf("old"),CandidatePolicy.due(book(),100,"https://example.org/b",setOf("CCTV1"))) }
}
