package com.lizongying.mytv
import com.lizongying.mytv.agent.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
class CandidatePolicyTest {
 private fun ch(name: String, vararg u: String)=PlaylistChannel(name,"央视","",u.toList())
 @Test fun importedRoutesAreOnlyCandidates() {
  val b=JSONObject();CandidatePolicy.add(b,listOf(ch("CCTV1","https://a.example/1.m3u8","https://a.example/2.m3u8")),10)
  assertEquals(2,b.length());assertFalse(b.toString().contains("passed"))
 }
 @Test fun duplicateImportsDoNotResetRetryCooldown() {
  val b=JSONObject();val c=ch("CCTV1","https://a.example/1.m3u8");CandidatePolicy.add(b,listOf(c),10)
  val id=b.keys().next();CandidatePolicy.failed(b,id,100);CandidatePolicy.add(b,listOf(c),200)
  assertTrue(CandidatePolicy.due(b,201,"").isEmpty());assertEquals(1,CandidatePolicy.due(b,6*3600000L+101,"").size)
 }
 @Test fun currentPlayingRouteIsNotProbed() {
  val b=JSONObject();CandidatePolicy.add(b,listOf(ch("CCTV1","https://a.example/1.m3u8")),1)
  assertTrue(CandidatePolicy.due(b,2,"https://a.example/1.m3u8").isEmpty())
 }
 @Test fun routeParametersAreNeverMerged() {
  val b=JSONObject();CandidatePolicy.add(b,listOf(ch("CCTV1","https://a.example/play?id=1","https://a.example/play?id=2")),1)
  assertEquals(2,b.length())
 }
 @Test fun localAddressesDoNotEnterQueue() {
  val b=JSONObject();CandidatePolicy.add(b,listOf(ch("CCTV1","http://127.0.0.1/x","http://192.168.1.1/x")),1);assertEquals(0,b.length())
 }
 @Test fun channelsAreInterleavedBeforeTheirMirrors() {
  val b=JSONObject();CandidatePolicy.add(b,listOf(ch("CCTV1","https://a.example/1","https://a.example/2"),ch("CCTV2","https://b.example/1")),1)
  val items=b.keys().asSequence().map { b.getJSONObject(it).getString("url") }.toSet()
  assertEquals(3,items.size)
  assertEquals(listOf("https://a.example/1","https://b.example/1","https://a.example/2"),CandidatePolicy.due(b,2,"").map { b.getJSONObject(it).getString("url") })
 }
 @Test fun candidateStorageIsBounded() {
  val b=JSONObject();CandidatePolicy.add(b,(0..1100).map { ch("CCTV$it","https://a.example/$it") },1)
  assertTrue(b.length()<=CandidatePolicy.LIMIT);assertTrue(b.toString().toByteArray().size<=CandidatePolicy.MAX_BYTES)
 }
 @Test fun failedRoutesAreReviewedSoonWithoutStarvingUnseenRoutes() {
  val urls=(0..9).map { "https://a.example/$it" }
  val health=mapOf(urls[9] to HealthRecord(false,1,0,failures=1,lastFailureAt=1))
  val selection=LightHealthSelection.select(urls,health,0,"",40_000)
  assertEquals(urls[9],selection.first.first());assertEquals(6,selection.first.size);assertEquals(5,selection.second)
 }
 @Test fun reviewNeverProbesCurrentRouteOrRepeatsTooSoon() {
  val urls=listOf("https://a.example/1","https://a.example/2")
  val health=mapOf(urls[0] to HealthRecord(false,1,0,failures=1,lastFailureAt=1))
  assertEquals(listOf(urls[1]),LightHealthSelection.select(urls,health,0,urls[0],40_000).first)
 }

 @Test fun repeatedBadCandidatesCannotOccupyQueueForever() {
  val b=JSONObject();CandidatePolicy.add(b,listOf(ch("CCTV1","https://a.example/1")),1);val id=b.keys().next()
  repeat(3) { CandidatePolicy.failed(b,id,10L+it) };assertFalse(b.has(id))
 }
 @Test fun renderedVideoRanksAheadOfAnUntestedDecoderDespiteSlowerHttp() {
  val now=100_000L
  val h=mapOf("rendered" to HealthRecord(true,now,500,lastPlaybackSuccess=now),"networkOnly" to HealthRecord(true,now,0))
  assertEquals(listOf("rendered","networkOnly"),HealthRanking.sort(listOf("networkOnly","rendered"),h,emptySet(),now))
 }

 @Test fun duplicateStationTitlesCannotChangeAnActiveStream() {
  val rows=listOf("综合频道" to listOf("https://a.example/1"),"综合频道" to listOf("https://b.example/2"))
  assertEquals(1,ChannelSelection.choose(rows,"https://b.example/2","综合频道",0))
 }
 @Test fun removedRouteFallsBackToTheSameStationThenABoundedPosition() {
  val rows=listOf("CCTV1" to listOf("new"),"CCTV2" to listOf("two"))
  assertEquals(0,ChannelSelection.choose(rows,"old","CCTV1",1))
  assertEquals(1,ChannelSelection.choose(rows,"old","missing",999))
  assertEquals(0,ChannelSelection.choose(emptyList(),"","",-3))
 }
 @Test fun protectedUriWinsWhenStationNamesChange() {
  assertEquals(1,ChannelSelection.choose(listOf("old" to listOf("other"),"renamed" to listOf("playing")),"playing","old",0))
 }
 @Test fun coolingCandidatesMakeSpaceForFreshSources() {
  val b=JSONObject()
  repeat(CandidatePolicy.LIMIT) { i -> b.put(MemoryPolicy.id("https://a.example/$i"),JSONObject().put("next",100)) }
  CandidatePolicy.add(b,listOf(ch("CCTV1","https://new.example/live.m3u8")),10)
  assertTrue(b.has(MemoryPolicy.id("https://new.example/live.m3u8")));assertTrue(b.length()<=CandidatePolicy.LIMIT)
 }

}
