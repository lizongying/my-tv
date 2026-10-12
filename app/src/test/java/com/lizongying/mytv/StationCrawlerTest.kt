package com.lizongying.mytv

import com.lizongying.mytv.agent.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class StationCrawlerTest {
    private val base="https://station.example.org/live/cctv1/"
    private fun playlist(sequence: Int=1, end: Boolean=false, segment: String="part$sequence.ts") =
        "#EXTM3U\n#EXT-X-TARGETDURATION:2\n#EXT-X-MEDIA-SEQUENCE:$sequence\n#EXTINF:2,\n$segment\n"+(if(end) "#EXT-X-ENDLIST\n" else "")
    private fun reply(text: String,url: String=base)=HttpFetch.Reply(url,text.toByteArray(),"application/vnd.apple.mpegurl")
    private fun video(url: String)=HttpFetch.Reply(url,ByteArray(400).also { it[0]=0x47;it[188]=0x47 },"video/mp2t")
    @Test fun defaultEntriesArePublicStationWebsitesWithoutSearchProviders() {
        assertEquals(6,StationCrawler.entries.size)
        StationCrawler.entries.forEach { assertNotNull(DiscoveryPolicy.canonical(it.url)); assertFalse(it.url.contains("github")); assertFalse(it.url.contains("bing")) }
    }
    @Test fun escapedJsonUrlsAndSignedQueriesSurviveExtraction() {
        val raw="""{"hls":"https:\/\/cdn.example.org\/live.m3u8?a=1\u0026b=x%2By"}"""
        assertEquals("https://cdn.example.org/live.m3u8?a=1&b=x%2By",StationCrawler.links(base,raw).single().url)
    }
    @Test fun actualAnchorNamesAreKeptForIndividualChannels() {
        val links=StationCrawler.links(base,"<a href='/live/cctv2/'>CCTV-2 财经</a><a href='/live/cctv3/'>CCTV-3</a>")
        assertEquals(listOf("CCTV2","CCTV3"),links.map { StationCrawler.channelTitle(it.title) })
    }
    @Test fun externalStationLinksCanExpandBeyondTheInitialDomain() {
        assertEquals("https://other.example.org/watch",StationCrawler.links(base,"<a href='https://other.example.org/watch'>地方频道直播</a>").single().url)
    }
    @Test fun unrelatedAdvertisementsAndDownloadsAreNotCrawlPages() {
        assertTrue(StationCrawler.links(base,"<a href='https://ads.example.org/buy'>买东西</a><a href='/live/photo.png'>直播图</a>").isEmpty())
    }
    @Test fun scriptsIframesAndVideoSourcesAreDiscovered() {
        val links=StationCrawler.links(base,"<script src='../player.js'></script><iframe src='/embed'></iframe><video src='//cdn.example.org/live.m3u8'></video>","CCTV1")
        assertTrue(links.any { it.kind=="script" && it.url.endsWith("/live/player.js") })
        assertTrue(links.any { it.kind=="page" && it.url.endsWith("/embed") })
        assertEquals("media",links.first().kind)
    }
    @Test fun externalScriptsDoNotConsumeFollowingChannelAnchors() {
        val links=StationCrawler.links(base,"<script src='/player.js'>var x=1;</script><a href='/live/cctv2/'>CCTV2</a>","CCTV1")
        assertTrue(links.any { it.kind=="script" && it.title=="CCTV1" })
        assertTrue(links.any { it.kind=="page" && it.title=="CCTV2" })
    }
    @Test fun privateAddressesAndActiveNonHttpSchemesAreRejected() {
        for(url in listOf("http://127.0.0.1/live.m3u8","http://192.168.1.2/live.m3u8","file:///live.m3u8","javascript:alert(1)","https://a:b@example.org/live.m3u8"))
            assertTrue(url,StationCrawler.links(base,"<video src='$url'></video>").isEmpty())
    }
    @Test fun perPageExtractionIsBounded() {
        assertEquals(StationCrawler.MAX_LINKS,StationCrawler.links(base,(1..100).joinToString("") { "<a href='/live/$it'>频道$it</a>" }).size)
    }
    @Test fun aFullArticleQueueStillAcceptsANewDirectStream() {
        val state=JSONObject();val now=100_000_000L
        DiscoveryPolicy.add(state,(1..128).map { "https://example.org/page/$it" },now)
        assertTrue(StationCrawler.enqueue(state,CrawlLink("https://example.org/play?id=1","CCTV1","media"),base,1,base,now))
        DiscoveryPolicy.bound(state)
        assertEquals(128,state.getJSONArray("queue").length())
        assertEquals("https://example.org/play?id=1",DiscoveryPolicy.next(state))
    }
    @Test fun aPreviouslyCheckedStreamIsNotRequeuedByEvictingOtherPages() {
        val state=JSONObject();val now=100_000_000L;val url="https://example.org/live.m3u8"
        DiscoveryPolicy.done(state,url,now)
        assertFalse(StationCrawler.enqueue(state,CrawlLink(url,"CCTV1","media"),base,1,base,now+1000))
    }
    @Test fun extensionlessJsonStreamKeepsItsChannelName() {
        val links=StationCrawler.jsonStreams(base,"""{"data":[{"name":"山东卫视","play_url":"https://cdn.example.org/play?id=1"}]}""")
        assertEquals("山东卫视",links.single().title); assertEquals("https://cdn.example.org/play?id=1",links.single().url)
    }
    @Test fun unrelatedJsonArticleUrlsAreNotAssumedToBeStreams() {
        assertTrue(StationCrawler.jsonStreams(base,"""{"name":"CCTV1","url":"https://example.org/article"}""").isEmpty())
    }
    @Test fun jsonChannelNamesAreNotAssignedFromNeighbouringObjects() {
        val links=StationCrawler.jsonStreams(base,"""[{"name":"CCTV1","hls":"/one.m3u8"},{"name":"CCTV2","hls":"/two.m3u8"}]""")
        assertEquals(listOf("CCTV1","CCTV2"),links.map { it.title })
    }
    @Test fun multipleCctvNamesInADirectoryAreAmbiguous() { assertNull(StationCrawler.channelTitle("CCTV-1 CCTV-2 直播大全")) }
    @Test fun knownLocalChannelNamesCanIdentifyAPlayerPage() { assertEquals("山东新闻频道",StationCrawler.channelTitle("山东新闻频道高清直播",listOf("山东新闻频道"))) }
    @Test fun relativeStreamAddressesUseTheActualPageLocation() { assertEquals("https://station.example.org/live/index.m3u8",StationCrawler.resolve(base,"../index.m3u8")) }
    @Test fun playerDetectionFindsAnEmbeddedPlayerOnAnExtensionlessPage() { assertTrue(StationCrawler.isPlayerPage("https://example.org/watch","<video></video>")) }
    @Test fun vodAndClosedLivePlaylistsAreExplicitlyDetected() {
        assertTrue(HlsInspection.parse(base,playlist(end=true))!!.ended)
        assertTrue(HlsInspection.parse(base,playlist()+"#EXT-X-PLAYLIST-TYPE:VOD")!!.ended)
    }
    @Test fun masterPlaylistFindsItsRelativeVariant() {
        val p=HlsInspection.parse(base,"#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=1000\nlow/index.m3u8\n")!!
        assertEquals(base+"low/index.m3u8",p.variant);assertTrue(p.segments.isEmpty())
    }
    @Test fun mediaPlaylistIncludesOnlySegmentsAfterExtinf() {
        val p=HlsInspection.parse(base,playlist()+"#EXT-X-MAP:URI=\"init.mp4\"\n")!!
        assertEquals(listOf(base+"part1.ts"),p.segments)
    }
    @Test fun unchangedPlaylistIsNotLiveEvidence() {
        val p=HlsInspection.parse(base,playlist())!!;assertFalse(HlsInspection.progressed(p,p))
    }
    @Test fun sequenceAdvancementIsLiveEvidence() { assertTrue(HlsInspection.progressed(HlsInspection.parse(base,playlist())!!,HlsInspection.parse(base,playlist(2))!!)) }
    @Test fun endedPlaylistCannotProvideLiveEvidence() { assertFalse(HlsInspection.progressed(HlsInspection.parse(base,playlist())!!,HlsInspection.parse(base,playlist(2,true))!!)) }
    @Test fun encryptionAndByteRangesRemainUnverified() {
        assertTrue(HlsInspection.parse(base,playlist()+"#EXT-X-KEY:METHOD=AES-128,URI=\"key\"")!!.encrypted)
        assertTrue(HlsInspection.parse(base,playlist()+"#EXT-X-BYTERANGE:100@0")!!.unsupported)
    }
    @Test fun htmlAndOrdinaryChannelDirectoriesAreNotHlsMedia() {
        assertNull(HlsInspection.parse(base,"<html>error</html>"));assertNull(HlsInspection.parse(base,"#EXTM3U\n#EXTINF:-1,CCTV1\nhttps://example.org/live.m3u8"))
    }
    @Test fun directLiveValidationChecksProgressAndBothRecentSegments() = runBlocking {
        val fetched=mutableListOf<String>();var waited=0L
        val result=LiveStreamVerifier.check(base,reply(playlist()),{}, { url -> fetched.add(url);if(url==base) reply(playlist(2)) else video(url) }, { waited+=it })
        assertTrue(result.passed);assertTrue(result.hls);assertEquals(2000,waited)
        assertEquals(listOf(base+"part1.ts",base,base+"part2.ts"),fetched)
    }
    @Test fun directVodDoesNotFetchVideoOrWait() = runBlocking {
        val result=LiveStreamVerifier.check(base,reply(playlist(end=true)),{}, { error("must not fetch") }, { error("must not wait") })
        assertFalse(result.passed)
    }
    @Test fun stalledStreamDoesNotEnterTheProgrammeList() = runBlocking {
        var waits=0
        val result=LiveStreamVerifier.check(base,reply(playlist()),{}, { url -> if(url==base) reply(playlist()) else video(url) }, { waits++ })
        assertFalse(result.passed);assertEquals(2,waits)
    }
    @Test fun brokenNewSegmentPreventsAcceptanceEvenIfPlaylistAdvances() = runBlocking {
        val result=LiveStreamVerifier.check(base,reply(playlist()),{}, { url -> when(url) { base -> reply(playlist(2));base+"part1.ts" -> video(url);else -> error("missing") } }, {})
        assertFalse(result.passed)
    }
    @Test fun foregroundViewingInterruptsDirectValidationBeforeMoreFetches() = runBlocking {
        var allowed=0
        try {
            LiveStreamVerifier.check(base,reply(playlist()),{ if(++allowed>1) throw ScanDeferred("viewing") }, { video(it) }, {})
            fail("must yield")
        } catch(_: ScanDeferred) { assertEquals(2,allowed) }
    }
    @Test fun unusuallyLongSegmentsDoNotBlockABackgroundBatch() = runBlocking {
        val result=LiveStreamVerifier.check(base,reply(playlist().replace("TARGETDURATION:2","TARGETDURATION:60")),{}, { error("no fetch") }, { error("no wait") })
        assertFalse(result.passed)
    }
    @Test fun directMasterValidationFollowsVariantThenObservesItsProgress() = runBlocking {
        var reads=0
        val master=reply("#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=1000\nindex.m3u8\n")
        val result=LiveStreamVerifier.check(base,master,{}, { url -> if(url.endsWith("index.m3u8")) reply(playlist(++reads),url) else video(url) }, {})
        assertTrue(result.passed);assertEquals(2,reads)
    }
}
