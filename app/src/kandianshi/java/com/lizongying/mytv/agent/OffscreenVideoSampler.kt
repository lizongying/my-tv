package com.lizongying.mytv.agent

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.*
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Base64
import android.view.Surface
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/** Decodes to a private offscreen surface, never a window or a screen-capture permission. */
@OptIn(UnstableApi::class)
class OffscreenVideoSampler(private val context: Context) {
    suspend fun sample(route: VisionRoute, allowed: ()->Boolean, spacingMs: Long=10_000, repeatForTest: Boolean=false): List<String> {
        require(spacingMs in 200..10_000)
        val thread=HandlerThread("tv-auto-video").apply { start() }
        val handler=Handler(thread.looper)
        try {
            return withContext(handler.asCoroutineDispatcher("tv-auto-video")) {
                fun guard() { if (!allowed()) throw ScanDeferred("用户播放优先，后台巡检已让出解码器") }
                guard()
                val output=OffscreenSurface(handler)
                var player: ExoPlayer?=null
                try {
                    val http=DefaultHttpDataSource.Factory().setUserAgent("Mozilla/5.0 (Linux; Android) KanDianshi/1.5")
                        .setConnectTimeoutMs(8000).setReadTimeoutMs(8000).setAllowCrossProtocolRedirects(true)
                    player=ExoPlayer.Builder(context).setLooper(Looper.myLooper()!!)
                        .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(context,http))).build()
                    val p=player
                    p.trackSelectionParameters=p.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_AUDIO,true).build()
                    p.volume=0f
                    var error: PlaybackException?=null
                    p.addListener(object: Player.Listener {
                        override fun onPlayerError(e: PlaybackException) { error=e }
                        override fun onVideoSizeChanged(size: VideoSize) {
                            if(size.width>0 && size.height>0) runCatching { output.resize(size) }.onFailure { output.noteFailure(it) }
                        }
                    })
                    p.setVideoSurface(output.surface)
                    p.repeatMode=if(repeatForTest) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                    p.setMediaItem(AgentRepository.mediaItem(context,route.url)); p.prepare(); p.play()
                    withTimeout(20_000) {
                        while(output.frames==0L || !p.isPlaying) {
                            guard(); error?.let { throw it }; output.failure?.let { throw it }; delay(100)
                        }
                    }
                    suspend fun wait(ms: Long) {
                        var left=ms
                        while(left>0) { guard(); error?.let { throw it }; val step=minOf(left,200); delay(step); left-=step }
                    }
                    wait(3000)
                    val pictures=mutableListOf<String>()
                    var last=-1L
                    repeat(VisionPolicy.FRAME_COUNT) { index ->
                        if(index>0) wait(spacingMs)
                        withTimeout(5000) {
                            while(output.frames<=last || !p.isPlaying) { guard(); error?.let { throw it }; delay(100) }
                        }
                        guard(); output.failure?.let { throw it }; last=output.frames
                        val bitmap=output.bitmap()
                        try {
                            val pixels=IntArray(bitmap.width*bitmap.height)
                            bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
                            check(VisionFrameQuality.usable(pixels)) { "后台截图缺少可靠视频画面" }
                            val out=ByteArrayOutputStream()
                            check(bitmap.compress(Bitmap.CompressFormat.JPEG,82,out))
                            val bytes=out.toByteArray(); check(bytes.size<=440_000)
                            pictures.add("data:image/jpeg;base64,"+Base64.encodeToString(bytes,Base64.NO_WRAP))
                        } finally { bitmap.recycle() }
                    }
                    check(pictures.distinct().size>1) { "三张画面完全相同，保留线路" }
                    pictures
                } finally {
                    try { player?.release() } finally { output.close() }
                }
            }
        } finally { thread.quitSafely() }
    }
}

