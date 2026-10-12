package com.lizongying.mytv

import com.lizongying.mytv.agent.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DiscoveryTest {
    private val now=100_000_000L
    private fun result(vararg urls: String): JSONObject = JSONObject().put("content",JSONArray().put(
        JSONObject().put("type","web_search_tool_result").put("content",JSONArray(urls.map {
            JSONObject().put("type","web_search_result").put("url",it).put("title","公开节目单")
        }))))
    @Test fun existingOfficialApiConfigurationAutomaticallyUsesNativeSearch() {
        for(path in listOf("","/","/v1","/chat/completions","/v1/chat/completions"))
            assertEquals("https://api.deepseek.com/anthropic/v1/messages",NativeSearchProtocol.endpoint("https://api.deepseek.com"+path))
    }
    @Test fun unofficialHostsNeverReceiveKeysThroughTheOfficialSearchEndpoint() {
        for(url in listOf("https://api.deepseek.com.evil.org","https://proxy.org/v1","http://api.deepseek.com","https://api.deepseek.com:8443","https://api.deepseek.com/private","https://api.deepseek.com?key=test"))
            assertNull(url,NativeSearchProtocol.endpoint(url))
    }
    @Test fun searchDefinitionUsesServerSearchAndBoundedUses() {
        val body=NativeSearchProtocol.body("my-model","地方台",emptyList())
        assertEquals("my-model",body.getString("model"))
        val tool=body.getJSONArray("tools").getJSONObject(0)
        assertEquals("web_search_20250305",tool.getString("type"))
        assertEquals(2,tool.getInt("max_uses"))
    }
    @Test fun knownUrlTokensAreExcludedFromSearchPrompts() {
        val body=NativeSearchProtocol.body("my-model","地方台",listOf("https://example.org/tv?secret=private-value"))
        assertFalse(body.toString().contains("private-value"))
        assertTrue(body.toString().contains("https://example.org/tv"))
    }
    @Test fun realServerResultsAreReadAndFragmentDuplicatesCollapsed() {
        assertEquals(listOf("https://example.org/list","https://other.org/tv"),
            NativeSearchProtocol.hits(result("https://example.org/list#1","https://example.org/list#2","https://other.org/tv")).map { it.url })
    }
    @Test fun generatedTextAndClientToolRequestsCannotInventSearchResults() {
        val response=JSONObject().put("content",JSONArray().put(JSONObject().put("type","text").put("text","https://invented.org/list.m3u"))
            .put(JSONObject().put("type","tool_use").put("name","web_search").put("input",JSONObject().put("url","https://invented.org"))))
        assertTrue(NativeSearchProtocol.hits(response).isEmpty())
    }
    @Test fun searchErrorsCannotBecomeChannels() {
        val error=JSONObject().put("content",JSONArray().put(JSONObject().put("type","web_search_tool_result")
            .put("content",JSONObject().put("type","web_search_tool_result_error").put("error_code","unavailable"))))
        assertTrue(NativeSearchProtocol.hits(error).isEmpty())
    }
    @Test fun privateAndNonHttpSearchResultsAreRejected() {
        assertTrue(NativeSearchProtocol.hits(result("http://127.0.0.1/a","http://10.1.2.3/a","file:///data/a","https://user:pass@example.org/a")).isEmpty())
    }
    @Test fun excessiveSearchResultsAreBounded() {
        assertEquals(30,NativeSearchProtocol.hits(result(*(1..100).map { "https://example.org/$it" }.toTypedArray())).size)
    }
    @Test fun urlCanonicalizationPreservesEncodedSignaturesAndChannelParameters() {
        assertEquals("https://example.org/live?a=x%26y%3Dz&id=2",DiscoveryPolicy.canonical("https://EXAMPLE.org/live?a=x%26y%3Dz&id=2#1"))
        assertNotEquals(DiscoveryPolicy.canonical("https://example.org/live?id=1"),DiscoveryPolicy.canonical("https://example.org/live?id=2"))
    }
    @Test fun mirroredPlaylistsHaveSameContentFingerprintDespiteOrderAndDuplicates() {
        val a=PlaylistChannel("CCTV1","央视","logo-a",listOf("https://example.org/1"))
        val b=PlaylistChannel("地方台","地方台","",listOf("https://example.org/2"))
        assertEquals(DiscoveryPolicy.contentId(listOf(a,b)),DiscoveryPolicy.contentId(listOf(b,a,a.copy(logo="logo-b"))))
    }
    @Test fun differentChannelsOrSignedRoutesRemainDistinct() {
        val a=PlaylistChannel("CCTV1","央视","",listOf("https://example.org/live?id=1"))
        assertNotEquals(DiscoveryPolicy.contentId(listOf(a)),DiscoveryPolicy.contentId(listOf(a.copy(urls=listOf("https://example.org/live?id=2")))))
        assertNotEquals(DiscoveryPolicy.contentId(listOf(a)),DiscoveryPolicy.contentId(listOf(a.copy(title="CCTV2"))))
    }
    @Test fun frontierIsDurableAcrossJsonRoundTripAndDoesNotLoseChildren() {
        val state=JSONObject(); DiscoveryPolicy.add(state,listOf("https://example.org/root"),now)
        DiscoveryPolicy.add(state,listOf("https://example.org/child.m3u"),now)
        DiscoveryPolicy.done(state,"https://example.org/root",now)
        val restored=JSONObject(state.toString())
        assertEquals("https://example.org/child.m3u",DiscoveryPolicy.next(restored))
        assertEquals(0,DiscoveryPolicy.add(restored,listOf("https://example.org/root"),now+1000))
    }
    @Test fun visitedPagesAreSkippedUntilRefreshAndExplicitRefreshCanRecheck() {
        val state=JSONObject(); val url="https://example.org/tv"
        DiscoveryPolicy.done(state,url,now)
        assertEquals(0,DiscoveryPolicy.add(state,listOf(url),now+3_600_000))
        assertEquals(1,DiscoveryPolicy.add(state,listOf(url),now+3_600_000,true))
    }
    @Test fun oldPagesBecomeEligibleAgain() {
        val state=JSONObject(); val url="https://example.org/tv"; DiscoveryPolicy.done(state,url,now)
        assertEquals(1,DiscoveryPolicy.add(state,listOf(url),now+7*3_600_000))
    }
    @Test fun duplicateQueueEntriesAndFragmentsDoNotWasteFetches() {
        val state=JSONObject()
        assertEquals(1,DiscoveryPolicy.add(state,listOf("https://example.org/tv#1","https://example.org/tv#2","https://example.org/tv"),now))
        assertEquals(1,state.getJSONArray("queue").length())
    }
    @Test fun frontierQueueAndHistoryAreBounded() {
        val state=JSONObject()
        DiscoveryPolicy.add(state,(0..300).map { "https://example.org/$it" },now)
        assertEquals(DiscoveryPolicy.MAX_QUEUE,state.getJSONArray("queue").length())
        for(i in 0..500) DiscoveryPolicy.done(state,"https://example.org/$i",now+i)
        assertEquals(DiscoveryPolicy.MAX_HISTORY,state.getJSONObject("history").length())
    }
    @Test fun metadataIsPrunedWithItsPageAndUtf8CapacityIsEnforced() {
        val state=JSONObject(); val meta=JSONObject().also { state.put("meta",it) }
        val urls=(0..100).map { "https://example.org/$it" }
        DiscoveryPolicy.add(state,urls,now)
        urls.forEach { meta.put(it,JSONObject().put("title","节目".repeat(2000))) }
        meta.put("https://orphan.org/",JSONObject().put("title","not queued"))
        DiscoveryPolicy.bound(state)
        assertTrue(state.toString().toByteArray(Charsets.UTF_8).size<=DiscoveryPolicy.MAX_BYTES)
        assertFalse(meta.has("https://orphan.org/"))
        assertEquals(state.getJSONArray("queue").length(),meta.length())
    }
    @Test fun githubRepositoryAndBlobCanBeReadWithoutTheLargeHtmlPage() {
        assertEquals("https://raw.githubusercontent.com/author/live/HEAD/README.md",DiscoveryPolicy.fetchAddress("https://github.com/author/live"))
        assertEquals("https://raw.githubusercontent.com/author/live/main/tv/list.m3u",DiscoveryPolicy.fetchAddress("https://github.com/author/live/blob/main/tv/list.m3u"))
        assertEquals("https://example.org/live",DiscoveryPolicy.fetchAddress("https://example.org/live"))
    }
    @Test fun markdownRelativeListsAndRelevantExternalPagesAreFollowed() {
        val links=SourceTools.pageLinks("https://raw.githubusercontent.com/a/tv/main/README.md",
            "[节目单](./tv/list.m3u)\n<a href=\"https://station.example.org/watch\">地方台直播</a><a href=\"https://ads.example.org/watch\">买东西</a>")
        assertTrue(links.contains("https://raw.githubusercontent.com/a/tv/main/tv/list.m3u"))
        assertTrue(links.contains("https://station.example.org/watch"))
        assertFalse(links.contains("https://ads.example.org/watch"))
    }
    @Test fun actualSubscriptionFilesAreFetchedBeforeMoreArticlePages() {
        val state=JSONObject(); DiscoveryPolicy.add(state,listOf("https://example.org/article","https://example.org/live.txt"),now)
        DiscoveryPolicy.bound(state)
        assertEquals("https://example.org/live.txt",DiscoveryPolicy.next(state))
    }
    @Test fun discoveryCacheKeepsRoutesUntilHealthRankingCanChooseThem() {
        val lines=(1..20).joinToString("\n") { "CCTV1,https://example.org/live?id=$it" }
        val channels=SourceTools.parse(lines)
        assertEquals(20,channels.single().urls.size)
        assertEquals(channels,SourceTools.parse(SourceTools.encode(channels)))
    }
}
