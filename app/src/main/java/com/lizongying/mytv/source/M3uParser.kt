package com.lizongying.mytv.source

data class M3uChannel(
    val title: String,
    val url: String,
    val group: String,
    val logo: String,
)

object M3uParser {

    const val FALLBACK_GROUP = "其他"

    fun parse(text: String): List<M3uChannel> {
        val result = mutableListOf<M3uChannel>()
        var title = ""
        var group = ""
        var logo = ""

        for (raw in text.removePrefix("\uFEFF").lineSequence()) {
            val line = raw.trim()
            when {
                line.startsWith("#EXTINF") -> {
                    title = titleOf(line).ifEmpty { attr(line, "tvg-name") }
                    group = attr(line, "group-title")
                    logo = attr(line, "tvg-logo")
                }

                line.isEmpty() || line.startsWith("#") -> Unit

                title.isNotEmpty() && isPlayable(line) -> {
                    result.add(
                        M3uChannel(
                            title = title,
                            url = line,
                            group = group.ifEmpty { FALLBACK_GROUP },
                            logo = logo,
                        )
                    )
                    title = ""
                }
            }
        }
        return result.distinctBy { it.url }
    }

    private fun isPlayable(url: String) =
        url.startsWith("http://") || url.startsWith("https://")

    private fun attr(line: String, name: String): String {
        val flag = "$name=\""
        val start = line.indexOf(flag)
        if (start < 0) {
            return ""
        }
        val from = start + flag.length
        val end = line.indexOf('"', from)
        return if (end > from) line.substring(from, end).trim() else ""
    }

    private fun titleOf(line: String): String {
        var inQuote = false
        for (i in line.indices) {
            when (line[i]) {
                '"' -> inQuote = !inQuote
                ',' -> if (!inQuote) return line.substring(i + 1).trim()
            }
        }
        return ""
    }
}
