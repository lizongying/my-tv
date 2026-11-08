package com.lizongying.mytv
import com.lizongying.mytv.agent.*
import org.junit.Assert.*
import org.junit.Test
class TransportTracksTest {
    private fun packet(pid: Int,section: List<Int>)=ByteArray(188) { 0xff.toByte() }.apply {
        this[0]=0x47;this[1]=(0x40 or (pid shr 8)).toByte();this[2]=pid.toByte();this[3]=0x10;this[4]=0
        section.forEachIndexed { i,v -> this[5+i]=v.toByte() }
    }
    private fun pat()=packet(0,listOf(0,0xb0,13,0,1,0xc1,0,0,0,1,0xe1,0,0,0,0,0))
    private fun pmt(vararg types: Int): ByteArray {
        val s=mutableListOf(2,0xb0,0,0,1,0xc1,0,0,0xe1,1,0xf0,0)
        types.forEach { s.addAll(listOf(it,0xe1,1,0xf0,0)) };s.addAll(listOf(0,0,0,0));s[2]=s.size-3
        return packet(256,s)
    }
    @Test fun h264AndAudioAreVideo() { assertEquals(TransportTracks.Evidence.VIDEO,TransportTracks.evidence(pat()+pmt(27,4))) }
    @Test fun onlyAacInACompleteProgramIsRejected() {
        val bytes=pat()+pmt(15)
        assertEquals(TransportTracks.Evidence.AUDIO_ONLY,TransportTracks.evidence(bytes))
        assertTrue(runCatching { HttpFetch.probe("https://example.org/audio.ts") { HttpFetch.Reply(it,bytes,"video/mp2t") } }.exceptionOrNull()?.message.orEmpty().contains("只有音频"))
    }
    @Test fun noPatCannotProveThereIsNoVideo() { assertEquals(TransportTracks.Evidence.UNKNOWN,TransportTracks.evidence(pmt(15)+packet(8191,emptyList()))) }
    @Test fun incompletePmtRemainsUnknown() { assertEquals(TransportTracks.Evidence.UNKNOWN,TransportTracks.evidence(pat()+pmt(15).copyOf(20))) }
    @Test fun unknownPrivateTracksRemainUnknown() { assertEquals(TransportTracks.Evidence.UNKNOWN,TransportTracks.evidence(pat()+pmt(6))) }
}
