package com.lizongying.mytv
import com.lizongying.mytv.agent.PlaybackRoutes
import org.junit.Assert.*
import org.junit.Test
class PlaybackRoutesTest {
 @Test fun continuesForwardInsteadOfReturningToTheFirstRoute() { assertEquals(3,PlaybackRoutes.after(listOf("a","b","c","d","e"),"c",setOf("c"))) }
 @Test fun wrapsAroundTheEnd() { assertEquals(0,PlaybackRoutes.after(listOf("a","b","c"),"c",setOf("c"))) }
 @Test fun skipsFailedMirrorsIncludingDuplicates() { assertEquals(4,PlaybackRoutes.after(listOf("a","b","c","b","d"),"a",setOf("a","b","c"))) }
 @Test fun anExplicitRetryCanContinueBeyondThePreviousRound() { val urls=listOf("a","b","c","d","e","f");assertEquals(3,PlaybackRoutes.after(urls,"c",emptySet()));assertEquals(4,PlaybackRoutes.after(urls,"d",setOf("d"))) }
 @Test fun exhaustedAndEmptyListsStop() { assertNull(PlaybackRoutes.after(emptyList(),"a",emptySet()));assertNull(PlaybackRoutes.after(listOf("a"),"a",setOf("a"))) }
}
