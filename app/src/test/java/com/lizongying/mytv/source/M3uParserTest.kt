package com.lizongying.mytv.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {

    @Test
    fun standard() {
        val text = """
            #EXTM3U
            #EXTINF:-1 tvg-logo="http://a/1.png" group-title="央视",CCTV-1 综合
            http://a/1.m3u8
            #EXTINF:-1 group-title="卫视",湖南卫视
            http://a/2.m3u8
        """.trimIndent()

        val list = M3uParser.parse(text)

        assertEquals(2, list.size)
        assertEquals("CCTV-1 综合", list[0].title)
        assertEquals("央视", list[0].group)
        assertEquals("http://a/1.png", list[0].logo)
        assertEquals("http://a/1.m3u8", list[0].url)
        assertEquals("卫视", list[1].group)
    }

    @Test
    fun dirty() {
        val text = "\uFEFF#EXTM3U\r\n" +
                "#EXTINF:-1,频道A\r\n" +
                "http://a/a.m3u8\r\n" +
                "http://naked/b.m3u8\r\n" +
                "#EXTINF:-1 tvg-name=\"频道B\",\r\n" +
                "http://a/b.m3u8"

        val list = M3uParser.parse(text)

        assertEquals(2, list.size)
        assertEquals("频道A", list[0].title)
        assertEquals(M3uParser.FALLBACK_GROUP, list[0].group)
        assertEquals("频道B", list[1].title)
    }

    @Test
    fun empty() {
        assertTrue(M3uParser.parse("").isEmpty())
        assertTrue(M3uParser.parse("#EXTM3U").isEmpty())
    }

    @Test
    fun nonHttp() {
        val text = "#EXTINF:-1,本地频道\nfile:///sdcard/a.m3u8"
        assertTrue(M3uParser.parse(text).isEmpty())
    }

    @Test
    fun duplicated() {
        val text = "#EXTINF:-1,频道A\nhttp://a/a.m3u8\n#EXTINF:-1,频道A\nhttp://a/a.m3u8"
        assertEquals(1, M3uParser.parse(text).size)
    }
}
