package com.lizongying.mytv.agent

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.webkit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext

/** A disposable WebView executes public players; all HTTP reads use our bounded transport.
 * No JS bridge, downloads, login, cookies, API credentials or video segments are supplied. */
class WebPageResolver(private val context: Context) {
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun resolve(url: String, html: String, hint: String, allowed: () -> Unit,
        transport: (String, Map<String,String>) -> HttpFetch.Reply = { u,h -> HttpFetch.get(u,1024*1024,headers=h) },
        durationMs: Long = 18_000): List<CrawlLink> {
        NetworkPolicy.publicUrl(url,false)
        val closed=AtomicBoolean(false); val requests=AtomicInteger(); val bytes=AtomicInteger()
        val lock=Any(); val links=linkedMapOf<String,CrawlLink>(); var pageTitle=hint
        var view: WebView?=null
        fun remember(items: List<CrawlLink>) = synchronized(lock) {
            items.forEach { if(links.size<48 && !links.containsKey(it.url)) links[it.url]=it }
        }
        fun empty()=WebResourceResponse("text/plain","UTF-8",ByteArrayInputStream(ByteArray(0)))
        val csp="default-src http: https: data: blob: 'unsafe-inline' 'unsafe-eval'; connect-src http: https:; object-src 'none'; worker-src 'none'; child-src http: https:; form-action 'none'"
        try {
            allowed(); coroutineContext.ensureActive()
            withContext(Dispatchers.Main) {
                view=WebView(context.applicationContext).apply {
                    settings.javaScriptEnabled=true
                    settings.domStorageEnabled=false
                    settings.databaseEnabled=false
                    settings.allowFileAccess=false; settings.allowContentAccess=false
                    settings.javaScriptCanOpenWindowsAutomatically=false
                    settings.setSupportMultipleWindows(false); settings.setGeolocationEnabled(false)
                    settings.cacheMode=WebSettings.LOAD_NO_CACHE
                    settings.loadsImagesAutomatically=false
                    settings.mediaPlaybackRequiresUserGesture=false
                    settings.mixedContentMode=WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this,false)
                    webChromeClient=object: WebChromeClient() {
                        override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
                        override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) { callback.invoke(origin,false,false) }
                        override fun onJsAlert(v: WebView?,u: String?,m: String?,r: JsResult): Boolean { r.cancel(); return true }
                        override fun onJsConfirm(v: WebView?,u: String?,m: String?,r: JsResult): Boolean { r.cancel(); return true }
                    }
                    setDownloadListener { _,_,_,_,_ -> }
                    webViewClient=object: WebViewClient() {
                        override fun shouldOverrideUrlLoading(v: WebView?,request: WebResourceRequest): Boolean =
                            request.url.toString()!=url // Link discovery queues navigation; this instance resolves one player only.
                        @Deprecated("Android 5 fallback")
                        override fun shouldOverrideUrlLoading(v: WebView?,u: String?): Boolean = u!=url
                        override fun shouldInterceptRequest(v: WebView?,request: WebResourceRequest): WebResourceResponse {
                            if(closed.get() || request.method!="GET" || requests.incrementAndGet()>40) return empty()
                            val address=DiscoveryPolicy.canonical(request.url.toString()) ?: return empty()
                            if(StationCrawler.isMedia(address)) {
                                remember(listOf(CrawlLink(address,synchronized(lock) { pageTitle },"media")))
                                return empty() // Capture manifests without rendering/decoding another video.
                            }
                            if(StationCrawler.isAsset(address)) return empty()
                            return try {
                                val headers=request.requestHeaders.filterKeys { it.lowercase() in setOf("accept","user-agent","referer","origin") }
                                val reply=transport(address,headers)
                                if(closed.get() || bytes.addAndGet(reply.bytes.size)>6*1024*1024) return empty()
                                val text=reply.bytes.toString(Charsets.UTF_8)
                                val title=synchronized(lock) { pageTitle }
                                if(SourceTools.isHls(text) || reply.type.contains("mpegurl",true)) {
                                    remember(listOf(CrawlLink(reply.url,title,"media"))); return empty()
                                }
                                if(reply.type.startsWith("video/",true) || reply.type.startsWith("audio/",true)) return empty()
                                remember(StationCrawler.links(reply.url,text,title)+StationCrawler.jsonStreams(reply.url,text,title))
                                val mime=reply.type.substringBefore(';').ifBlank { "text/plain" }
                                val responseHeaders=reply.headers.filterKeys { it.lowercase() in setOf("access-control-allow-origin","access-control-allow-credentials","access-control-allow-headers") }.toMutableMap()
                                responseHeaders["Content-Security-Policy"]=csp
                                WebResourceResponse(mime,"UTF-8",200,"OK",responseHeaders,ByteArrayInputStream(reply.bytes))
                            } catch(_: Exception) { empty() }
                        }
                        override fun onReceivedSslError(v: WebView?,handler: android.webkit.SslErrorHandler,error: android.net.http.SslError?) { handler.cancel() }
                    }
                    val injected="<meta http-equiv=\"Content-Security-Policy\" content=\"$csp\">"
                    val head=Regex("(?i)<head[^>]*>").find(html)
                    val content=if(head!=null) html.substring(0,head.range.last+1)+injected+html.substring(head.range.last+1)
                        else "<head>$injected</head>"+html
                    loadDataWithBaseURL(url,content,"text/html","UTF-8",null)
                }
            }
            val start=android.os.SystemClock.elapsedRealtime()
            while(android.os.SystemClock.elapsedRealtime()-start<durationMs) {
                delay(500); coroutineContext.ensureActive(); allowed()
                withContext(Dispatchers.Main) {
                    view?.evaluateJavascript(SNAPSHOT) { raw ->
                        if(!closed.get()) runCatching {
                            val value=JSONArray("[$raw]").getString(0); val data=JSONObject(value)
                            val title=data.optString("title").ifBlank { hint }.take(160)
                            synchronized(lock) { pageTitle=title }
                            remember(StationCrawler.links(url,data.optString("html"),title))
                            val resources=data.optJSONArray("resources") ?: JSONArray()
                            for(i in 0 until minOf(resources.length(),120)) {
                                val u=StationCrawler.resolve(url,resources.optString(i)) ?: continue
                                if(StationCrawler.isMedia(u)) remember(listOf(CrawlLink(u,title,"media")))
                            }
                        }
                    }
                }
            }
            return synchronized(lock) { links.values.toList().map { if(it.title==hint) it.copy(title=pageTitle) else it } }
        } finally {
            closed.set(true)
            withContext(kotlinx.coroutines.NonCancellable+Dispatchers.Main) {
                view?.apply { stopLoading(); onPause(); webChromeClient=null; destroy() }; view=null
            }
        }
    }
    companion object {
        // Only player controls are activated; forms, arbitrary buttons and station-account prompts are untouched.
        private val SNAPSHOT="""(function(){document.querySelectorAll('video').forEach(function(v){v.muted=true;try{var p=v.play();if(p&&p.catch)p.catch(function(){});}catch(e){}});if(!window.__kanPlayed){window.__kanPlayed=true;document.querySelectorAll('.vjs-big-play-button,.jw-icon-display,.dplayer-play-icon').forEach(function(b){try{b.click();}catch(e){}});}return JSON.stringify({title:document.title,html:document.documentElement.outerHTML.slice(0,262144),resources:performance.getEntriesByType('resource').slice(-120).map(function(r){return r.name;}).concat(Array.prototype.map.call(document.querySelectorAll('video,source'),function(v){return v.currentSrc||v.src||'';}))});})()"""
    }
}
