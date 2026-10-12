package com.lizongying.mytv
import com.lizongying.mytv.agent.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*

class RepositoryCrawlerTest {
    private val sample="https://jihulab.com/owen2000wy/owentv/-/raw/main/HP20230319.txt"
    private val api=RepositoryCrawler.catalogue(sample)!!
    @Test fun suppliedRawAddressDiscoversItsWholePublicRepository() {
        assertEquals("https://jihulab.com/api/v4/projects/owen2000wy%2Fowentv/repository/tree?ref=main&per_page=100&page=1",api)
    }
    @Test fun gitHubRepositoryAndRawFilesBecomeCatalogues() {
        assertEquals("https://api.github.com/repos/YueChan/Live/git/trees/HEAD?recursive=1",RepositoryCrawler.catalogue("https://github.com/YueChan/Live"))
        assertEquals("https://api.github.com/repos/YueChan/Live/git/trees/main?recursive=1",RepositoryCrawler.catalogue("https://raw.githubusercontent.com/YueChan/Live/main/GNTV.m3u"))
    }
    @Test fun nestedGitLabProjectAndChinesePathsAreEncoded() {
        val tree=RepositoryCrawler.catalogue("https://gitlab.com/group/sub/repo/-/raw/main/列表.txt")!!
        assertTrue(tree.contains("group%2Fsub%2Frepo"))
        val links=RepositoryCrawler.links(tree,"[{\"type\":\"blob\",\"path\":\"直播/列表.txt\"}]")
        assertEquals("https://gitlab.com/group/sub/repo/-/raw/main/%E7%9B%B4%E6%92%AD/%E5%88%97%E8%A1%A8.txt",links.single().url)
    }
    @Test fun unsupportedHostsAndCredentialUrlsDoNotBecomeApis() {
        assertNull(RepositoryCrawler.catalogue("https://evil.example/owner/repo"));assertNull(RepositoryCrawler.catalogue("https://name:pass@jihulab.com/owner/repo"))
    }
    @Test fun apiNeverLoopsBackToItselfAsANewRepository() { assertNull(RepositoryCrawler.catalogue(api)) }
    @Test fun catalogSeparatesDirectoriesFromPlaylistFiles() {
        val links=RepositoryCrawler.links(api,"[{\"type\":\"tree\",\"path\":\"TV\"},{\"type\":\"blob\",\"path\":\"a.txt\"},{\"type\":\"blob\",\"path\":\"b.M3U\"},{\"type\":\"blob\",\"path\":\"README.md\"}]")
        assertEquals(listOf("playlist","playlist","catalog"),links.map { it.kind });assertTrue(links.last().url.endsWith("&path=TV"))
    }
    @Test fun unsafeRepositoryPathsAreIgnored() {
        assertTrue(RepositoryCrawler.links(api,"[{\"type\":\"blob\",\"path\":\"../secret.txt\"},{\"type\":\"blob\",\"path\":\"/a.txt\"}]").isEmpty())
    }
    @Test fun githubRecursiveTreeFindsNestedPlaylistsWithoutFetchingEveryFolder() {
        val tree=RepositoryCrawler.catalogue("https://github.com/o/r")!!
        assertEquals("https://raw.githubusercontent.com/o/r/HEAD/list/a.m3u",RepositoryCrawler.links(tree,"{\"tree\":[{\"type\":\"blob\",\"path\":\"list/a.m3u\"}]}").single().url)
    }
    @Test fun largeCatalogueContinuesWithoutLosingRemainingItems() {
        val body=JSONArray((0..49).map { JSONObject().put("type","blob").put("path","tv$it.txt") }).toString()
        val first=RepositoryCrawler.links(api,body);assertEquals(25,first.size)
        val next=first.last().url;assertTrue(RepositoryCrawler.continuation(api,next));assertEquals(api,RepositoryCrawler.fetchAddress(next))
        val second=RepositoryCrawler.links(next,body);val third=RepositoryCrawler.links(second.last().url,body)
        val all=(first+second+third).filter { it.kind=="playlist" };assertEquals(50,all.map { it.url }.distinct().size)
    }
    @Test fun fullGitLabPagesCanDiscoverNextPage() {
        val body=JSONArray((0..99).map { JSONObject().put("type","blob").put("path","read$it.md") }).toString()
        assertTrue(RepositoryCrawler.links(api,body).single().url.contains("page=2"))
    }
    @Test fun malformedApiResponseIsHarmless() { assertTrue(RepositoryCrawler.links(api,"<html>login</html>").isEmpty()) }
    @Test fun repoFileDiscoveryNeedsNeitherApiKeyNorModelOutput() {
        assertTrue(RepositoryCrawler.entries.any { it.url==sample });assertEquals(1,RepositoryCrawler.links(api,"[{\"type\":\"blob\",\"path\":\"HP20230319.txt\"}]").size)
    }
}
