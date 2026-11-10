package com.lizongying.mytv.agent
import java.util.Locale
import com.lizongying.mytv.PlaylistChannel
object IptvOrgPolicy {
    private val marker=Regex("""^国际频道·[^\r\n]{1,60} \[([A-Z]{2})]$""")
    fun code(value: String)=value.trim().uppercase(Locale.ROOT).let { if(it=="GB") "UK" else it }
    fun clean(values: List<String>,supported: Set<String>): List<String> {
        val out=values.map(::code).distinct()
        require(out.size<=5 && out.all { it in supported }) { "IPTV 最多选择 5 个有效国家／地区" }
        return out
    }
    fun country(group: String)=marker.matchEntire(group)?.groupValues?.get(1)?.let(::code)
    fun group(input: String): String {
        val code=code(input)
        require(code.matches(Regex("[A-Z]{2}")))
        return "国际频道·"+Locale("",if(code=="UK") "GB" else code).getDisplayCountry(Locale.SIMPLIFIED_CHINESE)+" [$code]"
    }
    fun url(input: String): String { require(input.matches(Regex("[A-Z]{2}")));return "https://iptv-org.github.io/iptv/countries/"+code(input).lowercase(Locale.ROOT)+".m3u" }
    fun visible(ch: PlaylistChannel,selected: Set<String>,active: String=""): Boolean = country(ch.group)?.let { it in selected || active.isNotBlank() && active in ch.urls } ?: true
}
