package com.lizongying.mytv.agent

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MemoryActivity : FragmentActivity() {
    private lateinit var store: MemoryStore
    private lateinit var info: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store=MemoryStore(applicationContext)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        val content=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL; setPadding(dp(30),dp(20),dp(30),dp(20)); setBackgroundColor(Color.rgb(18,35,42))
        }
        content.addView(label("看电视 · 记忆管理",25))
        content.addView(label("确认的纯广告线路永久跳过。普通识别结果会过期，失败来源按 1 小时、6 小时、1 天、3 天、7 天逐步延后重试。",14))
        info=label("正在读取…",15); content.addView(info)
        val size=action("设置普通记忆容量（1 / 2 / 5 MB）") {
            val values=listOf(1,2,5)
            AlertDialog.Builder(this).setTitle("普通记忆容量，到上限自动淘汰最旧记录")
                .setSingleChoiceItems(values.map { "$it MB" }.toTypedArray(),values.indexOf(store.settings().mib)) { dialog,index ->
                    work { store.configure(store.settings().copy(mib=values[index])) }; dialog.dismiss()
                }.setNegativeButton("取消",null).show()
        }; content.addView(size)
        content.addView(action("设置普通记忆保留时间（1 / 7 / 30 / 90 天）") {
            val values=listOf(1,7,30,90)
            AlertDialog.Builder(this).setTitle("最长保留时间；疑似广告最多 1 天，失败截图最多 6 小时")
                .setSingleChoiceItems(values.map { "$it 天" }.toTypedArray(),values.indexOf(store.settings().days)) { dialog,index ->
                    work { store.configure(store.settings().copy(days=values[index])) }; dialog.dismiss()
                }.setNegativeButton("取消",null).show()
        })
        content.addView(action("查看 / 解除永久广告屏蔽") { showAds(0) })
        content.addView(action("清理普通记忆与巡检进度") {
            AlertDialog.Builder(this).setTitle("清理普通记忆？").setMessage("会清除近期识别、失败来源冷却、找源索引与巡检进度。永久广告屏蔽、API Key、每日调用限额保留。")
                .setPositiveButton("清理") { _,_ -> work {
                    check(VisionStore.runLock.tryLock()) { "画面巡检正在结束，请稍后清理" }
                    try {
                        check(AgentRepository.runLock.tryLock()) { "来源维护正在进行，请稍后清理" }
                        try { store.clearRecent(); VisionStore(applicationContext).clearPlans() }
                        finally { AgentRepository.runLock.unlock() }
                    } finally { VisionStore.runLock.unlock() }
                } }.setNegativeButton("取消",null).show()
        })
        content.addView(label("默认 1 MB / 7 天。永久屏蔽单独最多 2 MB / 5000 条，满后需手动解除旧条目，不会自动忘记。\n只保存线路指纹与简短结论，不保存聊天全文或截图。上限针对记忆文件；节目源缓存及应用本体另计。\n同一完整线路地址会复用记忆；地址或参数改变后需重新判断。仅疑似不会自动永久封禁。",13))
        content.addView(action("返回维护页") { finish() })
        setContentView(ScrollView(this).apply { addView(content) }); size.requestFocus()
        refresh()
    }
    private fun dp(n: Int)=(resources.displayMetrics.density*n).toInt()
    private fun label(s: String,n: Int)=TextView(this).apply {
        text=s; textSize=n.toFloat(); setTextColor(Color.rgb(230,242,239)); setPadding(0,dp(6),0,dp(6))
    }
    private fun action(s: String,f: ()->Unit)=Button(this).apply {
        text=s; textSize=14f; isAllCaps=false; isFocusable=true; setOnClickListener { f() }
    }
    private fun refresh()=lifecycleScope.launch {
        val result=withContext(Dispatchers.IO) { runCatching { AgentRepository.prepareMemory(applicationContext).summary() } }
        info.text=result.getOrElse { it.message ?: "读取记忆失败" }
    }
    private fun work(block: ()->Unit)=lifecycleScope.launch {
        val result=withContext(Dispatchers.IO) { runCatching { AgentRepository.prepareMemory(applicationContext); block() } }
        Toast.makeText(this@MemoryActivity,result.fold({ "已完成" },{ it.message ?: "操作失败" }),Toast.LENGTH_LONG).show()
        refresh()
    }
    private fun showAds(page: Int): kotlinx.coroutines.Job=lifecycleScope.launch {
        val result=withContext(Dispatchers.IO) { runCatching { AgentRepository.prepareMemory(applicationContext).ads() } }
        if (result.isFailure) { Toast.makeText(this@MemoryActivity,result.exceptionOrNull()?.message ?: "读取失败",Toast.LENGTH_LONG).show(); return@launch }
        val all=result.getOrThrow()
        if (all.isEmpty()) { Toast.makeText(this@MemoryActivity,"暂无永久广告屏蔽",Toast.LENGTH_SHORT).show(); return@launch }
        val from=(page*100).coerceAtMost(((all.size-1)/100)*100)
        val entries=all.drop(from).take(100)
        val dialog=AlertDialog.Builder(this@MemoryActivity).setTitle("永久广告 " + (from+1) + "–" + (from+entries.size) + " / " + all.size + "，点击解除")
            .setItems(entries.map { it.second.optString("title") + "\n" + it.second.optString("display") +
                "\n编号 " + it.first.take(12) }.toTypedArray()) { _,index ->
                val (id,entry)=entries[index]
                AlertDialog.Builder(this@MemoryActivity).setTitle("解除这条永久屏蔽？")
                    .setMessage(entry.optString("title") + "\n" + entry.optString("display") + "\n" + entry.optString("reason") + "\n下次更新或巡检可再次访问。")
                    .setPositiveButton("解除") { _,_ -> work {
                        store.forgetAd(id); AgentRepository.restoreAdMemory(applicationContext,id)
                    } }.setNegativeButton("取消",null).show()
            }.setNegativeButton("关闭",null)
        if (from+entries.size < all.size) dialog.setPositiveButton("下一页") { _,_ -> showAds(from/100+1) }
        if (from > 0) dialog.setNeutralButton("上一页") { _,_ -> showAds(from/100-1) }
        dialog.show()
    }
}
