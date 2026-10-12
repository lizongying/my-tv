package com.lizongying.mytv.agent

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AtomicFile
import com.lizongying.mytv.ChannelLogo
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

object RealLogoStore {
    private val lock=Any()
    private val writer=Executors.newSingleThreadExecutor()
    private var state: JSONObject?=null
    private var index: JSONObject?=null
    private fun state(c: Context)=state ?: runCatching { JSONObject(AgentRepository.read(c,"channel-logos.json") ?: "{}") }
        .getOrDefault(JSONObject()).also { state=it }
    private fun index(c: Context)=index ?: c.assets.open("real-logo-index.json").bufferedReader().use { JSONObject(it.readText()) }.also { index=it }
    private fun entries(c: Context)=state(c).optJSONObject("entries") ?: JSONObject().also { state(c).put("entries",it) }
    private fun failed(c: Context)=state(c).optJSONObject("failed") ?: JSONObject().also { state(c).put("failed",it) }
    private fun dir(c: Context)=File(c.filesDir,"channel-logos").apply { mkdirs() }
    private fun file(c: Context,title: String)=File(dir(c),MemoryPolicy.id(RealLogoPolicy.key(title))+".png")
    private fun save(c: Context) { AgentRepository.write(c,"channel-logos.json",RealLogoPolicy.bound(state(c)).toString()) }
    fun bitmap(c: Context,title: String): Bitmap? = synchronized(lock) {
        val f=file(c,title)
        if(f.exists()) runCatching { BitmapFactory.decodeFile(f.absolutePath) }.getOrNull()?.let { return@synchronized it }
        val asset=index(c).optJSONObject(RealLogoPolicy.key(title))?.optString("asset").orEmpty()
        if(asset.isEmpty() || !Regex("logos/[a-f0-9]{24}\\.png").matches(asset)) return@synchronized null
        runCatching { c.assets.open(asset).use { BitmapFactory.decodeStream(it) } }.getOrNull()
    }
    fun urls(c: Context,title: String,original: String?): List<String> = synchronized(lock) { RealLogoPolicy.candidates(title,original,index(c)) }
    fun isFailed(c: Context,url: String,now: Long=System.currentTimeMillis()): Boolean = synchronized(lock) { !RealLogoPolicy.usable(url,failed(c),now) }
    fun enqueue(c: Context,channels: List<Pair<String,String?>>)=synchronized(lock) {
        val es=entries(c);var changed=false
        channels.take(500).forEach { (title, original) ->
            val key=RealLogoPolicy.key(title)
            val e=es.optJSONObject(key) ?: JSONObject().put("title",title.take(80)).put("at",System.currentTimeMillis()).apply {
                if(index(c).optJSONObject(key)?.optString("asset").orEmpty().isNotBlank()) put("saved",System.currentTimeMillis())
            }.also { es.put(key,it);changed=true }
            val old=e.optJSONArray("urls") ?: JSONArray()
            val next=JSONArray((urls(c,title,original)+(0 until old.length()).map { old.optString(it) }).distinct().take(4))
            if(old.toString()!=next.toString()) { e.put("urls",next);changed=true }
        }
        if(changed) save(c)
    }
    fun note(c: Context,title: String,original: String?=null,failedUrl: String?=null) {
        val app=c.applicationContext
        writer.execute { runCatching {
            synchronized(lock) {
                enqueue(app,listOf(title to original))
                if(failedUrl!=null) { failed(app).put(MemoryPolicy.id(failedUrl),System.currentTimeMillis());save(app) }
            }
            LogoScheduler.nudge(app)
        } }
    }
    fun pending(c: Context,now: Long=System.currentTimeMillis()): List<Pair<String,List<String>>> = synchronized(lock) {
        val es=entries(c)
        es.keys().asSequence().map { es.getJSONObject(it) }.filter {
            val title=it.optString("title")
            RealLogoPolicy.ready(it,file(c,title).exists() || index(c).optJSONObject(RealLogoPolicy.key(title))?.optString("asset").orEmpty().isNotBlank(),now)
        }.sortedBy { it.optLong("attempt",0) }.map { e ->
            val title=e.optString("title"); val a=e.optJSONArray("urls") ?: JSONArray()
            title to (0 until a.length()).map { a.optString(it) }.filter { RealLogoPolicy.usable(it,failed(c),now) }
        }.filter { it.second.isNotEmpty() }.take(RealLogoPolicy.BATCH).toList()
    }
    fun attempt(c: Context,title: String,now: Long=System.currentTimeMillis())=synchronized(lock) {
        entries(c).optJSONObject(RealLogoPolicy.key(title))?.put("attempt",now);save(c)
    }
    fun failure(c: Context,url: String)=synchronized(lock) { failed(c).put(MemoryPolicy.id(url),System.currentTimeMillis());save(c) }
    fun put(c: Context,title: String,url: String,bytes: ByteArray): Boolean {
        val bitmap=decode(bytes) ?: return false
        try {
            synchronized(lock) {
                val target=AtomicFile(file(c,title));val out=target.startWrite()
                try { check(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));target.finishWrite(out) }
                catch(e: Exception) { target.failWrite(out);throw e }
                val key=RealLogoPolicy.key(title);val es=entries(c)
                val e=es.optJSONObject(key) ?: JSONObject().put("title",title.take(80)).also { es.put(key,it) }
                e.put("url",url).put("saved",System.currentTimeMillis()).put("at",System.currentTimeMillis())
                failed(c).remove(MemoryPolicy.id(url));trimLocked(c);save(c)
            }
            AgentRepository.memoryChanged(c)
            return true
        } finally { bitmap.recycle() }
    }
    fun decode(bytes: ByteArray): Bitmap? {
        if(bytes.size>256*1024) return null
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
        if(bounds.outWidth !in 8..2048 || bounds.outHeight !in 8..2048) return null
        var sample=1;while(bounds.outWidth/sample>600 || bounds.outHeight/sample>300) sample*=2
        val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply { inSampleSize=sample }) ?: return null
        if(!ChannelLogo.useful(bitmap)) { bitmap.recycle();return null }
        return bitmap
    }
    private fun trimLocked(c: Context) {
        val files=dir(c).listFiles()?.filter { it.name.endsWith(".png") }?.sortedBy { it.lastModified() }.orEmpty()
        var bytes=files.sumOf { it.length() };val cap=RealLogoPolicy.limitMb(AgentConfigStore(c).load().logoImageMb)*1024*1024L
        for(f in files) { if(bytes<=cap) break;val size=f.length();if(f.delete()) bytes-=size }
    }
    fun applyLimit(c: Context) { val app=c.applicationContext;writer.execute { runCatching { synchronized(lock) { trimLocked(app);save(app) } } } }
    fun summary(c: Context): String = synchronized(lock) {
        val bundled=index(c).keys().asSequence().mapNotNull { index(c).optJSONObject(it)?.optString("asset")?.takeIf(String::isNotBlank) }.distinct().count()
        "真实台标：内置 $bundled 个，缓存 ${dir(c).listFiles()?.count { it.name.endsWith(".png") } ?: 0} 个；图片缓存上限 ${AgentConfigStore(c).load().logoImageMb} MB"
    }
}
