package com.lizongying.mytv

import com.lizongying.mytv.agent.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class MemoryPolicyTest {
    private val url="https://example.com/live?id=1"
    private val settings=MemorySettings()
    private fun book(kind: String="tv",at: Long=100): JSONObject=JSONObject().also {
        MemoryPolicy.putVision(it,VisionRoute("央视",url),VisionVerdict(kind,"简短依据"),at)
    }
    private fun reject(block: ()->Unit) { try { block(); fail("Expected rejection") } catch (_: IllegalArgumentException) {} }

    @Test fun secureDefaultsAreSmallAndBounded() {
        assertEquals(1048576,settings.bytes); assertEquals(7,settings.days)
        reject { MemorySettings(100,7) }; reject { MemorySettings(1,36500) }
    }
    @Test fun fullQueryKeepsDifferentChannelsSeparate() {
        assertNotEquals(MemoryPolicy.id(url),MemoryPolicy.id("https://example.com/live?id=2"))
        assertNotEquals(MemoryPolicy.id(url),MemoryPolicy.id("https://example.com/live?id=1&token=other"))
    }
    @Test fun hostCaseDefaultPortAndFragmentDoNotEvadeAdMemory() {
        assertEquals(MemoryPolicy.id(url),MemoryPolicy.id("HTTPS://EXAMPLE.COM:443/live?id=1#other"))
    }
    @Test fun pathCaseRemainsSignificant() {
        assertNotEquals(MemoryPolicy.id(url),MemoryPolicy.id("https://example.com/LIVE?id=1"))
    }
    @Test fun cacheDoesNotRefreshItsOwnAge() {
        val b=book(); val before=b.toString()
        assertNotNull(MemoryPolicy.cached(b,url,200,settings))
        assertEquals(before,b.toString())
        assertNull(MemoryPolicy.cached(b,url,100+7*MemoryPolicy.DAY,settings))
    }
    @Test fun normalTvExpiresAtChosenDay() {
        assertNotNull(MemoryPolicy.cached(book(),url,100+MemoryPolicy.DAY-1,MemorySettings(1,1)))
        assertNull(MemoryPolicy.cached(book(),url,100+MemoryPolicy.DAY,MemorySettings(1,1)))
    }
    @Test fun uncertainPicturesHaveShortCooldown() {
        assertNotNull(MemoryPolicy.cached(book("uncertain"),url,100+6*3_600_000L-1,settings))
        assertNull(MemoryPolicy.cached(book("uncertain"),url,100+6*3_600_000L,settings))
    }
    @Test fun captureFailureIsRetriedAfterSixHours() {
        assertNull(MemoryPolicy.cached(book("unavailable"),url,100+6*3_600_000L,settings))
    }
    @Test fun suspectedAdsRequireRecheckWithinOneDay() {
        assertNotNull(MemoryPolicy.cached(book("suspect"),url,100+MemoryPolicy.DAY-1,MemorySettings(5,90)))
        assertNull(MemoryPolicy.cached(book("suspect"),url,100+MemoryPolicy.DAY,MemorySettings(5,90)))
    }
    @Test fun clockRollbackDoesNotMakeOrdinaryMemoryImmortal() {
        assertNull(MemoryPolicy.cached(book(at=500),url,100,settings))
    }
    @Test fun unknownDecisionsAreNeverReused() {
        val b=JSONObject().put("v:"+MemoryPolicy.id(url),JSONObject().put("url",url).put("kind","delete").put("at",100))
        assertNull(MemoryPolicy.cached(b,url,101,settings))
    }
    @Test fun cacheRoundTripSurvivesRestart() {
        val loaded=JSONObject(book().toString())
        assertEquals("央视",MemoryPolicy.cached(loaded,url,200,settings)!!.getString("title"))
    }
    @Test fun confirmedAdSurvivesOrdinaryMemoryEvictionAndRestart() {
        val b=book("suspect"); val ads=JSONObject()
        MemoryPolicy.resolve(b,ads,url,"removed",100,200)
        MemoryPolicy.trim(b,90*MemoryPolicy.DAY,settings,2)
        assertTrue(JSONObject(ads.toString()).has(MemoryPolicy.id(url)))
        assertEquals(0,b.length())
    }
    @Test fun suspectAloneNeverBecomesPermanentBan() {
        val b=book("suspect"); val ads=JSONObject()
        MemoryPolicy.trim(b,2*MemoryPolicy.DAY,settings)
        assertEquals(0,ads.length())
    }
    @Test fun keepingSuspectCreatesTemporaryReviewMemory() {
        val b=book("suspect"); val ads=JSONObject()
        MemoryPolicy.resolve(b,ads,url,"kept",100,200)
        assertEquals("kept",MemoryPolicy.cached(b,url,201,settings)!!.getString("kind"))
        assertFalse(ads.has(MemoryPolicy.id(url)))
    }
    @Test fun staleConfirmationCannotOverrideNewerResult() {
        val b=book("suspect",300); val ads=JSONObject()
        reject { MemoryPolicy.resolve(b,ads,url,"removed",100,400) }
        assertEquals(0,ads.length())
    }
    @Test fun confirmedNormalTvCannotBeRemovedThroughStaleDialog() {
        reject { MemoryPolicy.resolve(book("tv"),JSONObject(),url,"removed",100,200) }
    }
    @Test fun repeatPinIsIdempotent() {
        val ads=JSONObject(); MemoryPolicy.pin(ads,url,"央视",100)
        val before=ads.toString(); MemoryPolicy.pin(ads,url,"other",1000)
        assertEquals(before,ads.toString())
    }
    @Test fun fullAdListRefusesNewEntryWithoutForgettingExisting() {
        val ads=JSONObject(); MemoryPolicy.pin(ads,url,"央视",100,maxCount=1)
        reject { MemoryPolicy.pin(ads,"https://other.example/live","其他",200,maxCount=1) }
        assertEquals(1,ads.length()); assertTrue(ads.has(MemoryPolicy.id(url)))
    }
    @Test fun adByteLimitRollsBackAttemptedPin() {
        val ads=JSONObject(); MemoryPolicy.pin(ads,url,"央视",100)
        val before=ads.toString()
        reject { MemoryPolicy.pin(ads,"https://other.example/live","其他",200,maxBytes=MemoryPolicy.bytes(ads)) }
        assertEquals(before,ads.toString())
    }
    @Test fun adListHidesQueryTokensAndFullUrls() {
        val ads=JSONObject(); MemoryPolicy.pin(ads,url+"&secret=private-value","央视",100)
        assertFalse(ads.toString().contains("private-value")); assertFalse(ads.toString().contains("?id="))
    }
    @Test fun capacityCountsUtf8AndEvictsOldestFirst() {
        val b=book(at=100)
        val second=VisionRoute("地方".repeat(100),"https://example.com/2")
        MemoryPolicy.putVision(b,second,VisionVerdict("tv","证据".repeat(100)),200)
        val newestBytes=MemoryPolicy.bytes(JSONObject().put("v:"+MemoryPolicy.id(second.url),b.getJSONObject("v:"+MemoryPolicy.id(second.url))))
        MemoryPolicy.trim(b,300,settings,newestBytes)
        assertTrue(MemoryPolicy.bytes(b)<=newestBytes)
        assertNull(MemoryPolicy.cached(b,url,300,settings))
        assertNotNull(MemoryPolicy.cached(b,second.url,300,settings))
    }
    @Test fun escapedCharactersAreIncludedInCapacity() {
        val b=book()
        MemoryPolicy.putVision(b,VisionRoute("\"\\\n".repeat(40),"https://example.com/2"),VisionVerdict("tv","x"),200)
        MemoryPolicy.trim(b,300,settings,2)
        assertEquals("{}",b.toString())
    }
    @Test fun loweringRetentionImmediatelyPrunesOldResults() {
        val b=book(at=100)
        MemoryPolicy.trim(b,100+2*MemoryPolicy.DAY,MemorySettings(1,1))
        assertEquals(0,b.length())
    }
    @Test fun failedSourceBackoffIncreasesAndCapsAtSevenDays() {
        val b=JSONObject(); var now=100L
        listOf(1,6,24,72,168,168).forEach { hours ->
            MemoryPolicy.failedSource(b,url,now)
            assertTrue(MemoryPolicy.cooling(b,url,now+hours*3_600_000L-1,MemorySettings(1,90)))
            assertFalse(MemoryPolicy.cooling(b,url,now+hours*3_600_000L,MemorySettings(1,90)))
            now+=hours*3_600_000L
        }
    }
    @Test fun cooldownDoesNotAffectOtherChannelOnSameServer() {
        val b=JSONObject(); MemoryPolicy.failedSource(b,url,100)
        assertFalse(MemoryPolicy.cooling(b,"https://example.com/live?id=2",200,settings))
    }
    @Test fun sourceSuccessResetsBackoff() {
        val b=JSONObject(); repeat(4) { MemoryPolicy.failedSource(b,url,100+it.toLong()) }
        b.remove("s:"+MemoryPolicy.id(url))
        MemoryPolicy.failedSource(b,url,1000)
        assertFalse(MemoryPolicy.cooling(b,url,1000+3_600_000L,settings))
    }
    @Test fun shortRetentionAlsoBoundsFailureMemory() {
        val b=JSONObject(); repeat(5) { MemoryPolicy.failedSource(b,url,100+it.toLong()) }
        MemoryPolicy.trim(b,104+MemoryPolicy.DAY,MemorySettings(1,1))
        assertFalse(MemoryPolicy.cooling(b,url,104+MemoryPolicy.DAY,settings))
    }
    @Test fun legacyConfirmedAdsMigrateAndNormalBlackScreenDoesNot() {
        val ads=JSONObject(); val b=JSONObject()
        val health=mapOf(url to HealthPolicy.manuallyRemove(null,100,"用户确认 AI 疑似无台标纯广告"),
            "https://example.com/black" to HealthPolicy.manuallyRemove(null,100))
        MemoryPolicy.importLegacy(ads,b,health,emptyList())
        assertEquals(1,ads.length()); assertTrue(ads.has(MemoryPolicy.id(url)))
    }
    @Test fun legacyVisionConfirmationMigratesEvenWithoutHealthRecord() {
        val ads=JSONObject(); val b=JSONObject()
        MemoryPolicy.importLegacy(ads,b,emptyMap(),listOf(url to JSONObject().put("kind","removed").put("title","央视").put("at",100)))
        assertTrue(ads.has(MemoryPolicy.id(url))); assertEquals(0,b.length())
    }
    @Test fun legacySuspectRemainsReviewableWithoutPermanentBan() {
        val ads=JSONObject(); val b=JSONObject()
        MemoryPolicy.importLegacy(ads,b,emptyMap(),listOf(url to JSONObject().put("kind","suspect").put("title","央视").put("at",100)))
        assertEquals(0,ads.length()); assertNotNull(MemoryPolicy.cached(b,url,200,settings))
    }
    @Test fun unblockingIsExplicitAndDoesNotRemoveOtherAds() {
        val ads=JSONObject(); MemoryPolicy.pin(ads,url,"央视",100); MemoryPolicy.pin(ads,"https://other.example/2","地方",100)
        ads.remove(MemoryPolicy.id(url)); assertEquals(1,ads.length())
    }
    @Test fun scanProgressHasSeparateBoundedStorage() {
        val routes=(0..2000).map { VisionRoute("央视".repeat(100),"https://example.com/"+it+"?token="+"a".repeat(2000)) }
        val bounded=MemoryPolicy.boundedRoutes(routes)
        assertTrue(bounded.isNotEmpty()); assertTrue(bounded.size<2000)
        assertTrue(MemoryPolicy.bytes(VisionPlan(bounded).json())<=MemoryPolicy.PLAN_BYTES)
        assertEquals(routes[0].url,bounded[0].url)
    }
    @Test fun pinDoesNotRetainImagesOrConversations() {
        val ads=JSONObject(); MemoryPolicy.pin(ads,url,"央视",100)
        val v=ads.getJSONObject(MemoryPolicy.id(url))
        assertEquals(setOf("title","display","at"),v.keys().asSequence().toSet())
    }
}
