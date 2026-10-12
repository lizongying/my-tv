package com.lizongying.mytv
import com.lizongying.mytv.agent.*
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
class DiscoveryFairnessTest {
    @Test fun equalPriorityWorkRotatesDomains() {
        val s=JSONObject();val now=1_800_000_000_000L
        DiscoveryPolicy.add(s,listOf("https://a.example/1.txt","https://a.example/2.txt","https://b.example/3.txt"),now)
        assertEquals("https://a.example/1.txt",DiscoveryPolicy.next(s));DiscoveryPolicy.done(s,"https://a.example/1.txt",now)
        assertEquals("https://b.example/3.txt",DiscoveryPolicy.next(s))
    }
    @Test fun domainRotationNeverDelaysDirectPlaylistBehindGenericPages() {
        val s=JSONObject().put("lastHost","a.example");DiscoveryPolicy.add(s,listOf("https://b.example/page","https://a.example/live.txt"),1_800_000_000_000L)
        assertEquals("https://a.example/live.txt",DiscoveryPolicy.next(s))
    }
    @Test fun largePageFrontierDoesNotDiscardNewPlaylistFiles() {
        val s=JSONObject();val now=1_800_000_000_000L
        DiscoveryPolicy.add(s,(0..127).map { "https://site.example/page/$it" },now)
        assertTrue(StationCrawler.enqueue(s,CrawlLink("https://repo.example/new.txt","节目单","playlist"),"https://repo.example",1,"",now))
        assertEquals("https://repo.example/new.txt",DiscoveryPolicy.next(s))
    }
    @Test fun alreadyVisitedSubscriptionIsNotRepeatedDuringCooldown() {
        val s=JSONObject();val now=1_800_000_000_000L;val url="https://repo.example/new.txt"
        DiscoveryPolicy.add(s,listOf(url),now);DiscoveryPolicy.done(s,url,now)
        assertFalse(StationCrawler.enqueue(s,CrawlLink(url,"节目单","playlist"),url,1,"",now+1000))
    }
}
