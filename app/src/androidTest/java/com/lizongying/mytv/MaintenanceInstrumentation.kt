package com.lizongying.mytv

import android.app.ActivityManager
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import com.lizongying.mytv.agent.MaintenanceService
import com.lizongying.mytv.agent.AgentActivity
import java.io.File

/** Run only on the dedicated offline emulator; never calls a model or public source. */
class MaintenanceInstrumentation: Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val c=targetContext
        val config=c.getSharedPreferences("agent_config",Context.MODE_PRIVATE)
        val previous=config.getString("config",null)
        val result=Bundle()
        try {
            check(android.os.Build.MODEL.contains("sdk",true) || android.os.Build.PRODUCT.contains("sdk",true))
            check(config.edit().putString("config","{\"enabled\":false}").commit())
            val checkpoint=File(c.filesDir,"write-buffer-smoke.json")
            checkpoint.delete()
            repeat(1000) { com.lizongying.mytv.agent.BufferedState.write(c,checkpoint.name,it.toString(),true) }
            check(!checkpoint.exists())
            check(com.lizongying.mytv.agent.BufferedState.read(c,checkpoint.name)=="999")
            com.lizongying.mytv.agent.BufferedState.write(c,checkpoint.name,"critical",false)
            com.lizongying.mytv.agent.BufferedState.flush()
            check(checkpoint.readText()=="critical")
            checkpoint.delete()
            MaintenanceService.setEnabled(c,true)
            runOnMainSync { MaintenanceService.ensure(c) }
            repeat(50) { if(!MaintenanceService.running) Thread.sleep(100) }
            check(MaintenanceService.running)
            c.startActivity(Intent(c,AgentActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Thread.sleep(4000)
            uiAutomation.takeScreenshot()?.let { image -> File(c.filesDir,"maintenance-preview.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }; image.recycle() }
            c.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Thread.sleep(3000)
            @Suppress("DEPRECATION")
            val services=(c.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getRunningServices(100)
            check(services.any { it.service.className==MaintenanceService::class.java.name && it.foreground })
            check(MaintenanceService.running)
            runOnMainSync { c.startService(Intent(c,MaintenanceService::class.java).setAction("stop")) }
            repeat(50) { if(MaintenanceService.running) Thread.sleep(100) }
            check(!MaintenanceService.running)
            result.putString("stream","PERSISTENT_SERVICE_HOME_AND_STOP_OK; BUFFERED_STORAGE_OS_OK")
            finish(-1,result)
        } catch(e: Throwable) { result.putString("stream",e.toString());finish(0,result) }
        finally { config.edit().apply { if(previous==null) remove("config") else putString("config",previous) }.commit() }
    }
}
