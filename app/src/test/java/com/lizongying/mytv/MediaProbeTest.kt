package com.lizongying.mytv

import com.lizongying.mytv.agent.HttpFetch
import org.junit.Assert.*
import org.junit.Test

class MediaProbeTest {
    private fun text(url: String, value: String, type: String = "application/vnd.apple.mpegurl") = HttpFetch.Reply(url, value.toByteArray(), type)
    private fun ts() = ByteArray(600).apply { this[0] = 0x47; this[188] = 0x47; this[376] = 0x47 }
    @Test fun probesTheRealSegmentInsteadOfTrustingThePlaylistOrInitMap() {
        val requested = mutableListOf<String>()
        HttpFetch.probe("https://example.org/live/index.m3u8") { url ->
            requested += url
            if (url.endsWith(".m3u8")) text(url, "#EXTM3U\n#EXT-X-TARGETDURATION:4\n#EXT-X-MAP:URI=\"init.mp4\"\n#EXTINF:4,\nsegment.ts\n")
            else HttpFetch.Reply(url, ts(), "video/mp2t")
        }
        assertEquals(listOf("https://example.org/live/index.m3u8", "https://example.org/live/segment.ts"), requested)
    }
    @Test fun followsMasterAndMediaPlaylistsToVerifyBytes() {
        var requests = 0
        HttpFetch.probe("https://example.org/master.m3u8") { url ->
            requests++
            when {
                url.contains("master") -> text(url, "#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=800000\nmedia.m3u8\n")
                url.contains("media") -> text(url, "#EXTM3U\n#EXT-X-TARGETDURATION:4\n#EXTINF:4,\nsegment.ts\n")
                else -> HttpFetch.Reply(url, ts(), "video/mp2t")
            }
        }
        assertEquals(3, requests)
    }
    @Test fun htmlAndJsonErrorPagesAreNotHealthyMedia() {
        for (body in listOf("<html>unavailable</html>".repeat(3), "{\"error\":\"stream expired or missing\"}")) {
            val error = runCatching { HttpFetch.probe("https://example.org/live") { text(it, body, "text/plain") } }.exceptionOrNull()
            assertNotNull(error); assertFalse(error is HttpFetch.UnverifiedMediaException)
        }
    }
    @Test fun emptyResponsesAreConfirmedFailures() {
        val error = runCatching { HttpFetch.probe("https://example.org/live") { text(it, "") } }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }
    @Test fun encryptedHlsIsKeptForRealPlaybackValidation() {
        val encrypted = "#EXTM3U\n#EXT-X-TARGETDURATION:4\n#EXT-X-KEY:METHOD=AES-128,URI=\"key\"\n#EXTINF:4,\nsegment.ts\n"
        val error = runCatching { HttpFetch.probe("https://example.org/live") { text(it, encrypted) } }.exceptionOrNull()
        assertTrue(error is HttpFetch.UnverifiedMediaException)
        assertFalse(HttpFetch.encryptedPlaylist("#EXT-X-KEY:METHOD=NONE\n"))
    }
    @Test fun unfamiliarMediaCannotBeDeletedJustBecauseTheProbeDoesNotRecognizeIt() {
        val error = runCatching { HttpFetch.probe("https://example.org/live") { HttpFetch.Reply(it, ByteArray(100) { 1 }, "video/unknown") } }.exceptionOrNull()
        assertTrue(error is HttpFetch.UnverifiedMediaException)
        assertFalse(HttpFetch.Check("url", false, false, 0).failed)
    }
}
