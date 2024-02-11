package com.lizongying.mytv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sp = context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE)
        if (sp.getBoolean(MainActivity.BOOT_STARTUP, true)) {
            context.startActivity(
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

}