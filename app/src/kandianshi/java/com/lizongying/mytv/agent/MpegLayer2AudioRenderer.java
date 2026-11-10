package com.lizongying.mytv.agent;

import android.os.Handler;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.decoder.CryptoConfig;
import androidx.media3.decoder.DecoderException;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.decoder.SimpleDecoder;
import androidx.media3.decoder.SimpleDecoderOutputBuffer;
import androidx.media3.exoplayer.audio.AudioRendererEventListener;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.DecoderAudioRenderer;
import java.nio.ByteOrder;

/** Hardware audio remains first choice. This renderer only supports unencrypted Layer II. */
@UnstableApi
public final class MpegLayer2AudioRenderer extends DecoderAudioRenderer<MpegLayer2AudioRenderer.Layer2Decoder> {
    public MpegLayer2AudioRenderer(Handler handler,AudioRendererEventListener listener,AudioSink sink) { super(handler,listener,sink); }
    @Override public String getName() { return "JLayer-MPEG-L2"; }
    @Override protected int supportsFormatInternal(Format format) {
        if(!MimeTypes.isAudio(format.sampleMimeType)) return C.FORMAT_UNSUPPORTED_TYPE;
        if(!"audio/mpeg-L2".equals(format.sampleMimeType)) return C.FORMAT_UNSUPPORTED_SUBTYPE;
        if(format.cryptoType!=C.CRYPTO_TYPE_NONE) return C.FORMAT_UNSUPPORTED_DRM;
        if(format.channelCount<1 || format.channelCount>2 || format.sampleRate<=0) return C.FORMAT_UNSUPPORTED_SUBTYPE;
        Format pcm=new Format.Builder().setSampleMimeType(MimeTypes.AUDIO_RAW).setChannelCount(format.channelCount).setSampleRate(format.sampleRate).setPcmEncoding(C.ENCODING_PCM_16BIT).build();
        return sinkSupportsFormat(pcm) ? C.FORMAT_HANDLED : C.FORMAT_UNSUPPORTED_SUBTYPE;
    }
    @Override protected Layer2Decoder createDecoder(Format format,CryptoConfig crypto) { return new Layer2Decoder(format); }
    @Override protected Format getOutputFormat(Layer2Decoder decoder) {
        return new Format.Builder().setSampleMimeType(MimeTypes.AUDIO_RAW).setSampleRate(decoder.rate).setChannelCount(decoder.channels).setPcmEncoding(C.ENCODING_PCM_16BIT).build();
    }
    static final class Layer2Exception extends DecoderException {
        Layer2Exception(Throwable cause) { super("MPEG Layer II decode failed",cause); }
    }
    static final class Layer2Decoder extends SimpleDecoder<DecoderInputBuffer,SimpleDecoderOutputBuffer,Layer2Exception> {
        private MpegLayer2Pcm synthesis=new MpegLayer2Pcm();
        volatile int rate,channels;
        Layer2Decoder(Format format) {
            super(new DecoderInputBuffer[12],new SimpleDecoderOutputBuffer[12]);
            rate=format.sampleRate;channels=format.channelCount;setInitialInputBufferSize(MpegLayer2Pcm.MAX_FRAME_BYTES);
        }
        @Override public String getName() { return "jlayer-mp2"; }
        @Override protected DecoderInputBuffer createInputBuffer() { return new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_NORMAL); }
        @Override protected SimpleDecoderOutputBuffer createOutputBuffer() { return new SimpleDecoderOutputBuffer(this::releaseOutputBuffer); }
        @Override protected Layer2Exception createUnexpectedDecodeException(Throwable cause) { return new Layer2Exception(cause); }
        @Override protected Layer2Exception decode(DecoderInputBuffer input,SimpleDecoderOutputBuffer output,boolean reset) {
            try {
                if(reset) synthesis=new MpegLayer2Pcm();
                if(input.data==null || input.data.remaining()>MpegLayer2Pcm.MAX_FRAME_BYTES) throw new IllegalArgumentException("Layer II input too large");
                byte[] frame=new byte[input.data.remaining()];input.data.get(frame);
                MpegLayer2Pcm.Pcm pcm=synthesis.decode(frame);rate=pcm.sampleRate;channels=pcm.channels;
                output.init(input.timeUs,pcm.samples.length*2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcm.samples);
                return null;
            } catch(Exception cause) { return new Layer2Exception(cause); }
        }
    }
}
