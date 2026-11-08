package com.lizongying.mytv.agent;
import android.content.Context;
import android.os.Handler;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.audio.AudioRendererEventListener;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import java.util.ArrayList;
@UnstableApi
public final class ChannelRenderersFactory extends DefaultRenderersFactory {
    public ChannelRenderersFactory(Context context) { super(context);setEnableDecoderFallback(true); }
    @Override protected void buildAudioRenderers(Context context,int mode,MediaCodecSelector selector,boolean fallback,AudioSink sink,Handler handler,AudioRendererEventListener listener,ArrayList<Renderer> renderers) {
        super.buildAudioRenderers(context,mode,selector,fallback,sink,handler,listener,renderers);
        renderers.add(new MpegLayer2AudioRenderer(handler,listener,sink));
    }
}
