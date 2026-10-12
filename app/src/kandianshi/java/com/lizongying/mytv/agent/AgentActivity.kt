package com.lizongying.mytv.agent

import android.content.Context
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface

class AgentActivity : FragmentActivity() {
    @Volatile private var server: PairingHttpServer? = null
    @Volatile private var pairingAllowed = false
    private val serverLock = Any()
    private var pairingGeneration = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var pairing: TextView
    private lateinit var qr: ImageView
    private lateinit var open: Button
    private var openedAt = 0L
    private var refreshing = false
    private val tick = object : Runnable {
        override fun run() {
            server?.let { s ->
                if (s.expired) closePairing("配对已结束。需要修改时请重新打开。")
                else pairing.text = "手机与电视连接同一 Wi‑Fi\n剩余 " + ((PairingHttpServer.LIFETIME_MS - (android.os.SystemClock.elapsedRealtime() - openedAt)) / 1000).coerceAtLeast(0) +
                    " 秒\n" + s.url.substringBefore('#') + "\n令牌仅在二维码内，保存后失效"
            }
            if (!refreshing) {
                refreshing = true
                lifecycleScope.launch {
                    val text = withContext(Dispatchers.IO) { AgentRepository.status(applicationContext) }
                    status.text = text
                    refreshing = false
                }
            }
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        val root = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(24), dp(18), dp(24), dp(18)); setBackgroundColor(Color.rgb(18, 35, 42)) }
        val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 0, dp(24), 0) }
        val right = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        root.addView(ScrollView(this).apply { addView(left) }, LinearLayout.LayoutParams(0, -1, 1.15f))
        root.addView(right, LinearLayout.LayoutParams(0, -1, 1f))
        left.addView(label("看电视 · AI 源维护", 25))
        left.addView(label("找源与巡检全部内置。退出播放后自动找源，返回观看时让出资源。", 13))
        open = action("手机扫码配置 API / Key / 来源") { openPairing() }; left.addView(open)
        left.addView(action("立即自主找源与更新（空闲执行）") {
            AgentScheduler.runNow(applicationContext)
            Toast.makeText(this, "维护任务已安排，联网后执行", Toast.LENGTH_SHORT).show()
        })
        left.addView(action("检测并移除无效线路（全列表）") {
            AgentScheduler.scanAll(applicationContext)
            Toast.makeText(this, "已开始检测；失败线路间隔复核 3 次，进度见下方，可返回看电视", Toast.LENGTH_LONG).show()
        })
        left.addView(action("停止全列表检测") {
            AgentScheduler.stopScan(applicationContext)
            Toast.makeText(this, "已安排停止，已完成结果保留", Toast.LENGTH_SHORT).show()
        })
        left.addView(action("当前黑屏，移除此线路") {
            val url = intent.getStringExtra("currentUrl").orEmpty()
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) { runCatching { AgentRepository.removeCurrent(applicationContext, url) } }
                Toast.makeText(this@AgentActivity, if (result.isSuccess) "已移除；返回播放器生效，已移除列表中可恢复" else result.exceptionOrNull()?.message ?: "移除失败", Toast.LENGTH_LONG).show()
            }
        })
        left.addView(action("查看 / 恢复已移除线路") { showRemoved() })
        left.addView(action("自动广告巡检（默认开启）") {
            startActivity(android.content.Intent(this,AutoVisionActivity::class.java))
        })
        left.addView(action("AI 画面巡检：识别无台标纯广告") {
            startActivity(android.content.Intent(this, VisionActivity::class.java)
                .putExtra("currentUrl", intent.getStringExtra("currentUrl").orEmpty()))
        })
        left.addView(action("记忆管理：容量 / 保留时间 / 广告名单") {
            startActivity(android.content.Intent(this,MemoryActivity::class.java))
        })
        left.addView(label("连续 3 次失败才自动移除，尝试之间至少 30 秒。\n整批失败且无法确认网络时暂停清理；剩余线路可用的频道会保留。", 11))
        left.addView(action("当前是纯广告：永久记住并屏蔽") {
            val url = intent.getStringExtra("currentUrl").orEmpty()
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) { runCatching { AgentRepository.block(applicationContext, url) } }
                Toast.makeText(this@AgentActivity, if (result.isSuccess) "已永久屏蔽，返回播放器生效；记忆管理中可解除" else result.exceptionOrNull()?.message ?: "屏蔽失败", Toast.LENGTH_LONG).show()
            }
        })
        status = label("正在读取维护状态…", 13).apply { setPadding(0, dp(16), 0, 0) }; left.addView(status)
        right.addView(label("手机配置", 23))
        qr = ImageView(this).apply { contentDescription = "手机扫码配置二维码"; visibility = View.GONE }
        right.addView(qr, LinearLayout.LayoutParams(dp(238), dp(238)))
        pairing = label("网页默认关闭\n点击左侧“手机扫码配置”临时开启\n仅支持同一局域网访问", 14).apply { gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(8)) }
        right.addView(pairing)
        right.addView(action("关闭配对网页") { closePairing("配置网页已关闭，旧二维码失效。") })
        right.addView(label("使用可信家庭 Wi‑Fi；不要给路由器设置公网转发。\n电视关机后不执行维护，开机联网后补查。", 11).apply { gravity = Gravity.CENTER })
        setContentView(root); open.requestFocus()
    }

    private fun label(text: String, size: Int) = TextView(this).apply {
        this.text = text; textSize = size.toFloat(); setTextColor(Color.rgb(230, 242, 239)); setLineSpacing(dp(3).toFloat(), 1f)
    }
    private fun action(text: String, block: () -> Unit) = Button(this).apply {
        this.text = text; textSize = 14f; isAllCaps = false; isFocusable = true; minHeight = dp(45)
        setOnClickListener { block() }
    }
    private fun dp(value: Int) = (resources.displayMetrics.density * value).toInt()

    private fun showRemoved() {
        lifecycleScope.launch {
            val data = withContext(Dispatchers.IO) {
                val removed = AgentRepository.removed(applicationContext)
                val names = com.lizongying.mytv.TVList.load(applicationContext, 64, true).flatMap { ch -> ch.urls.map { it to ch.title } }.toMap()
                removed to names
            }
            if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) return@launch
            val entries = data.first.take(300)
            if (entries.isEmpty()) { Toast.makeText(this@AgentActivity, "暂无普通移除线路；永久广告请到记忆管理查看", Toast.LENGTH_SHORT).show(); return@launch }
            val labels = entries.map { (url, h) ->
                (data.second[url] ?: "直播线路") + " · " + h.reason + "\n" + NetworkPolicy.displayUrl(url).take(95)
            }.toTypedArray()
            AlertDialog.Builder(this@AgentActivity).setTitle("点击恢复一条线路（共 " + data.first.size + " 条，最多显示 300 条）")
                .setItems(labels) { _, which -> restore(entries[which].first) }
                .setNeutralButton("恢复全部") { _, _ -> restore(null) }
                .setNegativeButton("关闭", null).show()
        }
    }
    private fun restore(url: String?) {
        lifecycleScope.launch {
            val count = withContext(Dispatchers.IO) { AgentRepository.restoreRemoved(applicationContext, url) }
            Toast.makeText(this@AgentActivity, "已恢复 " + count + " 条线路，返回播放器后生效", Toast.LENGTH_LONG).show()
        }
    }

    private fun openPairing() {
        closePairing("正在开启…")
        val generation = synchronized(serverLock) { pairingGeneration }
        open.isEnabled = false
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching {
                val interfaces = java.util.Collections.list(NetworkInterface.getNetworkInterfaces()).filter {
                    it.isUp && !it.isLoopback && !it.name.startsWith("tun") && !it.name.startsWith("rmnet")
                }.sortedBy { if (it.name.startsWith("wlan") || it.name.startsWith("eth")) 0 else 1 }
                val local = interfaces.flatMap { it.interfaceAddresses }.firstOrNull {
                    it.address is Inet4Address && NetworkPolicy.privateIpv4(it.address.hostAddress.orEmpty()) != null && it.networkPrefixLength.toInt() in 8..30
                } ?: error("未找到家庭局域网 IPv4 地址，请连接 Wi‑Fi 或有线网络")
                val store = AgentConfigStore(applicationContext)
                val assets = mapOf(
                    "/pairing.css" to ("text/css; charset=utf-8" to this@AgentActivity.assets.open("pairing.css").bufferedReader().use { it.readText() }),
                    "/pairing.js" to ("application/javascript; charset=utf-8" to this@AgentActivity.assets.open("pairing.js").bufferedReader().use { it.readText() })
                )
                PairingHttpServer(local.address.hostAddress.orEmpty(), local.networkPrefixLength.toInt(),
                    assets = assets, html = this@AgentActivity.assets.open("pairing.html").bufferedReader().use { it.readText() },
                    readConfig = { store.publicJson().toString() }, saveConfig = { body -> store.save(body) },
                    testConfig = { body -> AiClient(applicationContext).test(body) },
                    saved = { handler.post {
                        pairing.text = "手机配置已保存，配对令牌已失效。\n返回播放器即可使用。"
                        AutoVisionSettings(applicationContext).clearApiPause()
                        AgentScheduler.schedule(applicationContext)
                        AutoVisionScheduler.nudge(applicationContext)
                        handler.postDelayed({ closePairing("配置已保存，网页已关闭。") }, 1200)
                    } }).also { created ->
                        synchronized(serverLock) {
                            check(pairingAllowed && generation == pairingGeneration) { "配置页面已关闭，请重新开启" }
                            server = created
                            try { created.start(5000, true) }
                            catch (e: Exception) { server = null; created.stop(); throw e }
                        }
                    }
            } }
            open.isEnabled = true
            result.onSuccess { s ->
                if (isFinishing || isDestroyed || !lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
                    withContext(Dispatchers.IO) { s.stop() }; return@onSuccess
                }
                if (server !== s) return@onSuccess
                openedAt = android.os.SystemClock.elapsedRealtime()
                qr.setImageBitmap(qrBitmap(s.url)); qr.visibility = View.VISIBLE
                pairing.text = "请用同一 Wi‑Fi 下的手机扫描二维码\n保存后或 10 分钟后自动关闭"
            }.onFailure { pairing.text = it.message ?: "无法开启配置网页" }
        }
    }

    private fun qrBitmap(value: String): Bitmap {
        val size = 600
        val matrix = QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, size, size,
            mapOf(EncodeHintType.MARGIN to 3, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M))
        val pixels = IntArray(size * size) { i -> if (matrix[i % size, i / size]) Color.BLACK else Color.WHITE }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    private fun closePairing(message: String) {
        val old = synchronized(serverLock) { pairingGeneration++; val previous = server; server = null; previous }
        if (old != null) Thread({ old.stop() }, "close-pairing").start()
        if (::qr.isInitialized) qr.visibility = View.GONE
        if (::pairing.isInitialized) pairing.text = message
    }
    override fun onStart() { super.onStart(); pairingAllowed = true; handler.post(tick) }
    override fun onStop() { pairingAllowed = false; handler.removeCallbacksAndMessages(null); closePairing("页面已离开，配对网页已关闭。请重新开启。"); super.onStop() }
    override fun onDestroy() { closePairing("配对网页已关闭"); super.onDestroy() }
}
