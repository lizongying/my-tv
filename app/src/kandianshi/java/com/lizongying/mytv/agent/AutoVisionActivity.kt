package com.lizongying.mytv.agent

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutoVisionActivity: FragmentActivity() {
    private lateinit var settings: AutoVisionSettings
    private lateinit var status: TextView
    private lateinit var enabled: Button
    private lateinit var blocking: Button
    private lateinit var network: Button
    private val handler=Handler(Looper.getMainLooper())
    private var reading=false
    private val tick=object: Runnable { override fun run() { refresh(); handler.postDelayed(this,2000) } }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state); settings=AutoVisionSettings(applicationContext)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        val content=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; setPadding(dp(28),dp(16),dp(28),dp(16)); setBackgroundColor(Color.rgb(18,35,42))
        }
        content.addView(label("看电视 · 自动广告巡检",25))
        content.addView(label("默认开启，配置并启用 AI 后自动执行。无需进入手动画面巡检。",15))
        enabled=action("") { change { settings.setEnabled(!settings.enabled()) } }; content.addView(enabled)
        blocking=action("") { change { settings.setAutoBlock(!settings.autoBlock()) } }; content.addView(blocking)
        network=action("") { change { settings.setWifiOnly(!settings.wifiOnly()) } }; content.addView(network)
        content.addView(action("设置共享的每日图片调用额度") {
            val values=listOf(10,50,200); val store=VisionStore(applicationContext)
            AlertDialog.Builder(this).setTitle("自动与手动合计每日图片调用次数；失败也计次")
                .setSingleChoiceItems(values.map { "$it 次 / 日" }.toTypedArray(),values.indexOf(store.limit())) { dialog,index ->
                    change { store.setLimit(values[index]); AutoVisionScheduler.nudge(applicationContext) }; dialog.dismiss()
                }.setNegativeButton("取消",null).show()
        })
        content.addView(action("查看 / 解除广告记忆") { startActivity(android.content.Intent(this,MemoryActivity::class.java)) })
        status=label("正在读取…",14); content.addView(status)
        content.addView(label("播放优先：你看电视或手动巡检时，后台让出解码器；离开播放后约 2 分钟安排补查。\n每批最多 3 条新识别，未完成时约 30 分钟后空闲续查；另按来源维护周期补查。\n同一线路两轮独立取样，相隔至少 30 分钟，每轮三帧均为高置信无台标纯广告，才自动屏蔽。有台标的插播广告保留。\n自动与手动共用默认每日 10 次额度，可改为 50 / 200。节目截图会发送到已配置 API，按服务商规则计费；截图不落盘。\n关机、强制停止、省电或系统调度可能延后任务；默认仅非计费网络。关闭自动屏蔽后仍自动检测，仅列为疑似。\n误判可到记忆管理解除；解除会清除该条复核记录，普通缓存清理不会解除永久屏蔽。",12))
        content.addView(action("返回") { finish() })
        setContentView(ScrollView(this).apply { addView(content) }); enabled.requestFocus()
        refresh()
    }
    private fun dp(n: Int)=(resources.displayMetrics.density*n).toInt()
    private fun label(s: String,n: Int)=TextView(this).apply { text=s; textSize=n.toFloat(); setTextColor(Color.rgb(230,242,239)); setPadding(0,dp(6),0,dp(6)) }
    private fun action(s: String,f: ()->Unit)=Button(this).apply { text=s; textSize=14f; isAllCaps=false; isFocusable=true; setOnClickListener { f() } }
    private fun change(f: ()->Unit)=lifecycleScope.launch {
        val result=withContext(Dispatchers.IO) { runCatching { f() } }
        result.exceptionOrNull()?.let { Toast.makeText(this@AutoVisionActivity,it.message ?: "保存失败",Toast.LENGTH_LONG).show() }
        refresh()
    }
    private fun refresh() {
        if(reading) return
        reading=true
        lifecycleScope.launch {
            try {
                val text=withContext(Dispatchers.IO) {
                    val store=VisionStore(applicationContext); val config=AgentConfigStore(applicationContext)
                    settings.summary()+"\n今日图片调用 "+store.used()+" / "+store.limit()+
                        if(config.load().aiEnabled && config.apiKey().isNotBlank()) "" else "\n请先在维护页扫码配置并启用 AI"
                }
                status.text=text
                enabled.text="自动后台巡检："+if(settings.enabled()) "开启（点击关闭）" else "关闭（点击开启）"
                blocking.text="自动屏蔽："+if(settings.autoBlock()) "两轮复核后屏蔽（点击改为仅检测）" else "仅检测（点击开启自动屏蔽）"
                network.text="网络限制："+if(settings.wifiOnly()) "仅非计费网络（点击改为任意联网）" else "任意联网（点击限制非计费网络）"
            } finally { reading=false }
        }
    }
    override fun onStart() { super.onStart(); handler.post(tick) }
    override fun onStop() { handler.removeCallbacksAndMessages(null); super.onStop() }
}
