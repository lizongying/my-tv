package com.lizongying.mytv

import com.lizongying.mytv.agent.NetworkPolicy
import com.lizongying.mytv.agent.PairingHttpServer
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.net.Socket
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

class PairingServerTest {
    private fun fixture(test: (PairingHttpServer, AtomicInteger, (Long) -> Unit) -> Unit) {
        val network = Collections.list(NetworkInterface.getNetworkInterfaces()).filter { it.isUp && !it.isLoopback }
            .flatMap { it.interfaceAddresses }.firstOrNull { it.address is Inet4Address &&
                NetworkPolicy.privateIpv4(it.address.hostAddress.orEmpty()) != null && it.networkPrefixLength.toInt() in 8..30 }
        assumeTrue("需要本机私有 IPv4 网卡验证 LAN HTTP 服务", network != null)
        val saved = AtomicInteger(); var now = 0L
        val server = PairingHttpServer(network!!.address.hostAddress, network.networkPrefixLength.toInt(), "<!doctype html><title>看电视</title>",
            { "{\"hasKey\":true}" }, { body -> require(!body.contains("invalid")) { "配置格式无效" } }, { "API OK" },
            { saved.incrementAndGet() }, clock = { now })
        server.start(1000, true)
        try { test(server, saved) { now = it } } finally { server.stop() }
    }
    private fun call(s: PairingHttpServer, path: String, token: String? = null, origin: String? = null, body: String? = null,
        host: String = s.address + ":" + s.listeningPort, extraHeader: String = ""): Pair<Int, String> {
        Socket(s.address, s.listeningPort).use { socket ->
            socket.soTimeout = 3000
            val bytes = body?.toByteArray(Charsets.UTF_8)
            val headers = buildString {
                append(if (body == null) "GET " else "POST ").append(path).append(" HTTP/1.1\r\n")
                append("Host: ").append(host).append("\r\nConnection: close\r\n")
                if (extraHeader.isNotBlank()) append(extraHeader).append("\r\n")
                if (token != null) append("Authorization: Bearer ").append(token).append("\r\n")
                if (origin != null) append("Origin: ").append(origin).append("\r\n")
                if (bytes != null) append("Content-Type: application/json\r\nContent-Length: ").append(bytes.size).append("\r\n")
                append("\r\n")
            }
            socket.getOutputStream().write(headers.toByteArray(Charsets.UTF_8))
            if (bytes != null && bytes.size <= 32768) socket.getOutputStream().write(bytes)
            socket.getOutputStream().flush()
            val reply = socket.getInputStream().readBytes().toString(Charsets.UTF_8)
            return reply.substringBefore("\r\n").split(' ')[1].toInt() to reply.substringAfter("\r\n\r\n")
        }
    }
    @Test fun privateInterfaceIsRequired() {
        assertTrue(runCatching { PairingHttpServer("0.0.0.0", 24, "", { "" }, {}, { "" }, {}) }.isFailure)
        assertTrue(runCatching { PairingHttpServer("8.8.8.8", 24, "", { "" }, {}, { "" }, {}) }.isFailure)
    }
    @Test fun configRequiresTokenAndNeverReturnsTheSecret() = fixture { s, _, _ ->
        assertTrue(s.token.matches(Regex("[a-f0-9]{64}")))
        assertEquals(200, call(s, "/").first)
        assertEquals(401, call(s, "/api/config").first)
        val response = call(s, "/api/config", s.token)
        assertEquals(200, response.first); assertEquals("{\"hasKey\":true}", response.second)
        assertEquals(401, call(s, "/api/config", "wrong").first)
    }
    @Test fun crossOriginAndMissingOriginCannotSave() = fixture { s, saved, _ ->
        assertEquals(403, call(s, "/api/config", s.token, "http://evil.example", "{}").first)
        assertEquals(403, call(s, "/api/config", s.token, body = "{}").first)
        assertEquals(0, saved.get())
    }
    @Test fun successfulSaveConsumesTokenAndCannotBeReplayed() = fixture { s, saved, _ ->
        val origin = s.url.substringBefore('#').removeSuffix("/")
        assertEquals(200, call(s, "/api/config", s.token, origin, "{}").first)
        assertEquals(1, saved.get())
        assertEquals(410, call(s, "/api/config", s.token).first)
        assertEquals(410, call(s, "/api/config", s.token, origin, "{}").first)
        assertEquals(1, saved.get())
    }
    @Test fun invalidConfigAndConnectionTestDoNotConsumeToken() = fixture { s, saved, _ ->
        val origin = s.url.substringBefore('#').removeSuffix("/")
        assertEquals(400, call(s, "/api/config", s.token, origin, "{\"invalid\":true}").first)
        assertEquals(200, call(s, "/api/test", s.token, origin, "{}").first)
        assertEquals(200, call(s, "/api/config", s.token).first)
        assertEquals(0, saved.get())
    }
    @Test fun expirationClosesAccessWithoutSaving() = fixture { s, saved, advance ->
        advance(PairingHttpServer.LIFETIME_MS)
        assertEquals(410, call(s, "/").first)
        assertEquals(410, call(s, "/api/config", s.token).first)
        assertEquals(0, saved.get())
    }
    @Test fun oversizedRequestsAreRejectedBeforeParsing() = fixture { s, saved, _ ->
        val origin = s.url.substringBefore('#').removeSuffix("/")
        assertEquals(400, call(s, "/api/config", s.token, origin, "a".repeat(32769)).first)
        assertEquals(0, saved.get())
    }
    @Test fun largerJsonIsReadInMemoryAndUnicodeIsPreserved() = fixture { s, saved, _ ->
        val origin = s.url.substringBefore('#').removeSuffix("/")
        assertEquals(200, call(s, "/api/config", s.token, origin, "{\"key\":\"" + "仅测试".repeat(400) + "\"}").first)
        assertEquals(1, saved.get())
    }
    @Test fun spoofedHostAndProxyForwardingAreRejected() = fixture { s, _, _ ->
        assertEquals(403, call(s, "/", host = "evil.example").first)
        assertEquals(403, call(s, "/api/config", s.token, extraHeader = "X-Forwarded-For: 8.8.8.8").first)
    }
}
