package com.lizongying.mytv

import com.lizongying.mytv.agent.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LogoPolicyTest {
    private fun answer(name: String, palette: String = "teal", motif: String = "wave") = JSONObject().put("icons", JSONArray().put(
        JSONObject().put("id",LogoPolicy.id(name)).put("palette",palette).put("motif",motif))).toString()
    @Test fun validModelRecipeRetainsChannelIdentity() {
        assertEquals(mapOf(LogoPolicy.id("CCTV-1") to LogoDesign("teal","wave")),LogoPolicy.parse(answer("CCTV-1"),listOf("CCTV-1")))
    }
    @Test fun unknownChannelIdIsIgnored() { assertTrue(LogoPolicy.parse(answer("购物"),listOf("CCTV-1")).isEmpty()) }
    @Test fun modelCannotInjectSvgOrColourCode() { assertTrue(LogoPolicy.parse(answer("CCTV-1","<svg onload=x>","wave"),listOf("CCTV-1")).isEmpty()) }
    @Test fun modelCannotChooseAnExecutableMotif() { assertTrue(LogoPolicy.parse(answer("CCTV-1","teal","https://evil.example/x"),listOf("CCTV-1")).isEmpty()) }
    @Test fun duplicateResponsesDoNotOverwriteFirstDesign() {
        val o=JSONObject(answer("CCTV-1")); o.getJSONArray("icons").put(JSONObject().put("id",LogoPolicy.id("CCTV-1")).put("palette","red").put("motif","star"))
        assertEquals(LogoDesign("teal","wave"),LogoPolicy.parse(o.toString(),listOf("CCTV-1"))[LogoPolicy.id("CCTV-1")])
    }
    @Test fun fencedJsonIsAccepted() { assertEquals(1,LogoPolicy.parse("```json\n"+answer("央视新闻")+"\n```",listOf("央视新闻")).size) }
    @Test fun emptyModelResultsAreHarmless() { assertTrue(LogoPolicy.parse("{\"icons\":[]}",listOf("央视新闻")).isEmpty()) }
    @Test(expected=Exception::class) fun oversizedModelResultIsRejected() { LogoPolicy.parse("x".repeat(12001),listOf("央视新闻")) }
    @Test(expected=Exception::class) fun extraBatchEntriesAreRejected() {
        LogoPolicy.parse(JSONObject().put("icons",JSONArray((0..8).map { JSONObject() })).toString(),listOf("央视新闻"))
    }
    @Test fun channelIdentityIgnoresCaseAndSurroundingSpaces() { assertEquals(LogoPolicy.id("cctv-1"),LogoPolicy.id("  CCTV-1  ")) }
    @Test fun deterministicFallbackWorksWithoutModel() {
        assertEquals(LogoPolicy.fallback("北京卫视"),LogoPolicy.fallback("北京卫视"))
        assertTrue(LogoPolicy.fallback("北京卫视").palette in LogoPolicy.palettes)
    }
    @Test fun failedDesignWaits24HoursAndSuccessfulDesignIsNotRepeated() {
        val o=JSONObject().put("attempt",1000L)
        assertFalse(LogoPolicy.eligible(o,1000+LogoPolicy.RETRY_MS-1))
        assertTrue(LogoPolicy.eligible(o,1000+LogoPolicy.RETRY_MS))
        o.put("palette","blue").put("motif","ring")
        assertFalse(LogoPolicy.eligible(o,1000+LogoPolicy.RETRY_MS*10))
    }
    @Test fun queueIsOldestFirstAndBatchBounded() {
        val state=JSONObject()
        for(i in 10 downTo 0) state.put(LogoPolicy.id("频道$i"),JSONObject().put("title","频道$i").put("at",i))
        assertEquals((0..7).map { "频道$it" },LogoPolicy.pending(state,1000))
    }
    @Test fun cacheHonoursSmallestByteLimitAndEntryCap() {
        val state=JSONObject()
        for(i in 0..700) state.put(LogoPolicy.id("频道$i"),JSONObject().put("title","频道$i").put("at",i).put("extra","字".repeat(128)))
        LogoPolicy.bound(state,64)
        assertTrue(state.toString().toByteArray(Charsets.UTF_8).size<=64*1024)
        assertTrue(state.length()<=500);assertTrue(state.has(LogoPolicy.id("频道700")));assertFalse(state.has(LogoPolicy.id("频道0")))
    }
    @Test fun promptCarriesOnlyNamesAndBoundedIdsAndStripsControlCharacters() {
        val names=(0..20).map { "频道$it\n" }
        val input=JSONArray(LogoPolicy.prompt(names).substringAfter("频道："))
        assertEquals(8,input.length());assertEquals(setOf("id","name"),input.getJSONObject(0).keySet())
        assertFalse(input.getJSONObject(0).getString("name").contains("\n"))
    }
}
