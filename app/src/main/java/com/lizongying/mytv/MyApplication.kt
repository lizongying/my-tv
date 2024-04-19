package com.lizongying.mytv

import android.app.Application
import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

class MyApplication : Application() {
    private lateinit var displayMetrics: DisplayMetrics

    override fun onCreate() {
        super.onCreate()

        displayMetrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
//        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        windowManager.defaultDisplay.getMetrics(displayMetrics)
    }

    fun getDisplayMetrics(): DisplayMetrics {
        return displayMetrics
    }
}