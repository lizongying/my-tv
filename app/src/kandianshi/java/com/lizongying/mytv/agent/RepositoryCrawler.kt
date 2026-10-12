package com.lizongying.mytv.agent

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.net.URLDecoder
import java.util.Locale

/** Public repository catalogues are another discovery network; no account or search key required. */
object RepositoryCrawler {
    val entries = listOf(
        CrawlLink("https://jihulab.com/owen2000wy/owentv/-/raw/main/HP20230319.txt", "用户示例公开节目单", "playlist"),
        CrawlLink("https://github.com/YueChan/Live", "公开直播节目单目录"),
        CrawlLink("https://github.com/vbskycn/iptv", "公开 IPTV 节目单目录")
    )
    private const val CURSOR = "_kan_offset"
    private const val CHUNK = 24
    private fun enc(s: String) = URLEncoder.encode(s,"UTF-8").replace("+","%20")
    private fun dec(s: String) = URLDecoder.decode(s,"UTF-8")
    private fun query(u: URI): Map<String,String> = u.rawQuery.orEmpty().split('&').filter { it.contains('=') }.associate { dec(it.substringBefore('=')) to dec(it.substringAfter('=')) }
    private fun parts(u: URI) = u.rawPath.trim('/').split('/').filter { it.isNotBlank() }.map(::dec)
    private fun filePath(path: String): Boolean = path.isNotBlank() && path.length<=512 && !path.startsWith('/') &&
        path.split('/').none { it in setOf(".","..") } && path.none { it.isISOControl() || it=='\\' || it=='?' || it=='#' }
    private fun playlist(path: String) = Regex("(?i)\\.(txt|m3u|m3u8)$").containsMatchIn(path)
    fun isCatalogue(url: String): Boolean = runCatching {
        val u=URI(url)
        (u.host.equals("api.github.com",true) && Regex("^/repos/[^/]+/[^/]+/git/trees/[^/]+$").matches(u.rawPath)) ||
            (u.host.lowercase(Locale.ROOT) in setOf("gitlab.com","jihulab.com") && Regex("^/api/v4/projects/[^/]+/repository/tree$").matches(u.rawPath))
    }.getOrDefault(false)
    /** The local cursor is never sent to the remote API. */
    fun fetchAddress(url: String): String {
        if(!isCatalogue(url)) return url
        val u=URI(url); val q=u.rawQuery.orEmpty().split('&').filterNot { it.substringBefore('=')==CURSOR }.filter { it.isNotEmpty() }.joinToString("&")
        return url.substringBefore('?') + if(q.isEmpty()) "" else "?"+q
    }
    fun continuation(parent: String, child: String) = isCatalogue(parent) && isCatalogue(child) && fetchAddress(parent)==fetchAddress(child)
    fun catalogue(url: String): String? = runCatching {
        if(isCatalogue(url)) return null
        val u=NetworkPolicy.publicUrl(url,false); val p=parts(u)
        val host=u.host.lowercase(Locale.ROOT)
        if(host in setOf("github.com","raw.githubusercontent.com")) {
            require(p.size>=2 && p[0].matches(Regex("[A-Za-z0-9_.-]+")) && p[1].matches(Regex("[A-Za-z0-9_.-]+")))
            val ref=if(host=="raw.githubusercontent.com") p.getOrNull(2) ?: "HEAD" else if(p.getOrNull(2) in setOf("blob","tree")) p.getOrNull(3) ?: "HEAD" else "HEAD"
            "https://api.github.com/repos/${enc(p[0])}/${enc(p[1])}/git/trees/${enc(ref)}?recursive=1"
        } else if(host in setOf("gitlab.com","jihulab.com")) {
            val sep=p.indexOf("-");val project=if(sep>=2) p.take(sep) else p
            require(project.size>=2 && project.size<=5 && project.all { it.matches(Regex("[A-Za-z0-9_.-]+")) })
            val ref=if(sep>=2 && p.getOrNull(sep+1) in setOf("raw","blob","tree")) p.getOrNull(sep+2) ?: "HEAD" else "HEAD"
            "https://$host/api/v4/projects/${enc(project.joinToString("/"))}/repository/tree?ref=${enc(ref)}&per_page=100&page=1"
        } else null
    }.getOrNull()
    fun links(url: String, text: String): List<CrawlLink> = runCatching {
        if(!isCatalogue(url)) return emptyList()
        val u=URI(url);val p=parts(u);val q=query(u);val github=u.host.equals("api.github.com",true)
        val array=if(github) JSONObject(text).getJSONArray("tree") else JSONArray(text)
        val ref=if(github) p.last() else q["ref"] ?: "HEAD"
        val project=if(github) p[1]+"/"+p[2] else p[3]
        val candidates=mutableListOf<CrawlLink>()
        for(i in 0 until minOf(array.length(),3000)) {
            val item=array.optJSONObject(i) ?: continue;val path=item.optString("path")
            if(!filePath(path)) continue
            if(item.optString("type")=="blob" && playlist(path)) {
                val raw=if(github) "https://raw.githubusercontent.com/$project/${enc(ref)}/" else "https://${u.host}/$project/-/raw/${enc(ref)}/"
                val address=raw+path.split('/').joinToString("/",transform=::enc)
                candidates.add(CrawlLink(address,path.substringAfterLast('/'),"playlist"))
            } else if(!github && item.optString("type")=="tree") {
                val address=url.substringBefore('?')+"?ref=${enc(ref)}&per_page=100&page=1&path=${enc(path)}"
                candidates.add(CrawlLink(address,path,"catalog"))
            }
        }
        val ordered=candidates.distinctBy { it.url }.sortedBy { if(it.kind=="playlist") 0 else 1 }
        val offset=q[CURSOR]?.toIntOrNull()?.coerceIn(0,3000) ?: 0
        val result=ordered.drop(offset).take(CHUNK).toMutableList()
        if(offset+CHUNK<ordered.size) result.add(CrawlLink(fetchAddress(url)+"&$CURSOR="+(offset+CHUNK),"继续读取节目单目录","catalog"))
        else if(!github && array.length()==100 && (q["page"]?.toIntOrNull() ?: 1)<3) {
            val page=(q["page"]?.toIntOrNull() ?: 1)+1
            val next=fetchAddress(url).replace(Regex("([?&])page=\\d+"),"$1page=$page")
            result.add(CrawlLink(next,"下一页节目单目录","catalog"))
        }
        result
    }.getOrDefault(emptyList())
}
