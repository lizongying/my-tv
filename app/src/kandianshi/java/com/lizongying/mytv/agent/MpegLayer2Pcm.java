package com.lizongying.mytv.agent;

import java.io.InputStream;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

/** Stateful Layer II synthesis; one complete extractor sample per call, no disk IO. */
public final class MpegLayer2Pcm {
    public static final int MAX_FRAME_BYTES=8192;
    private final FrameInput input=new FrameInput();
    private final Bitstream bits=new Bitstream(input);
    private final Decoder decoder=new Decoder();
    public static final class Pcm {
        public final int sampleRate,channels;
        public final short[] samples;
        Pcm(SampleBuffer decoded) {
            sampleRate=decoded.getSampleFrequency();channels=decoded.getChannelCount();
            int size=decoded.getBufferLength();
            if(size<=0 || size>2304 || channels<1 || channels>2) throw new IllegalArgumentException("Invalid Layer II output");
            samples=java.util.Arrays.copyOf(decoded.getBuffer(),size);
        }
    }
    public Pcm decode(byte[] frame) throws Exception {
        if(frame.length<4 || frame.length>MAX_FRAME_BYTES) throw new IllegalArgumentException("Invalid Layer II frame size");
        input.offer(frame);
        try {
            Header header=bits.readFrame();
            if(header==null || header.layer()!=2) throw new IllegalArgumentException("Not a Layer II frame");
            return new Pcm((SampleBuffer)decoder.decodeFrame(header,bits));
        } finally { bits.closeFrame(); }
    }
    private static final class FrameInput extends InputStream {
        private byte[] bytes=new byte[0];private int offset;
        void offer(byte[] frame) { bytes=frame;offset=0; }
        @Override public int read() { return offset<bytes.length ? bytes[offset++]&255 : -1; }
        @Override public int read(byte[] target,int start,int count) {
            if(count==0) return 0;
            if(offset>=bytes.length) return -1;
            int size=Math.min(count,bytes.length-offset);System.arraycopy(bytes,offset,target,start,size);offset+=size;return size;
        }
    }
}
