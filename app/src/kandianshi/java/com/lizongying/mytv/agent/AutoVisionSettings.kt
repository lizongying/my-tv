package com.lizongying.mytv.agent

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AutoVisionSettings(private val context: Context) {
    private val prefs=context.getSharedPreferences("agent_auto_vision",Context.MODE_PRIVATE)
    fun enabled(): Boolean=prefs.getBoolean("enabled",true)
    fun autoBlock(): Boolean=prefs.getBoolean("autoBlock",true)
    fun wifiOnly(): Boolean=prefs.getBoolean("wifiOnly",true)
    fun setEnabled(value: Boolean) { check(prefs.edit().putBoolean("enabled",value).commit()); AutoVisionScheduler.schedule(context) }
    fun setAutoBlock(value: Boolean) { check(prefs.edit().putBoolean("autoBlock",value).commit()) }
    fun setWifiOnly(value: Boolean) { check(prefs.edit().putBoolean("wifiOnly",value).commit()); AutoVisionScheduler.schedule(context) }
    fun apiPaused(): Boolean=System.currentTimeMillis()<prefs.getLong("apiPauseUntil",0)
    fun pauseApi() { prefs.edit().putLong("apiPauseUntil",System.currentTimeMillis()+6*3_600_000L).apply() }
    fun clearApiPause() { prefs.edit().remove("apiPauseUntil").apply() }
    fun status(text: String) { prefs.edit().putString("status",text.take(240)).putLong("at",System.currentTimeMillis()).apply() }
    fun summary(): String {
        val stamp=prefs.getLong("at",0)
        val time=if(stamp == 0L) "尚未运行" else SimpleDateFormat("MM-dd HH:mm",Locale.CHINA).format(Date(stamp))
        return "自动画面巡检："+(if(enabled()) "开启" else "关闭")+"；自动屏蔽："+(if(autoBlock()) "两轮复核后开启" else "仅检测")+
            "\n网络："+(if(wifiOnly()) "仅非计费网络（通常 Wi-Fi / 有线）" else "任意联网")+
            "\n"+time+" · "+prefs.getString("status","配置并启用 AI 后，空闲时自动执行")
    }
}
