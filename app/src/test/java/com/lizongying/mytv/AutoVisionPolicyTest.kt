package com.lizongying.mytv

import com.lizongying.mytv.agent.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AutoVisionPolicyTest {
    private val now=10*MemoryPolicy.DAY
    private val route=VisionRoute("测试台","https://example.com/live?id=1")
    private val suspect=VisionVerdict("suspect","三帧均明确广告")
    private fun observe(book: JSONObject,ads: JSONObject,time: Long=now,run: String="a",block: Boolean=true,verdict: VisionVerdict=suspect): Boolean =
        AutoVisionPolicy.observe(book,ads,route,verdict,time,run,block)
    private fun review(book: JSONObject)=book.optJSONObject("r:"+MemoryPolicy.id(route.url))
    @Test fun firstFreshResponseNeverBlocks() {
        val book=JSONObject(); val ads=JSONObject()
        assertFalse(observe(book,ads)); assertNotNull(review(book)); assertEquals(0,ads.length())
    }
    @Test fun twoIndependentSpacedResponsesBlockAndForgetOrdinaryVerdict() {
        val book=JSONObject(); val ads=JSONObject()
        MemoryPolicy.putVision(book,route,suspect,now)
        observe(book,ads)
        assertTrue(observe(book,ads,now+AutoVisionPolicy.RECHECK_MS,"b"))
        assertTrue(ads.has(MemoryPolicy.id(route.url))); assertNull(review(book))
        assertFalse(book.has("v:"+MemoryPolicy.id(route.url)))
        assertTrue(ads.getJSONObject(MemoryPolicy.id(route.url)).getString("reason").contains("30"))
    }
    @Test fun responsesTooCloseNeverBlock() {
        val book=JSONObject(); val ads=JSONObject(); observe(book,ads)
        assertFalse(observe(book,ads,now+AutoVisionPolicy.RECHECK_MS-1,"b")); assertEquals(0,ads.length())
        assertEquals(now,review(book)!!.getLong("at"))
    }
    @Test fun sameRunCannotBeSecondVote() {
        val book=JSONObject(); val ads=JSONObject(); observe(book,ads)
        assertFalse(observe(book,ads,now+AutoVisionPolicy.RECHECK_MS,"a"))
        assertEquals(0,ads.length())
    }
    @Test fun cachedSuspectWaitsAndThenNeedsNewModelResponse() {
        val book=JSONObject(); val ads=JSONObject(); observe(book,ads)
        val cached=JSONObject().put("kind","suspect")
        assertFalse(AutoVisionPolicy.needsInspection(cached,review(book),now))
        assertFalse(AutoVisionPolicy.needsInspection(cached,review(book),now+AutoVisionPolicy.RECHECK_MS-1))
        assertTrue(AutoVisionPolicy.needsInspection(cached,review(book),now+AutoVisionPolicy.RECHECK_MS))
        assertEquals(0,ads.length())
    }
    @Test fun cachedNormalAndKeptResultsDoNotNeedAnotherPaidCall() {
        for(kind in listOf("tv","kept","unavailable","uncertain")) {
            assertFalse(AutoVisionPolicy.needsInspection(JSONObject().put("kind",kind),null,now))
        }
        assertTrue(AutoVisionPolicy.needsInspection(null,null,now))
    }
    @Test fun suspectWithoutReviewNeedsFreshFirstVote() {
        assertTrue(AutoVisionPolicy.needsInspection(JSONObject().put("kind","suspect"),null,now))
    }
    @Test fun clockRollbackStartsOver() {
        val book=JSONObject(); val ads=JSONObject(); observe(book,ads)
        assertFalse(observe(book,ads,now-1,"b")); assertEquals(now-1,review(book)!!.getLong("firstAt"))
        assertEquals(0,ads.length())
    }
    @Test fun expiredFirstVoteStartsOver() {
        val book=JSONObject(); val ads=JSONObject(); observe(book,ads)
        val later=now+AutoVisionPolicy.REVIEW_MS
        assertFalse(observe(book,ads,later,"b")); assertEquals(later,review(book)!!.getLong("firstAt"))
    }
    @Test fun nonAdvertisingResultsResetReview() {
        for(kind in listOf("tv","kept","uncertain","unavailable")) {
            val book=JSONObject(); val ads=JSONObject(); observe(book,ads)
            assertFalse(observe(book,ads,now+100,"b",verdict=VisionVerdict(kind,"保留")))
            assertNull(review(book))
            assertFalse(observe(book,ads,now+AutoVisionPolicy.RECHECK_MS,"c"))
        }
    }
    @Test fun detectOnlyKeepsEvidenceButDoesNotBlock() {
        val book=JSONObject(); val ads=JSONObject(); observe(book,ads,block=false)
        val later=now+AutoVisionPolicy.RECHECK_MS
        assertFalse(observe(book,ads,later,"b",false)); assertEquals(0,ads.length())
        assertEquals(later,review(book)!!.getLong("at"))
        assertFalse(observe(book,ads,later+1,"c",true))
    }
    @Test fun distinctQueryChannelsDoNotShareVotes() {
        val book=JSONObject(); val ads=JSONObject(); observe(book,ads)
        val other=route.copy(url="https://example.com/live?id=2")
        assertFalse(AutoVisionPolicy.observe(book,ads,other,suspect,now+AutoVisionPolicy.RECHECK_MS,"b",true))
        assertEquals(2,book.length()); assertEquals(0,ads.length())
    }
    @Test fun evictedReviewRequiresTwoFreshResponsesAgain() {
        val book=JSONObject(); val ads=JSONObject(); observe(book,ads)
        MemoryPolicy.trim(book,now,MemorySettings(),2)
        assertEquals(0,book.length())
        assertFalse(observe(book,ads,now+AutoVisionPolicy.RECHECK_MS,"b"))
    }
    @Test fun selectedOneDayRetentionAlsoLimitsReviewMemory() {
        val book=JSONObject(); val ads=JSONObject(); observe(book,ads)
        MemoryPolicy.trim(book,now+MemoryPolicy.DAY,MemorySettings(1,1))
        assertNull(review(book)); assertEquals(0,ads.length())
    }
    @Test fun reviewRetentionNeverExceedsSevenDays() {
        assertEquals(7*MemoryPolicy.DAY,MemoryPolicy.ttl("auto-review",MemorySettings(5,90)))
    }
    @Test fun fullPinMemoryDoesNotSilentlyEvictExistingPins() {
        val ads=JSONObject()
        MemoryPolicy.pin(ads,route.url,route.title,now,maxCount=1)
        try { MemoryPolicy.pin(ads,"https://example.com/2","other",now,maxCount=1); fail() }
        catch(_: IllegalArgumentException) { }
        assertEquals(1,ads.length()); assertTrue(ads.has(MemoryPolicy.id(route.url)))
    }
    @Test fun explanatoryPinBytesRespectUtf8Limit() {
        val ads=JSONObject()
        try { MemoryPolicy.pin(ads,route.url,route.title,now,maxBytes=150,reason="汉".repeat(240)); fail() }
        catch(_: IllegalArgumentException) { }
        assertEquals(0,ads.length())
    }
    @Test fun autoPlanHasOwnSmallerBound() {
        val routes=(1..4000).map { VisionRoute("台".repeat(120),"https://example.com/"+it) }
        val auto=MemoryPolicy.boundedRoutes(routes,192*1024)
        assertTrue(auto.isNotEmpty()); assertTrue(auto.size<MemoryPolicy.boundedRoutes(routes).size)
    }
    @Test fun glPixelsFlipRowsAndPreserveChannels() {
        val bytes=byteArrayOf(0,0,-1,-1, -1,-1,-1,-1, -1,0,0,-1, 0,-1,0,-1)
        assertArrayEquals(intArrayOf(0xffff0000.toInt(),0xff00ff00.toInt(),0xff0000ff.toInt(),-1),FramePixels.argb(bytes,2,2))
    }
    @Test fun glPixelsRejectWrongBufferAndOversize() {
        for(pair in listOf(0 to 1,961 to 1,1 to 961,2 to 2)) {
            try { FramePixels.argb(ByteArray(4),pair.first,pair.second); fail() }
            catch(_: IllegalArgumentException) { }
        }
    }
}
