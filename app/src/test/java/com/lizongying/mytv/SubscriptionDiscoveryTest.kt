package com.lizongying.mytv

import com.lizongying.mytv.agent.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SubscriptionDiscoveryTest {
    private val ini="https://raw.githubusercontent.com/testtff/TV/master/config/config.ini"
    private val list="https://raw.githubusercontent.com/Guovin/iptv-api/master/config/subscribe.txt"
    @Test fun oldIniSelectsOnlyEnabledSubscriptionValues() {
        val text="""[Settings]
            # subscribe_urls = https://ignored.example/list.txt
            open_subscribe = True
            subscribe_urls = https://a.example/live.txt,https://b.example/list.m3u
            source_file = https://ignored.example/template.txt
            api_key = https://ignored.example/key.txt
            [Other]
            subscribe_urls = https://ignored.example/other.txt
        """.trimIndent()
        assertEquals(listOf("https://a.example/live.txt","https://b.example/list.m3u"),SubscriptionDiscovery.links(ini,text).map { it.url })
    }
    @Test fun disabledInputIsRespectedEvenAfterItsUrls() {
        assertTrue(SubscriptionDiscovery.links(ini,"[Settings]\nsubscribe_urls=https://a.example/live.txt\nopen_subscribe=False").isEmpty())
    }
    @Test fun continuedIniValuesAreCollectedWithoutOtherSettings() {
        val text="[Settings]\nsubscribe_urls=https://a.example/a.txt,\n  https://b.example/b.txt\nopen_proxy=True\nhttps://ignored.example/x.txt"
        assertEquals(listOf("https://a.example/a.txt","https://b.example/b.txt"),SubscriptionDiscovery.links(ini,text).map { it.url })
    }
    @Test fun plainListKeepsWhitelistUrlsInTheOrdinaryVerificationPipeline() {
        val links=SubscriptionDiscovery.links(list,"# https://ignored.example/x\nhttps://a.example/a.txt\n[WHITELIST]\nhttps://b.example/b.m3u UA=\"Player\"")
        assertEquals(2,links.size);assertTrue(links.all { it.kind=="playlist" });assertTrue(links.none { it.url.contains("UA") })
    }
    @Test fun duplicateAndSelfReferencesAreRemoved() {
        assertEquals(listOf("https://a.example/a.txt"),SubscriptionDiscovery.links(list,"$list\nhttps://a.example/a.txt\nhttps://a.example/a.txt").map { it.url })
    }
    @Test fun gitHubMirrorsAreUnwrappedToTheirPublicRawOrigin() {
        val expected="https://raw.githubusercontent.com/o/r/main/list.txt"
        assertEquals(expected,SubscriptionDiscovery.address("https://ghproxy.net/$expected"))
        assertEquals(expected,SubscriptionDiscovery.address("https://github.moeyy.xyz/https://github.com/o/r/blob/main/list.txt"))
    }
    @Test fun unrelatedProxyUrlsArePreserved() {
        val url="https://unknown.example/https://raw.githubusercontent.com/o/r/main/a.txt"
        assertEquals(url,SubscriptionDiscovery.address(url))
    }
    @Test fun privateCredentialAndNonHttpUrlsCannotEnterTheFrontier() {
        val text="http://127.0.0.1/a\nhttp://192.168.1.2/a\nhttps://user:pass@example.com/a\nhttp://localhost/a\nfile:///a\nrtp://239.1.1.1/a"
        assertTrue(SubscriptionDiscovery.links(list,text).isEmpty())
    }
    @Test fun urlQueryCommasAndChannelParametersArePreserved() {
        val urls=SubscriptionDiscovery.links(ini,"[Settings]\nsubscribe_urls=https://a.example/get?ids=1,2,https://a.example/get?ids=3").map { it.url }
        assertEquals(listOf("https://a.example/get?ids=1,2","https://a.example/get?ids=3"),urls)
    }
    @Test fun shellCommandsAndUnrecognizedHeadersAreNeverExecutedOrForwarded() {
        assertNull(SubscriptionDiscovery.address("curl https://a.example/a.txt"))
        assertNull(SubscriptionDiscovery.address("https://a.example/a.txt Authorization=secret"))
        assertTrue(SubscriptionDiscovery.links(ini,"[Settings]\ncommand=curl https://a.example/a.txt").isEmpty())
    }
    @Test fun arbitraryNamedUrlIndexesCanBeFollowed() {
        val url="https://a.example/feeds"
        assertTrue(SubscriptionDiscovery.handles(url,"# List\nhttps://b.example/channels.m3u"))
        assertEquals(1,SubscriptionDiscovery.links(url,"https://b.example/channels.m3u").size)
    }
    @Test fun mediaAndChannelPlaylistsAreNotMistakenForIndexes() {
        for(text in listOf("#EXTM3U\n#EXTINF:-1,CCTV1\nhttps://a.example/a", "#EXTM3U\n#EXT-X-TARGETDURATION:6\n#EXTINF:6,\nhttps://a.example/a.ts", "CCTV1,https://a.example/a"))
            assertFalse(SubscriptionDiscovery.handles("https://a.example/list.m3u",text))
    }
    @Test fun htmlLoginPagesDoNotContributeQuotedExampleAddresses() {
        assertTrue(SubscriptionDiscovery.links(list,"<html>https://ignored.example/ads.m3u</html>").isEmpty())
    }
    @Test fun emptyCurrentUpstreamIndexDoesNotInventExampleFeeds() {
        assertTrue(SubscriptionDiscovery.handles(list,"# Supports https://xxx.com/list.m3u\n[WHITELIST]"))
        assertTrue(SubscriptionDiscovery.links(list,"# Supports https://xxx.com/list.m3u\n[WHITELIST]").isEmpty())
    }
    @Test fun boundedInputAndOutputCannotFloodTheQueue() {
        assertEquals(SubscriptionDiscovery.MAX_LINKS,SubscriptionDiscovery.links(list,(0..100).joinToString("\n") { "https://a.example/$it.txt" }).size)
        assertTrue(SubscriptionDiscovery.links(list,"x".repeat(512*1024+1)).isEmpty())
    }
    @Test fun channelNamesLogosAndAlternativesSurviveIndexDiscovery() {
        val feed=SubscriptionDiscovery.links(list,"https://a.example/live.txt").single()
        val channels=SourceTools.parse("#EXTM3U\n#EXTINF:-1 tvg-logo=\"https://a.example/logo.png\" group-title=\"央视频道\",CCTV-1\nhttps://a.example/play?id=1\n#EXTINF:-1 group-title=\"央视频道\",CCTV-1\nhttps://a.example/play?id=2")
        assertEquals("https://a.example/live.txt",feed.url)
        val queue=JSONObject();CandidatePolicy.add(queue,channels,1)
        assertEquals(2,queue.length());assertEquals("https://a.example/logo.png",channels.single().logo)
        assertFalse(queue.toString().contains("passed"));assertEquals(2,CandidatePolicy.due(queue,2,"").size)
    }
    @Test fun aDiscoveredSubscriptionIsNeverAcceptedAsPlayableMedia() = runBlocking {
        val body="#EXTM3U\n#EXTINF:-1,CCTV1\nhttps://a.example/play"
        val url=SubscriptionDiscovery.links(list,"https://a.example/live.m3u").single().url
        val result=LiveStreamVerifier.check(url,HttpFetch.Reply(url,body.toByteArray(),"application/x-mpegURL"),{},fetch={error("Must not fetch a catalogue as media")},wait={})
        assertFalse(result.passed)
    }
    @Test fun defaultSeedsAreSubscriptionInputsRatherThanStaleGeneratedResults() {
        assertEquals(2,SubscriptionDiscovery.entries.size)
        assertTrue(SubscriptionDiscovery.entries.all { SubscriptionDiscovery.isConfiguration(it.url) && it.kind=="subscription" })
        assertTrue(SubscriptionDiscovery.entries.none { it.url.contains("output/result") })
    }
}
