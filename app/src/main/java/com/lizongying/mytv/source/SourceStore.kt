package com.lizongying.mytv.source

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lizongying.mytv.SP
import java.util.UUID

object SourceStore {

    private val gson = Gson()
    private val listType = object : TypeToken<MutableList<Source>>() {}.type

    fun list(): MutableList<Source> {
        val parsed = try {
            gson.fromJson<MutableList<Source>>(SP.sources, listType)
        } catch (e: Exception) {
            null
        }
        return parsed?.filterNotNull()?.filter { it.url.isNotBlank() }?.toMutableList()
            ?: mutableListOf()
    }

    fun enabled(): List<Source> = list().filter { it.enabled }

    fun add(name: String, url: String): Result<Source> {
        val address = url.trim()
        if (!isPlayable(address)) {
            return Result.failure(IllegalArgumentException("地址需以 http:// 或 https:// 开头"))
        }
        val sources = list()
        if (sources.any { it.url == address }) {
            return Result.failure(IllegalArgumentException("该地址已存在"))
        }
        val source = Source(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { hostOf(address) },
            url = address,
            enabled = true,
        )
        sources.add(source)
        save(sources)
        return Result.success(source)
    }

    fun update(source: Source): Result<Unit> {
        val address = source.url.trim()
        if (!isPlayable(address)) {
            return Result.failure(IllegalArgumentException("地址需以 http:// 或 https:// 开头"))
        }
        val sources = list()
        if (sources.any { it.id != source.id && it.url == address }) {
            return Result.failure(IllegalArgumentException("该地址已存在"))
        }
        val index = sources.indexOfFirst { it.id == source.id }
        if (index < 0) {
            return Result.failure(IllegalArgumentException("源不存在"))
        }
        source.url = address
        source.name = source.name.trim().ifEmpty { hostOf(address) }
        sources[index] = source
        save(sources)
        return Result.success(Unit)
    }

    fun remove(id: String) {
        save(list().filter { it.id != id })
    }

    fun setEnabled(id: String, enabled: Boolean) {
        val sources = list()
        sources.firstOrNull { it.id == id }?.enabled = enabled
        save(sources)
    }

    fun clear() {
        save(emptyList())
    }

    private fun save(sources: List<Source>) {
        SP.sources = gson.toJson(sources)
    }

    private fun isPlayable(url: String) =
        url.startsWith("http://") || url.startsWith("https://")

    private fun hostOf(url: String): String {
        return try {
            val host = java.net.URI(url).host
            host?.removePrefix("www.").orEmpty()
        } catch (e: Exception) {
            url
        }
    }
}
