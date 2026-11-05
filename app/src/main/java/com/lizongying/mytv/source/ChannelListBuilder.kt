package com.lizongying.mytv.source

import com.lizongying.mytv.TV

object ChannelListBuilder {

    fun build(
        sources: List<Source>,
        channels: Map<String, List<M3uChannel>>,
    ): Map<String, List<TV>> {
        val showSourceName = sources.size > 1
        val grouped = LinkedHashMap<String, MutableList<TV>>()
        for (source in sources) {
            for (channel in channels[source.id].orEmpty()) {
                val group = channel.group.ifEmpty { M3uParser.FALLBACK_GROUP }
                grouped.getOrPut(group) { mutableListOf() }.add(
                    TV(
                        title = channel.title,
                        videoUrl = listOf(channel.url),
                        channel = group,
                        logo = channel.logo,
                        sourceName = if (showSourceName) source.name else "",
                    )
                )
            }
        }
        // id 必须与列表装配顺序一致（按分组行），否则选台与频道号会错位
        var id = 0
        for ((_, list) in grouped) {
            for (tv in list) {
                tv.id = id++
            }
        }
        return grouped
    }

    fun signature(rows: Map<String, List<TV>>): List<String> {
        return rows.flatMap { (group, list) ->
            list.map { "$group\u0000${it.title}\u0000${it.videoUrl.firstOrNull().orEmpty()}" }
        }
    }
}
