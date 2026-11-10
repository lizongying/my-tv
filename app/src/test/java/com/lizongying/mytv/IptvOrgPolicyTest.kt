package com.lizongying.mytv
import com.lizongying.mytv.agent.*
import org.junit.Test
import org.junit.Assert.*
import org.json.JSONArray
class IptvOrgPolicyTest {
 private val foreign="#EXTM3U\n#EXTINF:-1 tvg-id=\"Example.us@HD\" tvg-logo=\"https://a.example/logo.png\" group-title=\"News\",Example TV\nhttps://a.example/live.m3u8"
 @Test fun ordinaryImportsRemainDomesticUnlessCountryIsExplicit() {assertTrue(PlaylistParser.parse(foreign).isEmpty());assertEquals(1,PlaylistParser.parse(foreign,internationalCountry="US").size)}
 @Test fun globalImportsKeepCountryAndOriginalLogos() {val c=PlaylistParser.parse(foreign,internationalCountry="US").single();assertEquals("US",IptvOrgPolicy.country(c.group));assertEquals("https://a.example/logo.png",c.logo);assertEquals("Example TV [US]",c.title)}
 @Test fun differentCountriesDoNotMergeSameNamedStations() {val all=PlaylistParser.merge(listOf(PlaylistParser.parse(foreign,internationalCountry="US"),PlaylistParser.parse(foreign,internationalCountry="GB")));assertEquals(2,all.size)}
 @Test fun internationalScopeSurvivesValidatedPlaylistPersistence() {val c=PlaylistParser.parse(foreign,internationalCountry="US");val next=SourceTools.parse(SourceTools.encode(c));assertEquals(c,next)}
 @Test fun chineseForeignStationAlsoSurvivesPersistence() {val c=PlaylistParser.parse(foreign.replace("Example TV","地方新闻"),internationalCountry="HK");assertEquals(c,SourceTools.parse(SourceTools.encode(c)))}
 @Test fun deselectingARegionHidesItWithoutDiscardingTheCurrentStream() {val c=PlaylistParser.parse(foreign,internationalCountry="US").single();assertFalse(IptvOrgPolicy.visible(c,setOf("CN")));assertTrue(IptvOrgPolicy.visible(c,setOf("CN"),c.urls.first()));assertTrue(IptvOrgPolicy.visible(c,setOf("US")))}
 @Test fun countryChoicesAreBoundedAndDoNotBecomeArbitraryUrls() {assertEquals(listOf("CN","US"),IptvOrgPolicy.clean(listOf("cn","US","CN"),setOf("CN","US")));assertTrue(runCatching {IptvOrgPolicy.clean(listOf("../"),setOf("CN"))}.isFailure);assertTrue(runCatching {IptvOrgPolicy.url("../x")}.isFailure)}
 @Test fun tooManyRegionsAreRejectedAndEmptySelectionCanPause() {assertTrue(runCatching {IptvOrgPolicy.clean(listOf("CN","US","GB","HK","TW","JP"),setOf("CN","US","GB","HK","TW","JP"))}.isFailure);assertTrue(IptvOrgPolicy.clean(emptyList(),setOf("CN")).isEmpty())}
 @Test fun officialCountryAddressesAreDeterministic() {assertEquals("https://iptv-org.github.io/iptv/countries/cn.m3u",IptvOrgPolicy.url("CN"));assertEquals("https://iptv-org.github.io/iptv/countries/uk.m3u",IptvOrgPolicy.url("GB"))}
 @Test fun domesticGroupingAndNamingRemainCompatible() {val c=PlaylistParser.parse("#EXTM3U\n#EXTINF:-1 tvg-id=\"CCTV1.cn\" group-title=\"General\",CCTV-1\nhttps://a.example/cctv1.m3u8").single();assertEquals("央视",c.group);assertEquals("CCTV1",c.title)}
 @Test fun shopReligionAndAdultCategoriesStayFilteredForGlobalImports() {for(group in listOf("Shop","Religious","Adult","News;XXX")) assertTrue(PlaylistParser.parse(foreign.replace("News",group),internationalCountry="US").isEmpty())}
 @Test fun privateAndNonHttpRoutesStayOutOfGlobalImports() {for(url in listOf("http://127.0.0.1/live","http://192.168.1.2/live","rtp://239.1.1.1/live","file:///data/local/live")) assertTrue(PlaylistParser.parse(foreign.replace("https://a.example/live.m3u8",url),internationalCountry="US").isEmpty())}
 @Test fun commasInTitlesAndLogoUrlsAreNotDiscarded() {val c=PlaylistParser.parse(foreign.replace("logo.png","logo,a.png").replace("Example TV","Example, News"),internationalCountry="US").single();assertEquals("Example, News [US]",c.title);assertEquals("https://a.example/logo,a.png",c.logo)}
 @Test fun catalogueAssetProvidesActualKnownCountryCodes() {val a=JSONArray(java.io.File(System.getProperty("kandianshi.assets"),"iptv-org-countries.json").readText());val codes=(0 until a.length()).map {a.getJSONObject(it).getString("code")};assertEquals(codes.size,codes.distinct().size);assertTrue(codes.size>=200);assertTrue(codes.containsAll(listOf("CN","US","UK","HK","TW")))}
 @Test fun countryMarkersCannotSmuggleUnsupportedUrlData() {assertNull(IptvOrgPolicy.country("国际频道·x [../../]"));assertNull(IptvOrgPolicy.country("News"));assertNull(IptvOrgPolicy.country("国际频道·x [US]\nextra"))}
 @Test fun alternativeRoutesRetainTheirCountryAndAreDeduplicated() {val text=foreign+"\n#EXTINF:-1 tvg-id=\"Example.us\" group-title=\"News\",Example TV\nhttps://b.example/live.m3u8";val c=PlaylistParser.parse(text,internationalCountry="US").single();assertEquals(2,c.urls.size);assertEquals("US",IptvOrgPolicy.country(c.group))}
 @Test fun standardGbAliasUsesThePublishedUkCountryCode() {assertEquals(listOf("UK"),IptvOrgPolicy.clean(listOf("GB","UK"),setOf("UK")));assertEquals("UK",IptvOrgPolicy.country(IptvOrgPolicy.group("GB")));assertEquals(IptvOrgPolicy.url("UK"),IptvOrgPolicy.url("GB"))}
}
