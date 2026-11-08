package com.lizongying.mytv.agent

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import org.json.JSONObject

/** One explicitly supplied free channel. No resource enumeration or rights-denial fallback. */
object CwjdProtocol {
    const val ROUTE="https://saleservice.5gtv.com.cn/v1/resourceProductRightsAuth?resId=30167"
    fun matches(url: String)=url==ROUTE
    fun sign(path: String,seconds: Long,nonce: String): String {
        require(seconds>0 && nonce.length in 4..7 && nonce.all { it.isLetterOrDigit() && it.code<128 })
        val md5=MessageDigest.getInstance("MD5").digest("$path-$seconds-$nonce-01234ibcp9".toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }
        return "$seconds-$nonce-$md5"
    }
    fun success(text: String): String {
        val j=JSONObject(text);require(j.optString("code")=="0") { "频道接口未授权或暂不可用" }
        return j.getJSONObject("data").getString("url").also { NetworkPolicy.publicUrl(it,false) }
    }
    fun dispatcher(url: String): String {
        val u=NetworkPolicy.publicUrl(url,false)
        require(u.scheme=="https" && u.host=="live-dispatcher.5gtv.com.cn" && u.port in setOf(-1,443))
        return url+(if(u.rawQuery==null) "?" else "&")+"t=1&v=204"
    }
}

/** Short-lived URLs stay in memory. Aliases let a long-running HLS session renew its manifest. */
class CwjdCache(private val load: ()->String,private val clock: ()->Long={System.currentTimeMillis()}) {
    private var current="";@Volatile private var expires=0L;private var retry=0L
    private val aliases=linkedSetOf<String>()
    private val known=java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String,Boolean>())
    fun resolve(url: String): String {
        if(!CwjdProtocol.matches(url) && url !in known) return url
        return resolveKnown()
    }
    @Synchronized private fun resolveKnown(): String {
        val now=clock()
        if(current.isNotEmpty() && now<expires) return current
        check(now>=retry) { "频道解析暂缓重试" }
        try {
            val next=load();NetworkPolicy.publicUrl(next,false)
            aliases.add(next);known.add(next)
            while(aliases.size>64) {val old=aliases.drop(1).first();aliases.remove(old);known.remove(old)}
            current=next;expires=clock()+300_000;retry=0
            return next
        } catch(e: Exception) {retry=clock()+15_000;throw e}
    }
    fun invalidate() { expires=0 }
}
object CwjdResolver {
    private val random=SecureRandom()
    private val device=java.util.UUID.randomUUID().toString().replace("-","")
    private val cache=CwjdCache(::load)
    fun resolve(url: String)=cache.resolve(url)
    fun invalidate()=cache.invalidate()
    private fun request(url: String,payload: String?=null): String {
        val u=NetworkPolicy.publicUrl(url)
        require(u.scheme=="https" && u.host in setOf("saleservice.5gtv.com.cn","live-dispatcher.5gtv.com.cn"))
        val connection=URL(url).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects=false;connection.connectTimeout=4500;connection.readTimeout=4500
        val charset="0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val nonce=(1..6).map { charset[random.nextInt(charset.length)] }.joinToString("")
        val path=u.rawPath+(u.rawQuery?.let { "?$it" } ?: "")
        val headers=mapOf("channelId" to "cbn","deviceType" to "2048","releaseVersion" to "2.0.4","releaseVersionCode" to "204","os" to "Android","deviceId" to device,"API-VERSION" to "3","User-Agent" to "okhttp/3.14.9","Accept" to "*/*","sign" to CwjdProtocol.sign(path,System.currentTimeMillis()/1000,nonce))
        headers.forEach { (k,v)->connection.setRequestProperty(k,v) }
        try {
            if(payload!=null) {connection.requestMethod="POST";connection.doOutput=true;connection.setRequestProperty("Content-Type","application/json");connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }}
            check(connection.responseCode==200) { "频道接口请求失败" }
            return connection.inputStream.use { input ->
                val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(4096);val deadline=System.nanoTime()+8_000_000_000L
                while(true) {check(System.nanoTime()<deadline);val n=input.read(buffer);if(n<0) break;check(out.size()+n<=64*1024);out.write(buffer,0,n)}
                out.toByteArray().toString(Charsets.UTF_8)
            }
        } finally {connection.disconnect()}
    }
    private fun load(): String {
        val first=CwjdProtocol.success(request("https://saleservice.5gtv.com.cn/v1/resourceProductRightsAuth","{\"resId\":\"30167\",\"resourceStreamId\":\"30167\"}"))
        return CwjdProtocol.success(request(CwjdProtocol.dispatcher(first)))
    }
    fun offer(c: android.content.Context) = CandidateAdmission.offer(c,listOf(com.lizongying.mytv.PlaylistChannel("重温经典","地方台","",listOf(CwjdProtocol.ROUTE))))
}
