package com.lizongying.mytv.api

import okhttp3.Dns
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

class DnsCache : Dns {
    private val dnsCache: MutableMap<String, List<InetAddress>> = ConcurrentHashMap()

    override fun lookup(hostname: String): List<InetAddress> {
        dnsCache[hostname]?.let {
            return it
        }

        val addresses = InetAddress.getAllByName(hostname).toList()

        if (addresses.isNotEmpty()) {
            dnsCache[hostname] = addresses
        }

        return addresses
    }
}