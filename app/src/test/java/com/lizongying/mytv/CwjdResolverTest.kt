package com.lizongying.mytv
import com.lizongying.mytv.agent.*
import org.junit.Assert.*
import org.junit.Test
class CwjdResolverTest {
 @Test fun signatureMatchesTheSuppliedProtocol() { assertEquals("1700000000-Ab12Cd-0981d8c47ba5ae4fe85025d3a43f1614",CwjdProtocol.sign("/v1/resourceProductRightsAuth",1700000000,"Ab12Cd")) }
 @Test fun channelIdentityCannotSelectOtherResources() {assertTrue(CwjdProtocol.matches(CwjdProtocol.ROUTE));assertFalse(CwjdProtocol.matches(CwjdProtocol.ROUTE.replace("30167","1")));assertFalse(CwjdProtocol.matches(CwjdProtocol.ROUTE+"&id=1"))}
 @Test fun deniedRightsNeverProduceAPlayableAddress() {assertTrue(runCatching {CwjdProtocol.success("{\"code\":1,\"data\":{\"url\":\"https://a.example/play\"}}")}.isFailure)}
 @Test fun dispatcherCannotRedirectSignedRequestsToAnotherHost() {for(url in listOf("http://live-dispatcher.5gtv.com.cn/a","https://evil.example/a","https://live-dispatcher.5gtv.com.cn:8443/a")) assertTrue(runCatching {CwjdProtocol.dispatcher(url)}.isFailure)}
 @Test fun temporaryUrlMustBePublic() {assertTrue(runCatching {CwjdProtocol.success("{\"code\":0,\"data\":{\"url\":\"http://127.0.0.1/a\"}}")}.isFailure)}
 @Test fun cacheUsesMemoryAndRefreshesAfterFiveMinutes() {var now=100L;var calls=0;val c=CwjdCache({"https://a.example/live?token=${++calls}"},{now});assertEquals(c.resolve(CwjdProtocol.ROUTE),c.resolve(CwjdProtocol.ROUTE));assertEquals(1,calls);now+=300001;assertTrue(c.resolve(CwjdProtocol.ROUTE).endsWith("=2"))}
 @Test fun ongoingManifestAliasCanRefreshWithoutChangingChannelIdentity() {var now=100L;var calls=0;val c=CwjdCache({"https://a.example/live?token=${++calls}"},{now});val first=c.resolve(CwjdProtocol.ROUTE);repeat(70) {now+=300001;c.resolve(CwjdProtocol.ROUTE)};assertEquals(c.resolve(CwjdProtocol.ROUTE),c.resolve(first))}
 @Test fun unrelatedSegmentsNeverTriggerAuthentication() {val c=CwjdCache({error("Unrelated request must not authenticate")});assertEquals("https://a.example/segment.ts",c.resolve("https://a.example/segment.ts"))}
 @Test fun failedApiRequestsHaveABoundedRetryDelay() {var now=100L;var n=0;val c=CwjdCache({n++;error("Unavailable")},{now});repeat(3){runCatching {c.resolve(CwjdProtocol.ROUTE)}};assertEquals(1,n);now+=15001;runCatching {c.resolve(CwjdProtocol.ROUTE)};assertEquals(2,n)}
 @Test fun explicitRefreshDiscardsTheCachedManifest() {var n=0;val c=CwjdCache({"https://a.example/play?n=${++n}"});c.resolve(CwjdProtocol.ROUTE);c.invalidate();assertTrue(c.resolve(CwjdProtocol.ROUTE).endsWith("=2"))}
}
