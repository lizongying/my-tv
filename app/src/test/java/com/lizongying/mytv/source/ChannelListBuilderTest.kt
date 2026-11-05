package com.lizongying.mytv.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelListBuilderTest {

    private fun source(id: String, name: String) = Source(id, name, "http://$id", true)

    @Test
    fun merge() {
        val first = source("a", "源A")
        val second = source("b", "源B")
        val channels = mapOf(
            "a" to listOf(
                M3uChannel("CCTV-1", "http://a/1", "央视", ""),
                M3uChannel("湖南卫视", "http://a/2", "卫视", ""),
            ),
            "b" to listOf(
                M3uChannel("CCTV-2", "http://b/1", "央视", ""),
            ),
        )

        val rows = ChannelListBuilder.build(listOf(first, second), channels)

        assertEquals(listOf("央视", "卫视"), rows.keys.toList())
        assertEquals(listOf("CCTV-1", "CCTV-2"), rows["央视"]?.map { it.title })
        assertEquals(listOf("湖南卫视"), rows["卫视"]?.map { it.title })
        assertEquals(listOf(0, 1), rows["央视"]?.map { it.id })
        assertEquals(listOf(2), rows["卫视"]?.map { it.id })
        assertEquals(listOf("http://a/1", "http://b/1"), rows["央视"]?.map { it.videoUrl.first() })
        assertEquals(listOf("源A", "源B"), rows["央视"]?.map { it.sourceName })
    }

    @Test
    fun singleSourceHasNoSourceName() {
        val only = source("a", "源A")
        val channels = mapOf("a" to listOf(M3uChannel("CCTV-1", "http://a/1", "央视", "")))

        val rows = ChannelListBuilder.build(listOf(only), channels)

        assertEquals("", rows["央视"]?.get(0)?.sourceName)
    }

    @Test
    fun emptyGroupFallsBack() {
        val only = source("a", "源A")
        val channels = mapOf("a" to listOf(M3uChannel("频道A", "http://a/1", "", "")))

        val rows = ChannelListBuilder.build(listOf(only), channels)

        assertEquals(listOf(M3uParser.FALLBACK_GROUP), rows.keys.toList())
    }

    @Test
    fun missingSourceChannelsIgnored() {
        val only = source("a", "源A")

        val rows = ChannelListBuilder.build(listOf(only), emptyMap())

        assertTrue(rows.isEmpty())
        assertTrue(ChannelListBuilder.signature(rows).isEmpty())
    }
}
