package com.lizongying.mytv

import com.lizongying.mytv.agent.AiClient
import com.lizongying.mytv.agent.HealthRanking
import com.lizongying.mytv.agent.HealthRecord
import com.lizongying.mytv.agent.NetworkPolicy
import com.lizongying.mytv.agent.SourceTools
import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress

class AgentCoreTest {
    @Test fun onlySamePrivateSubnetCanPair() {
        assertTrue(NetworkPolicy.sameLan("192.168.12.8", 24, "192.168.12.90"))
        assertFalse(NetworkPolicy.sameLan("192.168.12.8", 24, "192.168.13.90"))
        assertFalse(NetworkPolicy.sameLan("192.168.12.8", 24, "8.8.8.8"))
        assertFalse(NetworkPolicy.sameLan("192.168.12.8", 24, "127.0.0.1"))
        assertFalse(NetworkPolicy.sameLan("192.168.12.8", 0, "192.168.12.90"))
        assertFalse(NetworkPolicy.sameLan("192.168.12.8", 24, "::ffff:192.168.12.90"))
    }
    @Test fun rejectsAmbiguousPrivateAddresses() {
        for (value in listOf("192.168.01.2", "192.168.1.256", "192.168.1", "2130706433", "0.0.0.0", "169.254.1.2", "172.32.0.1", "8.8.8.8"))
            assertNull(value, NetworkPolicy.privateIpv4(value))
    }
    @Test fun publicFetchCannotReachLoopbackOrPrivateNetwork() {
        for (value in listOf("http://127.0.0.1/test", "http://10.1.2.3/", "http://2130706433/", "http://[::1]/", "http://169.254.169.254/", "http://localhost/", "http://foo.local/"))
            assertTrue(value, runCatching { NetworkPolicy.publicUrl(value, false) }.isFailure)
        assertTrue(NetworkPolicy.publicAddress(InetAddress.getByName("8.8.8.8")))
        assertFalse(NetworkPolicy.publicAddress(InetAddress.getByName("100.64.0.1")))
    }
    @Test fun onlyHttpAddressesWithoutEmbeddedCredentialsAreAccepted() {
        for (value in listOf("file:///data/private", "javascript:alert(1)", "https://user:pass@example.org/a", "https://example.org/a#token"))
            assertTrue(runCatching { NetworkPolicy.httpUrl(value) }.isFailure)
        assertEquals("example.org", NetworkPolicy.httpUrl("https://example.org/list.m3u?a=1").host)
    }
    @Test fun tokensMustMatchInFull() {
        assertTrue(NetworkPolicy.tokenMatches("a".repeat(64), "a".repeat(64)))
        assertFalse(NetworkPolicy.tokenMatches("a".repeat(64), "a".repeat(63)))
        assertFalse(NetworkPolicy.tokenMatches("a".repeat(64), "b" + "a".repeat(63)))
    }
    @Test fun logsStripCredentialsAndQuerySecrets() {
        assertEquals("https://example.org/list.m3u", NetworkPolicy.displayUrl("https://user:secret@example.org/list.m3u?key=secret#private"))
    }
    @Test fun txtPlaylistAcceptsCctvAndLocalChannelsAndDropsPromotions() {
        val channels = SourceTools.parse("央视频道,#genre#\n央视1,http://example.org/1.m3u8\nCCTV5+,https://example.org/5.m3u8\n杭州综合,https://example.org/hz.m3u8\n支持作者,https://example.org/ad.m3u8")
        assertEquals(listOf("CCTV1", "CCTV5+", "杭州综合"), channels.map { it.title })
        assertEquals("地方台", channels.last().group)
    }
    @Test fun m3uWithoutMetadataCanUseChineseChannelNames() {
        val channels = SourceTools.parse("#EXTM3U\n#EXTINF:-1,浙江卫视\nhttps://example.org/zj.m3u8")
        assertEquals("卫视", channels.single().group)
    }
    @Test fun hlsSegmentsAndHtmlAreNotTreatedAsChannels() {
        assertTrue(SourceTools.parse("#EXTM3U\n#EXT-X-TARGETDURATION:6\n#EXTINF:6,\nsegment.ts").isEmpty())
        assertTrue(SourceTools.parse("<html>CCTV1,https://example.org/live.m3u8</html>").isEmpty())
    }
    @Test fun webpagesResolveSubscriptionLinksWithoutExecutingScripts() {
        val links = SourceTools.pageLinks("https://example.org/live/", """<a href="../list.m3u">列表</a><a href="javascript:evil()">脚本</a><a href="http://10.0.0.1/a.txt">内网</a><script>let x="https:\/\/other.org\/tv.txt";</script>""")
        assertTrue(links.contains("https://example.org/list.m3u"))
        assertTrue(links.contains("https://other.org/tv.txt"))
        assertFalse(links.any { it.contains("10.0.0.1") || it.startsWith("javascript") })
    }
    @Test fun cachedPlaylistRoundTripsWithoutChangingChannelIdentity() {
        val input = SourceTools.parse("CCTV1,https://example.org/1.m3u8\n浙江卫视,https://example.org/zj.m3u8\n杭州新闻,https://example.org/hz.m3u8")
        assertEquals(input, SourceTools.parse(SourceTools.encode(input)))
    }
    @Test fun publicStationPagesCanIdentifyDirectHlsAndGenericListsCannot() {
        assertEquals("CCTV1", SourceTools.directTitle(SourceTools.pageTitle("<title>CCTV-1综合频道直播_央视网</title>")))
        assertEquals("浙江卫视", SourceTools.directTitle("浙江卫视直播"))
        assertNull(SourceTools.directTitle("全国电视直播频道列表"))
    }
    @Test fun healthRankingPrefersRecentSuccessAndNeverDiscardsUnknownSources() {
        val now = 1_000_000L
        val input = listOf("https://a.org/live", "https://b.org/live", "https://c.org/live", "https://d.org/live")
        val health = mapOf(input[0] to HealthRecord(false, now, 100, 3), input[1] to HealthRecord(true, now, 80), input[3] to HealthRecord(true, now, 40))
        assertEquals(listOf(input[3], input[1], input[2], input[0]), HealthRanking.sort(input, health, emptySet(), now))
        assertEquals(listOf(input[1], input[2], input[0]), HealthRanking.sort(input, health, setOf("d.org"), now))
    }
    @Test fun staleResultsAreRetriedAndExplicitBlocksAreRespected() {
        val now = 100_000_000L
        val urls = listOf("https://a.org/a", "https://b.org/b")
        assertEquals(urls, HealthRanking.sort(urls, mapOf(urls[0] to HealthRecord(false, 0, 1, 20)), emptySet(), now))
        assertTrue(HealthRanking.sort(urls, emptyMap(), urls.toSet(), now).isEmpty())
    }
    @Test fun aiCannotInventUrlsOrSelectOutsideFetchedSearchResults() {
        assertEquals(listOf(1, 0), AiClient.chooseIds("```json\n{\"ids\":[1,-1,99,1,0]}\n```", 3))
        assertTrue(AiClient.chooseIds("{\"ids\":[]}", 3).isEmpty())
        assertTrue(runCatching { AiClient.chooseIds("{\"urls\":[\"https://invented.org\"]}", 3) }.isFailure)
    }
    @Test fun additionalUrlsCanBeRankedBeforeApplyingPlaybackLimit() {
        val first = PlaylistChannel("CCTV1", "央视", "", (1..8).map { "https://example.org/" + it })
        val extra = first.copy(urls = listOf("https://good.org/live"))
        assertEquals(9, PlaylistParser.merge(listOf(listOf(first), listOf(extra)), 64).single().urls.size)
    }
    @Test fun emptyThinkingResponsesAreNotReportedAsSuccessfulConnections() {
        assertTrue(runCatching { AiClient.answerContent("{\"choices\":[{\"message\":{\"content\":\"\"}}]}") }.isFailure)
        assertTrue(runCatching { AiClient.answerContent("{\"choices\":[{\"message\":{\"content\":null}}]}") }.isFailure)
        assertEquals("OK", AiClient.answerContent("{\"choices\":[{\"message\":{\"content\":\"OK\"}}]}"))
    }
}
