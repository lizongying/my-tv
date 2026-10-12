package com.lizongying.mytv
import org.junit.Assert.*
import org.junit.Test

class PlaylistParserTest {
    @Test fun mergesCctvVariantsWithoutMixingUhdOrPlus() {
        val input = """#EXTM3U
#EXTINF:-1 group-title="央视",CCTV-1 综合 (1080p)
https://a.example/cctv1.m3u8
#EXTINF:-1 group-title="央视",CCTV1
https://b.example/cctv1.m3u8
#EXTINF:-1 group-title="央视",CCTV-5+
https://b.example/cctv5plus.m3u8
#EXTINF:-1 group-title="央视",CCTV-4K HD
https://b.example/cctv4k.m3u8
"""
        val channels = PlaylistParser.parse(input)
        assertEquals(3, channels.size)
        assertEquals(listOf("https://a.example/cctv1.m3u8","https://b.example/cctv1.m3u8"), channels.first().urls)
        assertTrue(channels.any { it.title == "CCTV5+" })
        assertTrue(channels.any { it.title == "CCTV4K" })
    }
    @Test fun chineseNamesClassifySatelliteAndLocalChannels() {
        val input = """#EXTM3U
#EXTINF:-1 tvg-id="ZhejiangSatelliteTV.cn@HD" group-title="General",Zhejiang TV
https://a.example/zj.m3u8
#EXTINF:-1 tvg-id="Anshun.cn@SD" group-title="News",Anshun TV
https://a.example/as.m3u8
"""
        val channels = PlaylistParser.parse(input, mapOf("ZhejiangSatelliteTV.cn" to "浙江卫视","Anshun.cn" to "安顺新闻综合"))
        assertEquals("卫视", channels[0].group)
        assertEquals("地方台", channels[1].group)
    }
    @Test fun rejectsPromoForeignPrivateAndMulticastEntries() {
        val input = """#EXTM3U
#EXTINF:-1 group-title="央视",支持作者
https://a.example/promo.mp4
#EXTINF:-1 group-title="央视",CCTV1
rtp://239.3.1.129:8008
#EXTINF:-1 tvg-id="Foreign.us" group-title="News",Foreign TV
https://a.example/foreign.m3u8
#EXTINF:-1 group-title="地方频道",测试
http://127.0.0.1/a.m3u8
#EXTINF:-1 group-title="地方频道",地方台
http://192.168.1.2/a.m3u8
"""
        assertTrue(PlaylistParser.parse(input).isEmpty())
    }
    @Test fun supportsBomCrlfCommentsAndUrlSuffixes() {
        val input = "\uFEFF#EXTM3U\r\n#EXTINF:-1 group-title=\"地方频道\",地方新闻\r\n#EXTVLCOPT:http-user-agent=ignored\r\nhttps://a.example/a.m3u8?x=1&amp;y=2" + "$" + "高清\r\n"
        val channel = PlaylistParser.parse(input).single()
        assertEquals("https://a.example/a.m3u8?x=1&y=2",channel.urls.single())
    }
    @Test fun emptyHtmlIsNotAPlaylist() {
        assertTrue(PlaylistParser.parse("<html>Service unavailable</html>").isEmpty())
    }
    @Test fun deduplicatesUrlsAndSortsChannelNumbersNaturally() {
        val input = """#EXTM3U
#EXTINF:-1 group-title="央视",CCTV10
https://a.example/10.m3u8
#EXTINF:-1 group-title="央视",CCTV2
https://a.example/2.m3u8
#EXTINF:-1 group-title="央视",CCTV-2 高清
https://a.example/2.m3u8
"""
        val channels = PlaylistParser.parse(input)
        assertEquals(listOf("CCTV2","CCTV10"),channels.map { it.title })
        assertEquals(1,channels.first().urls.size)
    }

    @Test fun bundledListsKeepFullCctvAndLocalCoverage() {
        val assets = java.io.File(System.getProperty("kandianshi.assets"))
        val json = java.io.File(assets, "channel_names.json").readText()
        val names = Regex(""""([^"]+)":\s*"([^"]*)"""").findAll(json)
            .associate { it.groupValues[1] to it.groupValues[2] }
        val channels = PlaylistParser.merge(listOf("iptv-org-cn.m3u", "vbskycn-iptv4.m3u", "yuechan-gntv.m3u")
            .map { PlaylistParser.parse(java.io.File(assets, it).readText(), names) })
        for (number in 1..17) assertTrue("Missing CCTV$number", channels.any { it.title == "CCTV$number" })
        assertTrue(channels.count { it.group == "\u5730\u65b9\u53f0" } > 100)
        assertTrue(channels.count { it.group == "\u536b\u89c6" } > 20)
        assertTrue(channels.all { it.urls.isNotEmpty() && it.urls.all(PlaylistParser::validUrl) })
        java.io.File(System.getProperty("kandianshi.inventory")).apply { parentFile.mkdirs() }.writeText(
            "group\tname\turl_count\n" + channels.joinToString("\n") { it.group + "\t" + it.title + "\t" + it.urls.size })
        println("Bundled channels: " + channels.size + " groups: " + channels.groupingBy { it.group }.eachCount())
    }

    @Test fun keepsDifferentRegionalStationsWithSameDisplayNameSeparate() {
        val input = """#EXTM3U
#EXTINF:-1 tvg-id="\u5185\u8499\u53e4\u7ecf\u6d4e\u751f\u6d3b" group-title="\u5730\u65b9\u9891\u9053",\u7ecf\u6d4e\u751f\u6d3b
https://a.example/a.m3u8
#EXTINF:-1 tvg-id="\u6d59\u6c5f\u7ecf\u6d4e\u751f\u6d3b" group-title="\u5730\u65b9\u9891\u9053",\u7ecf\u6d4e\u751f\u6d3b
https://b.example/b.m3u8
""".replace("\\u5185\\u8499\\u53e4", "\u5185\u8499\u53e4")
            .replace("\\u6d59\\u6c5f", "\u6d59\u6c5f")
            .replace("\\u7ecf\\u6d4e\\u751f\\u6d3b", "\u7ecf\u6d4e\u751f\u6d3b")
            .replace("\\u5730\\u65b9\\u9891\\u9053", "\u5730\u65b9\u9891\u9053")
        val channels = PlaylistParser.parse(input)
        assertEquals(2, channels.size)
        assertTrue(channels.any { it.title == "\u5185\u8499\u53e4\u7ecf\u6d4e\u751f\u6d3b" })
        assertTrue(channels.any { it.title == "\u6d59\u6c5f\u7ecf\u6d4e\u751f\u6d3b" })
    }
}
