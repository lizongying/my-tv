package com.lizongying.mytv

import com.lizongying.mytv.agent.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RealLogoPolicyTest {
    @Test fun cctvDecorationsMapToSameChannel() { assertEquals("CCTV1",RealLogoPolicy.key(" cctv-1 综合 高清 (1080p) ")) }
    @Test fun sportsPlusDoesNotMapToSports() {
        assertEquals("CCTV5+",RealLogoPolicy.key("CCTV-5 Plus HD"))
        assertNotEquals(RealLogoPolicy.key("CCTV5"),RealLogoPolicy.key("CCTV5+体育赛事"))
    }
    @Test fun fourKDoesNotMapToInternational() { assertEquals("CCTV4K",RealLogoPolicy.key("CCTV4K"));assertNotEquals(RealLogoPolicy.key("CCTV4"),RealLogoPolicy.key("CCTV4K")) }
    @Test fun internationalRegionsAreDistinct() {
        assertEquals("CCTV4欧洲",RealLogoPolicy.key("CCTV-4欧洲高清"))
        assertNotEquals(RealLogoPolicy.key("CCTV4"),RealLogoPolicy.key("CCTV4美洲"))
    }
    @Test fun localChannelsDoNotUseFuzzyPrefixMatch() { assertNotEquals(RealLogoPolicy.key("北京卫视"),RealLogoPolicy.key("北京新闻")) }
    @Test fun hdSuffixIsRemoved() { assertEquals("北京卫视",RealLogoPolicy.key("北京卫视HD")) }
    @Test fun validNonAsciiImageUrlIsEncoded() { assertTrue(RealLogoPolicy.safeUrl("https://example.org/北京.png")!!.contains("%E5")) }
    @Test fun executableAndPrivateLogoUrlsAreRejected() {
        for(url in listOf("javascript:alert(1)","file:///etc/a.png","http://127.0.0.1/a.png","http://192.168.1.1/a.png","https://user:secret@example.org/logo.png")) assertNull(RealLogoPolicy.safeUrl(url))
    }
    @Test fun exactCatalogueKeepsOriginalAsFallback() {
        val i=JSONObject().put("北京卫视",JSONObject().put("urls",JSONArray(listOf("https://example.org/a.png","https://example.org/a.png"))))
        assertEquals(listOf("https://example.org/a.png","https://example.org/b.png"),RealLogoPolicy.candidates("北京卫视高清","https://example.org/b.png",i))
        assertEquals(listOf("https://example.org/b.png"),RealLogoPolicy.candidates("北京新闻","https://example.org/b.png",i))
    }
    @Test fun missingFileIsFetchedEvenWithSavedMetadata() { assertTrue(RealLogoPolicy.ready(JSONObject().put("saved",1000),false,2000)) }
    @Test fun cachedLogoRefreshesAfterSevenDays() {
        val e=JSONObject().put("saved",1000)
        assertFalse(RealLogoPolicy.ready(e,true,1000+RealLogoPolicy.REFRESH_MS-1))
        assertTrue(RealLogoPolicy.ready(e,true,1000+RealLogoPolicy.REFRESH_MS))
    }
    @Test fun unsuccessfulDownloadWaits24Hours() {
        val e=JSONObject().put("attempt",1000)
        assertFalse(RealLogoPolicy.ready(e,false,1000+RealLogoPolicy.RETRY_MS-1))
        assertTrue(RealLogoPolicy.ready(e,false,1000+RealLogoPolicy.RETRY_MS))
    }
    @Test fun failedUrlMemoryExpiresAndKeepsUrlsIndependent() {
        val f=JSONObject().put(MemoryPolicy.id("https://example.org/a.png"),1000)
        assertFalse(RealLogoPolicy.usable("https://example.org/a.png",f,2000))
        assertTrue(RealLogoPolicy.usable("https://example.org/b.png",f,2000))
        assertTrue(RealLogoPolicy.usable("https://example.org/a.png",f,1000+RealLogoPolicy.RETRY_MS))
    }
    @Test fun cacheLimitChoicesAreBounded() { assertEquals(2,RealLogoPolicy.limitMb(2));assertEquals(10,RealLogoPolicy.limitMb(10));assertEquals(5,RealLogoPolicy.limitMb(1000)) }
    @Test fun memoryHasByteAndEntryBounds() {
        val e=JSONObject();val f=JSONObject()
        for(n in 0..600) { e.put("key$n",JSONObject().put("at",n).put("title","字".repeat(250)));f.put("bad$n",n) }
        val bounded=RealLogoPolicy.bound(JSONObject().put("entries",e).put("failed",f))
        assertTrue(bounded.toString().toByteArray().size<=128*1024)
        assertTrue(e.length()<=500 && f.length()<=500);assertTrue(e.has("key600"));assertFalse(e.has("key0"))
    }
    @Test fun savedPlaylistRetainsOriginalLogo() {
        val original=SourceTools.parse("#EXTM3U\n#EXTINF:-1 group-title=\"央视\" tvg-logo=\"https://example.org/CCTV1.png\",CCTV1\nhttps://example.org/live.m3u8\n")
        assertEquals("https://example.org/CCTV1.png",SourceTools.parse(SourceTools.encode(original)).single().logo)
    }
    @Test fun playlistDoesNotPersistPrivateLogo() {
        val original=SourceTools.parse("#EXTM3U\n#EXTINF:-1 group-title=\"央视\" tvg-logo=\"http://127.0.0.1/logo.png\",CCTV1\nhttps://example.org/live.m3u8\n")
        assertFalse(SourceTools.encode(original).contains("127.0.0.1"))
    }
    @Test fun internationalVersionsKeepTheirLogoAndRouteAfterSaving() {
        val channels=SourceTools.parse("#EXTM3U\n#EXTINF:-1 group-title=\"央视\" tvg-logo=\"https://example.org/europe.png\",CCTV-4欧洲高清\nhttps://example.org/europe.m3u8\n#EXTINF:-1 group-title=\"央视\" tvg-logo=\"https://example.org/america.png\",CCTV-4美洲\nhttps://example.org/america.m3u8\n")
        val saved=SourceTools.parse(SourceTools.encode(channels))
        assertEquals(2,saved.size)
        assertEquals("https://example.org/europe.png",saved.single { it.title=="CCTV4欧洲" }.logo)
        assertEquals(listOf("https://example.org/america.m3u8"),saved.single { it.title=="CCTV4美洲" }.urls)
    }
}
