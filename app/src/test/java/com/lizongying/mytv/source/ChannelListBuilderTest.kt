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
        assertEquals(2, rows["央视"]?.size)
        assertEquals(1, rows["卫视"]?.size)
        assertEquals(listOf(0, 1, 2), rows.values.flatten().map { it.id })
        assertEquals("CCTV-1", rows["央视"]?.get(0)?.title)
        assertEquals(listOf("http://a/1"), rows["央视"]?.get(0)?.videoUrl)
        assertEquals("源A", rows["央视"]?.get(0)?.sourceName)
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
