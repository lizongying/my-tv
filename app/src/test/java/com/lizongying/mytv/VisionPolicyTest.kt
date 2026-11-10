package com.lizongying.mytv

import com.lizongying.mytv.agent.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class VisionPolicyTest {
    private fun answer(logo: String = "absent", ad: Boolean = true, confidence: Double = 0.99): JSONObject =
        JSONObject().put("frames", JSONArray((0..2).map {
            JSONObject().put("id",it).put("logo",logo).put("advertising",ad).put("confidence",confidence)
        })).put("reason","购物电话与促销，未见台标")
    private fun fail(block: () -> Unit) { try { block(); org.junit.Assert.fail("Expected rejection") } catch (_: IllegalArgumentException) {} }
    @Test fun clearAdsBecomeCandidatesOnly() { assertEquals("suspect",VisionPolicy.parse(answer().toString()).kind) }
    @Test fun stationCommercialsArePreserved() { assertEquals("tv",VisionPolicy.parse(answer("present").toString()).kind) }
    @Test fun logoInOnlyOneFrameProtectsRoute() {
        val o=answer(); o.getJSONArray("frames").getJSONObject(1).put("logo","present")
        assertEquals("tv",VisionPolicy.parse(o.toString()).kind)
    }
    @Test fun noLogoWithoutAdsIsNotAdvertising() { assertEquals("uncertain",VisionPolicy.parse(answer(ad=false).toString()).kind) }
    @Test fun hiddenOrUnreadableLogoIsUncertain() { assertEquals("uncertain",VisionPolicy.parse(answer("uncertain").toString()).kind) }
    @Test fun mixedProgramsArePreserved() {
        val o=answer();o.getJSONArray("frames").getJSONObject(2).put("advertising",false)
        assertEquals("uncertain",VisionPolicy.parse(o.toString()).kind)
    }
    @Test fun weakConfidenceInOneFramePreventsCandidate() {
        val o=answer();o.getJSONArray("frames").getJSONObject(0).put("confidence",0.92)
        assertEquals("uncertain",VisionPolicy.parse(o.toString()).kind)
    }
    @Test fun singleFrameCannotClassifyWholeRoute() {
        val o=answer();o.getJSONArray("frames").remove(2)
        fail { VisionPolicy.parse(o.toString()) }
    }
    @Test fun duplicateFrameIdsAreRejected() {
        val o=answer();o.getJSONArray("frames").getJSONObject(2).put("id",1)
        fail { VisionPolicy.parse(o.toString()) }
    }
    @Test fun inventedIdsAreRejected() {
        val o=answer();o.getJSONArray("frames").getJSONObject(2).put("id",20)
        fail { VisionPolicy.parse(o.toString()) }
    }
    @Test fun fractionalIdsAreRejected() {
        val o=answer();o.getJSONArray("frames").getJSONObject(2).put("id",1.5)
        fail { VisionPolicy.parse(o.toString()) }
    }
    @Test fun booleanStringsAreRejected() {
        val o=answer();o.getJSONArray("frames").getJSONObject(0).put("advertising","true")
        fail { VisionPolicy.parse(o.toString()) }
    }
    @Test fun invalidConfidenceIsRejected() { fail { VisionPolicy.parse(answer(confidence=1.01).toString()) } }
    @Test fun promptInjectionCannotDirectlySetDecision() {
        val o=answer(ad=false).put("decision","delete").put("url","https://example.com/delete")
        assertEquals("uncertain",VisionPolicy.parse(o.toString()).kind)
    }
    @Test fun responseReasonIsBounded() {
        val o=answer().put("reason","a\n".repeat(500))
        assertEquals(180,VisionPolicy.parse(o.toString()).reason.length)
    }
    @Test fun visionRequestContainsActualImageParts() {
        val images=(0..2).map { "data:image/jpeg;base64,AAA$it" }
        val o=VisionPolicy.request("deepseek-flash","频道",images)
        val content=o.getJSONArray("messages").getJSONObject(1).getJSONArray("content")
        assertEquals(4,content.length())
        (0..2).forEach {
            assertEquals("image_url",content.getJSONObject(it+1).getString("type"))
            assertEquals(images[it],content.getJSONObject(it+1).getJSONObject("image_url").getString("url"))
        }
        assertFalse(o.toString().contains("Authorization"))
    }
    @Test fun visionRequestRejectsMissingOrRemoteScreenshots() {
        fail { VisionPolicy.request("deepseek-flash","频道",listOf("https://example.com/a.jpg")) }
    }
    @Test fun emptyBlackOrSolidCapturesCannotReachModel() {
        assertFalse(VisionFrameQuality.usable(intArrayOf()))
        assertFalse(VisionFrameQuality.usable(IntArray(500){0xff000000.toInt()}))
        assertFalse(VisionFrameQuality.usable(IntArray(500){0xff777777.toInt()}))
        assertTrue(VisionFrameQuality.usable(IntArray(500){if (it%2==0) 0xffffffff.toInt() else 0xff000000.toInt()}))
    }
    @Test fun quotaStopsPaidCallsAtLimitAndResetsNextDay() {
        val q=VisionQuota(10,9).reserve(10*86_400_000L,10)
        assertEquals(10,q.calls)
        fail { q.reserve(10*86_400_000L,10) }
        assertEquals(1,q.reserve(11*86_400_000L,10).calls)
    }
    @Test fun quotaCannotBeRaisedArbitrarily() { fail { VisionQuota(-1,0).reserve(10,10000) } }
    @Test fun planSurvivesRestartWithoutRepeatingCompletedRoute() {
        val a=VisionRoute("央视","https://example.com/a")
        val b=VisionRoute("地方","https://example.com/b")
        val plan=VisionPlan(VisionPolicy.queue(listOf(a,a,b))).advance(a.url)
        val resumed=VisionPlan.read(plan.json())
        assertEquals(b,resumed.next())
        assertTrue(resumed.advance(b.url).done)
        fail { resumed.advance(a.url) }
    }
    @Test fun everyChannelComesBeforeBackupRoutesAtCap() {
        val groups = (0..1000).map { channel -> (0..2).map { index -> VisionRoute("频道$channel","https://example.com/$channel/$index") } }
        val routes = VisionPolicy.queue(VisionPolicy.interleave(groups))
        assertEquals(2000,routes.size)
        assertEquals(groups.map { it[0] },routes.take(groups.size))
        assertEquals(groups[0][1],routes[groups.size])
        assertTrue(VisionPolicy.interleave(emptyList()).isEmpty())
    }
    @Test fun fullCatalogIsBounded() {
        val routes=(0..2500).map { VisionRoute("频道$it","https://example.com/$it") }
        assertEquals(2000,VisionPolicy.queue(routes).size)
    }
    @Test fun invalidCursorIsRejected() {
        val o=VisionPlan(emptyList()).json().put("cursor",1)
        fail { VisionPlan.read(o) }
    }
    @Test fun confirmedAdvertisingUsesRecoverableManualRemoval() {
        val h=HealthPolicy.manuallyRemove(null,100,"用户确认 AI 疑似无台标纯广告")
        assertTrue(h.manualRemoval); assertEquals(100L,h.removedAt)
        assertEquals(h.removedAt,HealthPolicy.observe(h,true,true,1000,0,"").removedAt)
        assertTrue(h.reason.contains("纯广告"))
    }
}
