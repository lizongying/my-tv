package com.lizongying.mytv.agent

import java.net.Inet4Address
import java.net.InetAddress
import java.net.URI
import java.security.MessageDigest
import java.util.Locale

object NetworkPolicy {
    fun httpUrl(value: String): URI {
        require(value.length in 1..2048) { "地址过长或为空" }
        val uri = URI(value.trim())
        require(uri.scheme?.lowercase(Locale.ROOT) in setOf("http", "https") &&
            !uri.host.isNullOrBlank() && uri.userInfo == null && uri.fragment == null) { "请输入 HTTP/HTTPS 地址，不含账号或片段" }
        require(uri.port == -1 || uri.port in 1..65535) { "端口无效" }
        return uri
    }

    fun publicAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress) return false
        val b = address.address.map { it.toInt() and 255 }
        if (b.size == 4) {
            return b[0] !in setOf(0, 127) && b[0] < 224 &&
                !(b[0] == 100 && b[1] in 64..127) &&
                !(b[0] == 198 && b[1] in 18..19) && !(b[0] == 192 && b[1] == 0 && b[2] == 0)
        }
        return b.size == 16 && (b[0] and 0xfe) != 0xfc && b.take(12) != List(10) { 0 } + listOf(255, 255)
    }

    fun publicUrl(value: String, resolve: Boolean = true): URI {
        val uri = httpUrl(value)
        val host = uri.host.removePrefix("[").removeSuffix("]")
        require(host.lowercase(Locale.ROOT) != "localhost" && !host.endsWith(".local", true)) { "不访问本地地址" }
        if (host.contains(':') || host.matches(Regex("[0-9.]+"))) require(publicAddress(InetAddress.getByName(host))) { "来源必须使用公网地址" }
        if (resolve) require(InetAddress.getAllByName(host).all(::publicAddress)) { "来源必须使用公网地址" }
        return uri
    }

    fun privateIpv4(value: String): ByteArray? {
        val chunks = value.split('.')
        if (chunks.size != 4 || chunks.any { it.isEmpty() || it.any { c -> c !in '0'..'9' } || (it.length > 1 && it[0] == '0') }) return null
        val octets = chunks.map { it.toIntOrNull() ?: return null }
        if (octets.any { it !in 0..255 }) return null
        if (!(octets[0] == 10 || octets[0] == 192 && octets[1] == 168 || octets[0] == 172 && octets[1] in 16..31)) return null
        return octets.map(Int::toByte).toByteArray()
    }

    fun sameLan(local: String, prefix: Int, remote: String): Boolean {
        val a = privateIpv4(local) ?: return false
        val b = privateIpv4(remote) ?: return false
        if (prefix !in 8..30) return false
        for (bit in 0 until prefix) {
            val mask = 1 shl (7 - bit % 8)
            if ((a[bit / 8].toInt() and mask) != (b[bit / 8].toInt() and mask)) return false
        }
        return true
    }

    fun tokenMatches(expected: String, supplied: String): Boolean =
        MessageDigest.isEqual(expected.toByteArray(Charsets.UTF_8), supplied.toByteArray(Charsets.UTF_8))

    fun displayUrl(value: String): String = runCatching {
        val u = URI(value)
        URI(u.scheme, null, u.host, u.port, u.path, null, null).toString()
    }.getOrDefault("无效地址")
}
