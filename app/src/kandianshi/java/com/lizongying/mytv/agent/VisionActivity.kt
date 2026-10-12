package com.lizongying.mytv.agent

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.annotation.OptIn
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import com.lizongying.mytv.TVList
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

/** A dedicated foreground player captures only decoded IPTV video, never the phone configuration or Key. */
@OptIn(UnstableApi::class)
class VisionActivity : FragmentActivity() {
    private lateinit var store: VisionStore
    private lateinit var texture: TextureView
    private lateinit var video: AspectRatioFrameLayout
    private lateinit var status: TextView
    private lateinit var usage: TextView
    private lateinit var player: ExoPlayer
    private var scan: Job? = null
    private var activeId = ""
    private var rendered = false
    private var playbackError = false
    private var serial = 0L
    private var usageJob: Job? = null
    private val decoderOwner=Any()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = VisionStore(applicationContext)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(dp(24), dp(18), dp(24), dp(18)); setBackgroundColor(Color.rgb(18,35,42))
        }
        val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0,0,dp(20),0) }
        root.addView(ScrollView(this).apply { addView(left) }, LinearLayout.LayoutParams(0,-1,1f))
        val right = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        root.addView(right, LinearLayout.LayoutParams(0,-1,1.2f))
        left.addView(label("台标与纯广告 · AI 画面巡检", 22))
        left.addView(label("每条线路取 3 张画面，间隔 10 秒。\n电视台插播广告保留；无台标纯广告只列为疑似，确认后才移除。", 13))
        val start = action("开始 / 继续全列表巡检") { startScan(false) }
        left.addView(start)
        left.addView(action("仅识别刚才观看的线路") { startScan(true) })
        left.addView(action("暂停巡检（保留进度）") { pauseScan() })
        left.addView(action("查看疑似纯广告，逐条确认") { showResults(true) })
        left.addView(action("查看全部识别结果") { showResults(false) })
        left.addView(action("自动巡检设置（默认开启）") { startActivity(android.content.Intent(this,AutoVisionActivity::class.java)) })
        left.addView(action("设置每日识别限额") { chooseLimit() })
        left.addView(action("记忆容量 / 解除广告屏蔽") { startActivity(android.content.Intent(this,MemoryActivity::class.java)) })
        left.addView(label("有效记忆会直接复用，免重复截图与调用；确认纯广告后永久记住。\n每轮最多 2000 条、进度最多 384 KB；超出可使用当前线路识别。",11))
        left.addView(action("返回维护页") { finish() })
        usage = label("",13); left.addView(usage)
        left.addView(label("会将节目截图发送到已配置的 AI API，按图片与文字计费。\n默认每日最多 10 条，可改为 50 / 200 条。每条一次请求，失败也计次；与来源发现的每日 3 次额度分开。\n需要模型支持图片，DeepSeek 请用 deepseek-flash。\n巡检期间请停留此页，离开会暂停。",11))
        status = label("点击开始巡检；逐条解码检查，不会同时播放多个节目。",15)
        right.addView(status, LinearLayout.LayoutParams(-1,-2).apply { bottomMargin = dp(12) })
        video = AspectRatioFrameLayout(this).apply { setAspectRatio(16f/9f); resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT }
        texture = TextureView(this)
        video.addView(texture, FrameLayout.LayoutParams(-1,-1))
        right.addView(video, LinearLayout.LayoutParams(-1,0,1f))
        right.addView(label("静音预览 · 只采集节目画面\n截图不保存到相册，不采集设置页面或密钥。\n离开时已发送的请求可能仍产生一次费用。",11))
        setContentView(root)
        val http = DefaultHttpDataSource.Factory().setUserAgent("Mozilla/5.0 (Linux; Android) KanDianshi/1.4")
            .setConnectTimeoutMs(8000).setReadTimeoutMs(8000).setAllowCrossProtocolRedirects(true)
        player = ExoPlayer.Builder(this).setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(this,http))).build()
        player.volume = 0f
        player.setVideoTextureView(texture)
        player.addAnalyticsListener(object: AnalyticsListener {
            override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime, output: Any, renderTimeMs: Long) {
                val id = runCatching { eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem.mediaId }.getOrDefault("")
                if (id == activeId) rendered = true
            }
            override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) {
                val id = runCatching { eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem.mediaId }.getOrDefault("")
                if (id == activeId) playbackError = true
            }
        })
        player.addListener(object: Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.height > 0) {
                    val ratio = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                    video.setAspectRatio(if (videoSize.unappliedRotationDegrees % 180 == 0) ratio else 1f / ratio)
                }
            }
        })
        updateUsage()
        start.requestFocus()
    }
    private fun dp(n: Int) = (resources.displayMetrics.density * n).toInt()
    private fun label(s: String, n: Int) = TextView(this).apply { text = s; textSize = n.toFloat(); setTextColor(Color.rgb(230,242,239)); setPadding(0,dp(5),0,dp(5)) }
    private fun action(s: String, f: () -> Unit) = Button(this).apply { text = s; textSize = 13f; isAllCaps = false; isFocusable = true; minHeight = dp(42); setOnClickListener { f() } }
    private fun updateUsage() {
        if (usageJob?.isActive == true) return
        usageJob=lifecycleScope.launch {
            val text=withContext(Dispatchers.IO) {
                "今日图片请求 " + store.used() + " / " + store.limit() + "；疑似待确认 " + store.results("suspect").size + " 条"
            }
            usage.text=text
        }
    }
    private fun chooseLimit() {
        val limits = listOf(10,50,200)
        AlertDialog.Builder(this).setTitle("每日最多识别多少条？三张图合成一次请求")
            .setSingleChoiceItems(limits.map { "$it 条 / 日" }.toTypedArray(), limits.indexOf(store.limit())) { dialog, index ->
                store.setLimit(limits[index]); updateUsage(); dialog.dismiss()
            }.setNegativeButton("取消",null).show()
    }
    private fun pauseScan() {
        scan?.cancel(); if (::player.isInitialized) player.stop()
        status.text = "已暂停，完成的结果和进度已保留。当前未完成线路下次重试。"
    }
    private fun startScan(currentOnly: Boolean) {
        if (scan?.isCompleted == false) { Toast.makeText(this,"正在巡检，可先暂停",Toast.LENGTH_SHORT).show(); return }
        scan = lifecycleScope.launch {
            if (!VisionStore.runLock.tryLock()) { status.text = "另一项画面巡检正在结束，请稍后重试"; return@launch }
            try {
                DecoderGate.acquireForeground(decoderOwner)
                val cfg = AgentConfigStore(applicationContext)
                val memory=withContext(Dispatchers.IO) { AgentRepository.prepareMemory(applicationContext) }
                val routes = withContext(Dispatchers.IO) {
                    val all = VisionPolicy.interleave(TVList.load(applicationContext,64).map { ch -> ch.urls.map { VisionRoute(ch.title,it) } })
                    if (currentOnly) {
                        val url = intent.getStringExtra("currentUrl").orEmpty()
                        require(url.isNotBlank()) { "刚才没有播放线路" }
                        all.firstOrNull { it.url == url }?.let { listOf(it) } ?: error("该线路已经移除；请先恢复再识别")
                    } else all
                }
                val activeStore = if (currentOnly) VisionStore(applicationContext,"singlePlan") else store
                var plan = withContext(Dispatchers.IO) { activeStore.begin(routes,currentOnly) }
                while (!plan.done) {
                    ensureActive()
                    val route = plan.next() ?: break
                    val index = plan.cursor + 1
                    status.text = "巡检 $index / " + plan.routes.size + "\n" + route.title + "\n正在等待真实视频画面…"
                    val excluded = withContext(Dispatchers.IO) {
                        MemoryPolicy.id(route.url) in memory.adIds() ||
                            HealthRanking.sort(listOf(route.url),AgentRepository.health(applicationContext),cfg.load().blocked,System.currentTimeMillis()).isEmpty()
                    }
                    val cached=withContext(Dispatchers.IO) { if (excluded) null else memory.cached(route.url) }
                    if (excluded || cached != null) {
                        plan=withContext(Dispatchers.IO) { activeStore.advanceCached(plan,route) }
                        player.stop()
                        status.text=route.title + if (excluded) "：已屏蔽，直接跳过" else "：复用有效记忆，未调用 API\n" + labelKind(cached!!.optString("kind"))
                        updateUsage(); delay(80); continue
                    }
                    require(cfg.load().aiEnabled && cfg.apiKey().isNotBlank()) { "此线路没有有效记忆，请先扫码配置并启用 AI" }
                    require(store.used() < store.limit()) { "今日图片请求已达限额，进度已保留；明日继续" }
                    var frames = emptyList<String>()
                    val verdict = if (excluded) VisionVerdict("excluded","该线路已被移除或屏蔽，跳过")
                    else {
                        frames = try { capture(route, index, plan.routes.size) }
                        catch (e: CancellationException) { throw e }
                        catch (_: Exception) { emptyList() }
                        if (frames.size != VisionPolicy.FRAME_COUNT) VisionVerdict("unavailable","未取得三张可靠视频画面；保留线路，可用无效线路检测复核")
                        else {
                            ensureActive()
                            status.text = "巡检 $index / " + plan.routes.size + "\n" + route.title + "\n正在识别台标与纯广告…"
                            player.pause()
                            withContext(Dispatchers.IO) { VisionClient(applicationContext).inspect(route.title,frames) }
                        }
                    }
                    frames = emptyList()
                    ensureActive()
                    plan = withContext(Dispatchers.IO) { activeStore.commit(plan,route,verdict) }
                    updateUsage()
                    player.stop()
                    status.text = route.title + "：" + labelKind(verdict.kind) + "\n" + verdict.reason
                    delay(500)
                }
                val suspectCount=withContext(Dispatchers.IO) { store.results("suspect").size }
                status.text = "本轮巡检完成。疑似纯广告 " + suspectCount + " 条。\n请打开待确认列表逐条查看；未自动移除任何线路。"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status.text = (if (e is IllegalArgumentException || e is IllegalStateException) e.message else "画面识别连接失败，请检查网络或 API 配置").orEmpty() +
                    "\n已保留进度和线路，可重新开始继续。"
            } finally {
                if (!isDestroyed) { player.stop(); updateUsage() }
                DecoderGate.releaseLease(decoderOwner)
                VisionStore.runLock.unlock()
            }
        }
    }

    private suspend fun capture(route: VisionRoute, index: Int, total: Int): List<String> {
        activeId = "vision-" + (++serial)
        rendered = false; playbackError = false
        player.setMediaItem(AgentRepository.mediaItem(this,route.url,activeId))
        player.prepare(); player.play()
        val ready = withTimeoutOrNull(20_000) {
            while (!rendered || !player.isPlaying || !texture.isAvailable) {
                check(!playbackError) { "解码失败" }; delay(150)
            }
            true
        } ?: false
        check(ready)
        delay(3000)
        val encoded = mutableListOf<String>()
        repeat(VisionPolicy.FRAME_COUNT) { n ->
            if (n > 0) delay(10_000)
            currentCoroutineContext().ensureActive()
            check(rendered && !playbackError && player.isPlaying && texture.isAvailable && texture.width > 0)
            status.text = "巡检 $index / $total\n" + route.title + "\n取样 " + (n+1) + " / 3（间隔 10 秒）"
            val bitmap = texture.getBitmap(960,(960L * texture.height / texture.width).toInt().coerceIn(240,960)) ?: error("无法读取画面")
            try {
                val pixels = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
                check(VisionFrameQuality.usable(pixels)) { "黑屏或空截图" }
                val bytes = withContext(Dispatchers.Default) {
                    val out = ByteArrayOutputStream()
                    check(bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,82,out))
                    out.toByteArray().also { check(it.size <= 440_000) }
                }
                encoded.add("data:image/jpeg;base64," + Base64.encodeToString(bytes,Base64.NO_WRAP))
            } finally { bitmap.recycle() }
        }
        check(encoded.distinct().size > 1) { "三张画面完全相同，可能是截图或播放停滞" }
        return encoded
    }
    private fun labelKind(kind: String): String = when(kind) {
        "tv" -> "有台标，保留"; "suspect" -> "疑似无台标纯广告，待确认"
        "removed" -> "已确认移除"; "kept" -> "本次已确认保留"
        "unavailable" -> "未能取得可靠画面，保留"; "excluded" -> "已移除，跳过"
        else -> "无法确定，保留"
    }
    private fun showResults(suspectsOnly: Boolean) = lifecycleScope.launch {
        val entries = withContext(Dispatchers.IO) { AgentRepository.prepareMemory(applicationContext); store.results(if (suspectsOnly) "suspect" else null).take(300) }
        if (entries.isEmpty()) { Toast.makeText(this@VisionActivity,"暂无结果",Toast.LENGTH_SHORT).show(); return@launch }
        AlertDialog.Builder(this@VisionActivity).setTitle(if (suspectsOnly) "疑似纯广告，点击查看依据" else "识别结果（最近 300 条）")
            .setItems(entries.map { it.second.optString("title") + " · " + labelKind(it.second.optString("kind")) }.toTypedArray()) { _,index ->
                val (url,result) = entries[index]
                val dialog = AlertDialog.Builder(this@VisionActivity).setTitle(result.optString("title")).setMessage(
                    labelKind(result.optString("kind")) + "\n" + result.optString("reason") + "\n" + NetworkPolicy.displayUrl(url).take(150) +
                    "\n仅反映三次取样，不能证明全天内容。确认后永久记住，在记忆管理中可解除。").setNegativeButton("关闭",null)
                if (result.optString("kind") == "suspect") {
                    dialog.setPositiveButton("确认纯广告，移除") { _,_ -> lifecycleScope.launch {
                        val outcome = withContext(Dispatchers.IO) { runCatching { store.resolve(url,"removed",result.optLong("at")) } }
                        updateUsage(); Toast.makeText(this@VisionActivity,if (outcome.isSuccess) "已永久记住；记忆管理中可解除屏蔽" else outcome.exceptionOrNull()?.message ?: "保存失败，请重新查看",Toast.LENGTH_LONG).show()
                    } }
                    dialog.setNeutralButton("本次保留") { _,_ -> lifecycleScope.launch {
                        val outcome = withContext(Dispatchers.IO) { runCatching { store.resolve(url,"kept",result.optLong("at")) } }
                        if (outcome.isFailure) Toast.makeText(this@VisionActivity,"结果已变化，请重新查看",Toast.LENGTH_LONG).show()
                        updateUsage()
                    } }
                }
                dialog.show()
            }.setNegativeButton("关闭",null).show()
    }
    override fun onStart() { super.onStart(); DecoderGate.requestForeground(decoderOwner) }
    override fun onStop() { pauseScan(); DecoderGate.releaseForeground(decoderOwner); super.onStop() }
    override fun onDestroy() { scan?.cancel(); player.release(); DecoderGate.releaseForeground(decoderOwner); super.onDestroy() }
}