private class OffscreenSurface(private val handler: Handler) {
    private var display: EGLDisplay=EGL14.EGL_NO_DISPLAY
    private var context: EGLContext=EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface=EGL14.EGL_NO_SURFACE
    private var config: EGLConfig?=null
    private var texture: SurfaceTexture?=null
    private var videoSurface: Surface?=null
    val surface: Surface get()=checkNotNull(videoSurface)
    var frames=0L; private set
    var failure: Exception?=null; private set
    private var textureId=0
    private var program=0
    private var width=960
    private var height=540
    private var rotation=0
    private val transform=FloatArray(16)
    private val vertices: FloatBuffer=ByteBuffer.allocateDirect(8*4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        .apply { put(floatArrayOf(-1f,-1f,1f,-1f,-1f,1f,1f,1f)); position(0) }
    private val coords: FloatBuffer=ByteBuffer.allocateDirect(8*4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        .apply { put(floatArrayOf(0f,0f,1f,0f,0f,1f,1f,1f)); position(0) }
    init {
        try {
            display=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val versions=IntArray(2)
            check(display!=EGL14.EGL_NO_DISPLAY && EGL14.eglInitialize(display,versions,0,versions,1))
            val configs=arrayOfNulls<EGLConfig>(1); val count=IntArray(1)
            val attributes=intArrayOf(EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,
                EGL14.EGL_ALPHA_SIZE,8,EGL14.EGL_RENDERABLE_TYPE,EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE,EGL14.EGL_PBUFFER_BIT,EGL14.EGL_NONE)
            check(EGL14.eglChooseConfig(display,attributes,0,configs,0,1,count,0) && count[0]>0)
            config=configs[0]
            context=EGL14.eglCreateContext(display,config,EGL14.EGL_NO_CONTEXT,intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION,2,EGL14.EGL_NONE),0)
            check(context!=EGL14.EGL_NO_CONTEXT)
            createSurface()
            val ids=IntArray(1); GLES20.glGenTextures(1,ids,0); textureId=ids[0]
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE)
            program=link(
                "attribute vec4 aPosition; attribute vec4 aTex; uniform mat4 uTransform; varying vec2 vTex; void main(){ gl_Position=aPosition; vTex=(uTransform*aTex).xy; }",
                "#extension GL_OES_EGL_image_external : require\nprecision mediump float; varying vec2 vTex; uniform samplerExternalOES uTexture; void main(){ gl_FragColor=texture2D(uTexture,vTex); }")
            texture=SurfaceTexture(textureId).also { st ->
                st.setOnFrameAvailableListener({
                    try { st.updateTexImage(); frames++ } catch(e: Exception) { failure=e }
                },handler)
                videoSurface=Surface(st)
            }
        } catch(e: Exception) { close(); throw e }
    }
    private fun createSurface() {
        eglSurface=EGL14.eglCreatePbufferSurface(display,config,intArrayOf(EGL14.EGL_WIDTH,width,EGL14.EGL_HEIGHT,height,EGL14.EGL_NONE),0)
        check(eglSurface!=EGL14.EGL_NO_SURFACE && EGL14.eglMakeCurrent(display,eglSurface,eglSurface,context))
    }
    fun noteFailure(error: Throwable) { failure=error as? Exception ?: IllegalStateException("后台图像初始化失败") }
    fun resize(size: VideoSize) {
        var ratio=size.width*size.pixelWidthHeightRatio/size.height
        if(!ratio.isFinite() || ratio<=0) return
        rotation=size.unappliedRotationDegrees
        if(rotation%180!=0) ratio=1f/ratio
        val w=if(ratio>=1) minOf(960,size.width) else (minOf(960,size.height)*ratio).toInt().coerceAtLeast(1)
        val h=if(ratio>=1) (w/ratio).toInt().coerceAtLeast(1) else minOf(960,size.height)
        if(width==w && height==h) return
        EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,context)
        EGL14.eglDestroySurface(display,eglSurface)
        width=w; height=h; createSurface()
    }
    fun bitmap(): Bitmap {
        check(frames>0)
        texture!!.getTransformMatrix(transform)
        if(rotation!=0) {
            val rotate=FloatArray(16); val composed=FloatArray(16)
            Matrix.setIdentityM(rotate,0); Matrix.translateM(rotate,0,.5f,.5f,0f)
            Matrix.rotateM(rotate,0,rotation.toFloat(),0f,0f,1f); Matrix.translateM(rotate,0,-.5f,-.5f,0f)
            Matrix.multiplyMM(composed,0,transform,0,rotate,0); composed.copyInto(transform)
        }
        GLES20.glViewport(0,0,width,height); GLES20.glUseProgram(program)
        val position=GLES20.glGetAttribLocation(program,"aPosition"); val uv=GLES20.glGetAttribLocation(program,"aTex")
        GLES20.glEnableVertexAttribArray(position); GLES20.glEnableVertexAttribArray(uv)
        GLES20.glVertexAttribPointer(position,2,GLES20.GL_FLOAT,false,0,vertices)
        GLES20.glVertexAttribPointer(uv,2,GLES20.GL_FLOAT,false,0,coords)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program,"uTransform"),1,false,transform,0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0); GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"uTexture"),0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4)
        val buffer=ByteBuffer.allocateDirect(width*height*4)
        GLES20.glReadPixels(0,0,width,height,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,buffer)
        check(GLES20.glGetError()==GLES20.GL_NO_ERROR) { "后台图像读取失败" }
        buffer.rewind(); val bytes=ByteArray(buffer.remaining()); buffer.get(bytes)
        return Bitmap.createBitmap(FramePixels.argb(bytes,width,height),width,height,Bitmap.Config.ARGB_8888)
    }
    private fun shader(type: Int,source: String): Int {
        val id=GLES20.glCreateShader(type); GLES20.glShaderSource(id,source); GLES20.glCompileShader(id)
        val ok=IntArray(1); GLES20.glGetShaderiv(id,GLES20.GL_COMPILE_STATUS,ok,0)
        if(ok[0]==0) { GLES20.glDeleteShader(id); error("后台图像着色器不受支持") }; return id
    }
    private fun link(vertex: String,fragment: String): Int {
        val v=shader(GLES20.GL_VERTEX_SHADER,vertex); var f=0; var p=0
        try {
            f=shader(GLES20.GL_FRAGMENT_SHADER,fragment); p=GLES20.glCreateProgram()
            GLES20.glAttachShader(p,v); GLES20.glAttachShader(p,f); GLES20.glLinkProgram(p)
            val ok=IntArray(1); GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0)
            check(ok[0]!=0); return p
        } catch(e: Exception) { if(p!=0) GLES20.glDeleteProgram(p); throw e }
        finally { GLES20.glDeleteShader(v); if(f!=0) GLES20.glDeleteShader(f) }
    }
    fun close() {
        texture?.setOnFrameAvailableListener(null)
        videoSurface?.release(); videoSurface=null
        texture?.release(); texture=null
        if(display!=EGL14.EGL_NO_DISPLAY) {
            if(context!=EGL14.EGL_NO_CONTEXT && eglSurface!=EGL14.EGL_NO_SURFACE) {
                EGL14.eglMakeCurrent(display,eglSurface,eglSurface,context)
                if(program!=0) GLES20.glDeleteProgram(program)
                if(textureId!=0) GLES20.glDeleteTextures(1,intArrayOf(textureId),0)
            }
            EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT)
            if(eglSurface!=EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display,eglSurface)
            if(context!=EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display,context)
            EGL14.eglReleaseThread(); EGL14.eglTerminate(display)
        }
        display=EGL14.EGL_NO_DISPLAY; context=EGL14.EGL_NO_CONTEXT; eglSurface=EGL14.EGL_NO_SURFACE
    }
}
