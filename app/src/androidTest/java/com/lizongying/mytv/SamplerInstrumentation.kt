package com.lizongying.mytv

import android.app.Activity
import android.app.Instrumentation
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Bundle
import android.os.SystemClock
import android.util.Base64
import com.lizongying.mytv.agent.*
import kotlinx.coroutines.*
import java.io.File

/** An original moving test clip, no network, credentials or model requests. */
class SamplerInstrumentation: Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result=Bundle()
        try {
            runBlocking {
                verifyDefaultScheduling()
                result.putString("scheduler","DEFAULT_SCHEDULER_OK")
                val file=File(targetContext.cacheDir,"sampler-original-test.mp4")
                try {
                    if(!file.exists() || file.length()<1024) fixture(file)
                    val route=VisionRoute("本地移动测试图",file.toURI().toString())
                    var cancelled=false; val start=SystemClock.elapsedRealtime()
                    try {
                        OffscreenVideoSampler(targetContext).sample(route,
                            { SystemClock.elapsedRealtime()-start<1200 },400,true)
                    } catch(_: ScanDeferred) { cancelled=true }
                    check(cancelled) { "取样未及时响应前台请求" }
                    val frames=OffscreenVideoSampler(targetContext).sample(route,{true},400,true)
                    check(frames.size==3 && frames.distinct().size>1)
                    frames.forEach {
                        val bytes=Base64.decode(it.substringAfter(','),Base64.NO_WRAP)
                        val image=checkNotNull(BitmapFactory.decodeByteArray(bytes,0,bytes.size))
                        try {
                            check(image.width==320 && image.height==192) { "画面比例不正确" }
                            val pixels=IntArray(image.width*image.height)
                            image.getPixels(pixels,0,image.width,0,0,image.width,image.height)
                            check(VisionFrameQuality.usable(pixels)) { "解码画面不可靠" }
                            // Bright top/left marker must remain in the original corner.
                            val top=pixels[12*image.width+12] and 255
                            val bottom=pixels[(image.height-13)*image.width+12] and 255
                            check(top>210 && bottom<210) { "画面上下翻转或角落内容丢失" }
                        } finally { image.recycle() }
                    }
                    verifySettingsUi()
                    result.putString("stream","OFFSCREEN_CAPTURE_OK: default persistent scheduling/cancellation, 3 JPEGs, original corner retained, cancellation and second capture passed")
                } finally { file.delete() }
            }
            finish(Activity.RESULT_OK,result)
        } catch(e: Throwable) {
            result.putString("stream","OFFSCREEN_CAPTURE_FAILED: "+e.stackTraceToString())
            finish(Activity.RESULT_CANCELED,result)
        }
    }
    private fun verifySettingsUi() {
        val activity=startActivitySync(android.content.Intent(targetContext,AutoVisionActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        try {
            waitForIdleSync()
            Thread.sleep(700)
            val screenshot=checkNotNull(uiAutomation.takeScreenshot())
            try {
                File(targetContext.cacheDir,"auto-settings-test.png").outputStream().use {
                    check(screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it))
                }
            } finally { screenshot.recycle() }
        } finally { runOnMainSync { activity.finish() } }
    }
    private fun verifyDefaultScheduling() {
        val settings=AutoVisionSettings(targetContext)
        check(settings.enabled() && settings.autoBlock() && settings.wifiOnly()) { "自动巡检默认值不正确" }
        // This emulator has no API key. Test only flags and delayed persisted scheduling.
        check(AgentConfigStore(targetContext).apiKey().isBlank())
        val prefs=targetContext.getSharedPreferences("agent_config",android.content.Context.MODE_PRIVATE)
        val previous=prefs.getString("config",null)
        val wm=androidx.work.WorkManager.getInstance(targetContext)
        try {
            check(prefs.edit().putString("config",org.json.JSONObject().put("enabled",true)
                .put("aiEnabled",true).put("intervalHours",6).toString()).commit())
            AutoVisionScheduler.schedule(targetContext)
            val periodic=wm.getWorkInfosForUniqueWork("auto-vision-periodic").get(20,java.util.concurrent.TimeUnit.SECONDS)
            check(periodic.any { !it.state.isFinished }) { "自动周期任务未持久化" }
            AutoVisionScheduler.nudge(targetContext)
            val once=wm.getWorkInfosForUniqueWork("auto-vision-once").get(20,java.util.concurrent.TimeUnit.SECONDS)
            check(once.any { !it.state.isFinished }) { "空闲补查任务未持久化" }
            settings.setEnabled(false)
            val limit=SystemClock.elapsedRealtime()+10_000
            while(true) {
                val jobs=wm.getWorkInfosForUniqueWork("auto-vision-periodic").get()+
                    wm.getWorkInfosForUniqueWork("auto-vision-once").get()
                if(jobs.all { it.state.isFinished }) break
                check(SystemClock.elapsedRealtime()<limit) { "关闭后后台任务未取消" }
                Thread.sleep(100)
            }
        } finally {
            check(prefs.edit().putString("config",previous).commit())
            settings.setEnabled(true)
        }
    }
    private fun fixture(file: File) {
        val width=320; val height=192; val fps=15
        val codec=MediaCodec.createEncoderByType("video/avc")
        val formats=codec.codecInfo.getCapabilitiesForType("video/avc").colorFormats
        val format=if(formats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar))
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar else MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
        check(formats.contains(format))
        val media=MediaFormat.createVideoFormat("video/avc",width,height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT,format)
            setInteger(MediaFormat.KEY_BIT_RATE,500_000)
            setInteger(MediaFormat.KEY_FRAME_RATE,fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1)
        }
        val muxer=MediaMuxer(file.absolutePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxing=false; var configured=false
        try {
            codec.configure(media,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE); codec.start(); configured=true
            var frame=0; var sentEos=false; var done=false; var track=-1
            val info=MediaCodec.BufferInfo(); val limit=SystemClock.elapsedRealtime()+25_000
            while(!done) {
                check(SystemClock.elapsedRealtime()<limit) { "本地视频生成超时" }
                if(!sentEos) {
                    val i=codec.dequeueInputBuffer(10_000)
                    if(i>=0) {
                        val input=checkNotNull(codec.getInputBuffer(i)); input.clear()
                        if(frame<60) {
                            val yuv=ByteArray(width*height*3/2) { 128.toByte() }
                            for(y in 0 until height) for(x in 0 until width) {
                                yuv[y*width+x]=(if(x<32 && y<32) 235 else 35+(x/3+y/2+frame*5)%140).toByte()
                            }
                            input.put(yuv); codec.queueInputBuffer(i,0,yuv.size,frame*1_000_000L/fps,0); frame++
                        } else {
                            codec.queueInputBuffer(i,0,0,frame*1_000_000L/fps,MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sentEos=true
                        }
                    }
                }
                val o=codec.dequeueOutputBuffer(info,10_000)
                if(o==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    track=muxer.addTrack(codec.outputFormat); muxer.start(); muxing=true
                } else if(o>=0) {
                    val output=checkNotNull(codec.getOutputBuffer(o))
                    if(info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG==0 && info.size>0) {
                        check(muxing); output.position(info.offset); output.limit(info.offset+info.size)
                        muxer.writeSampleData(track,output,info)
                    }
                    done=info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0
                    codec.releaseOutputBuffer(o,false)
                }
            }
        } finally {
            if(configured) runCatching { codec.stop() }; codec.release()
            if(muxing) runCatching { muxer.stop() }; muxer.release()
        }
    }
}
