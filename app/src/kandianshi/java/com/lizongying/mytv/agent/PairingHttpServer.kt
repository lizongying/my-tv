package com.lizongying.mytv.agent

import fi.iki.elonen.NanoHTTPD
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean

class PairingHttpServer(
    val address: String,
    private val prefix: Int,
    private val html: String,
    private val readConfig: () -> String,
    private val saveConfig: (String) -> Unit,
    private val testConfig: (String) -> String,
    private val saved: () -> Unit,
    private val assets: Map<String, Pair<String, String>> = emptyMap(),
    private val clock: () -> Long = { System.nanoTime() / 1_000_000 },
    port: Int = 0
) : NanoHTTPD(address, port) {
    val token: String = ByteArray(32).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
    private val created = clock()
    private val consumed = AtomicBoolean(false)
    private val handlingSave = AtomicBoolean(false)
    private val attempts = mutableMapOf<String, Int>()
    val expired: Boolean get() = clock() - created >= LIFETIME_MS || consumed.get()
    val url: String get() = "http://" + address + ":" + listeningPort + "/#" + token

    init { require(NetworkPolicy.privateIpv4(address) != null && prefix in 8..30) }

    override fun serve(session: IHTTPSession): Response {
        val remote = session.remoteIpAddress.orEmpty().removePrefix("/")
        if (!NetworkPolicy.sameLan(address, prefix, remote)) return reply(Response.Status.FORBIDDEN, "仅允许同一局域网访问")
        if (expired) return reply(Response.Status.GONE, "配对已结束，请在电视上重新开启")
        val authority = address + ":" + listeningPort
        if (session.headers["host"] != authority) return reply(Response.Status.FORBIDDEN, "访问地址无效")
        val origin = "http://" + authority
        val suppliedOrigin = session.headers["origin"]
        if (suppliedOrigin != null && suppliedOrigin != origin) return reply(Response.Status.FORBIDDEN, "拒绝跨站请求")
        if (session.headers.keys.any { it in setOf("forwarded", "x-forwarded-for", "x-forwarded-host") })
            return reply(Response.Status.FORBIDDEN, "拒绝代理转发请求")
        if (session.uri == "/" && session.method == Method.GET) return secure(newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html))
        if (session.method == Method.GET && session.uri in assets) {
            val asset = assets.getValue(session.uri)
            return secure(newFixedLengthResponse(Response.Status.OK, asset.first, asset.second))
        }
        val supplied = session.headers["authorization"].orEmpty().removePrefix("Bearer ")
        if (!NetworkPolicy.tokenMatches(token, supplied)) {
            synchronized(attempts) { attempts[remote] = (attempts[remote] ?: 0) + 1 }
            return reply(Response.Status.UNAUTHORIZED, "配对令牌无效，请重新扫描电视二维码")
        }
        if (synchronized(attempts) { (attempts[remote] ?: 0) >= 10 }) return reply(Response.Status.FORBIDDEN, "配对失败次数过多，请重新开启")
        if (session.uri == "/api/config" && session.method == Method.GET) return try {
            secure(newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", readConfig()))
        } catch (_: Exception) { reply(Response.Status.INTERNAL_ERROR, "读取配置失败") }
        if (session.method != Method.POST || session.uri !in setOf("/api/config", "/api/test")) return reply(Response.Status.NOT_FOUND, "未找到页面")
        if (suppliedOrigin != origin) return reply(Response.Status.FORBIDDEN, "保存操作需要同源浏览器请求")
        if (!session.headers["content-type"].orEmpty().startsWith("application/json")) return reply(Response.Status.UNSUPPORTED_MEDIA_TYPE, "仅接受 JSON 配置")
        val length = session.headers["content-length"]?.toLongOrNull()
        if (length == null || length !in 1..32768 || session.headers.containsKey("transfer-encoding")) return reply(Response.Status.BAD_REQUEST, "请求大小无效")
        if (!handlingSave.compareAndSet(false, true)) return reply(Response.Status.CONFLICT, "正在处理，请稍后重试")
        try {
            if (expired) return reply(Response.Status.GONE, "配对已结束")
            // Bounded in-memory reads: NanoHTTPD.parseBody may spill larger JSON (including a key) to a temporary file.
            val bytes = ByteArray(length.toInt()); var offset = 0
            while (offset < bytes.size) {
                val n = session.inputStream.read(bytes, offset, bytes.size - offset)
                require(n > 0) { "请求内容不完整" }; offset += n
            }
            val body = Charsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString()
            if (session.uri == "/api/test") return reply(Response.Status.OK, testConfig(body))
            saveConfig(body)
            consumed.set(true)
            val result = reply(Response.Status.OK, "已保存到电视，配置网页即将关闭。返回电视即可开始维护。")
            saved()
            return result
        } catch (error: IllegalArgumentException) {
            // Only validation messages are public. API response bodies and keys never enter this path.
            return reply(Response.Status.BAD_REQUEST, error.message?.take(180) ?: "配置无效")
        } catch (_: Exception) { return reply(Response.Status.INTERNAL_ERROR, "操作失败，请检查网络或配置后重试") }
        finally { handlingSave.set(false) }
    }

    private fun reply(status: Response.Status, message: String): Response =
        secure(newFixedLengthResponse(status, "text/plain; charset=utf-8", message))

    private fun secure(response: Response): Response = response.apply {
        addHeader("Cache-Control", "no-store, max-age=0")
        addHeader("Pragma", "no-cache")
        addHeader("Referrer-Policy", "no-referrer")
        addHeader("X-Content-Type-Options", "nosniff")
        addHeader("X-Frame-Options", "DENY")
        addHeader("Content-Security-Policy", "default-src 'none'; style-src 'self'; script-src 'self'; connect-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'")
        addHeader("Connection", "close")
    }

    companion object { const val LIFETIME_MS = 10 * 60 * 1000L }
}
