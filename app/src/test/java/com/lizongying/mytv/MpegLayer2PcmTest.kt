package com.lizongying.mytv
import com.lizongying.mytv.agent.MpegLayer2Pcm
import org.junit.Assert.*
import org.junit.Test
class MpegLayer2PcmTest {
    // Original MPEG-1 Layer II frames: zero subband allocations encode silence.
    private fun frame(mono: Boolean=false,rate44100: Boolean=false)=ByteArray(if(rate44100) 417 else 384).apply {
        this[0]=0xff.toByte();this[1]=0xfd.toByte();this[2]=(if(rate44100) 0x80 else 0x84).toByte();this[3]=(if(mono) 0xc0 else 0).toByte()
    }
    @Test fun decodesStereo48kPcm() { val out=MpegLayer2Pcm().decode(frame());assertEquals(48000,out.sampleRate);assertEquals(2,out.channels);assertEquals(2304,out.samples.size);assertTrue(out.samples.all { it.toInt()==0 }) }
    @Test fun preservesDecoderStateAcrossFrameFeeds() { val decoder=MpegLayer2Pcm();repeat(100) { assertEquals(2304,decoder.decode(frame()).samples.size) } }
    @Test fun decodesMonoWithoutInventingASecondChannel() { val out=MpegLayer2Pcm().decode(frame(mono=true));assertEquals(1,out.channels);assertEquals(1152,out.samples.size) }
    @Test fun derives44100HzFromTheMpegHeader() { val out=MpegLayer2Pcm().decode(frame(rate44100=true));assertEquals(44100,out.sampleRate);assertEquals(2304,out.samples.size) }
    @Test fun malformedAndOversizedFramesAreRejected() { for(data in listOf(ByteArray(0),ByteArray(8),ByteArray(8193))) assertTrue(runCatching { MpegLayer2Pcm().decode(data) }.isFailure) }
}
