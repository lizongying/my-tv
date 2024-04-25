package com.lizongying.mytv

import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.multidex.MultiDex


class MyTvApplication : Application() {
    private lateinit var displayMetrics: DisplayMetrics
    private lateinit var realDisplayMetrics: DisplayMetrics

    private var width = 0
    private var height = 0
    private var deviation = 0
    private var ratio = 1.0
    private var density = 2.0f
    private var scale = 1.0f

    override fun onCreate() {
        super.onCreate()

        displayMetrics = DisplayMetrics()
        realDisplayMetrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        windowManager.defaultDisplay.getRealMetrics(realDisplayMetrics)

        if (realDisplayMetrics.heightPixels > realDisplayMetrics.widthPixels) {
            width = realDisplayMetrics.heightPixels
            height = realDisplayMetrics.widthPixels
        } else {
            width = realDisplayMetrics.widthPixels
            height = realDisplayMetrics.heightPixels
        }

        density = displayMetrics.density

        ratio = if ((width.toDouble() / height) < (16.0 / 9)) {
            width * 2 / 1920.0 / density
        } else {
            height * 2 / 1080.0 / density
        }

        deviation = width - Resources.getSystem().displayMetrics.widthPixels
        scale = Resources.getSystem().displayMetrics.scaledDensity
    }

    fun getDisplayMetrics(): DisplayMetrics {
        return displayMetrics
    }

    fun getRealDisplayMetrics(): DisplayMetrics {
        return realDisplayMetrics
    }

    fun getWidth(): Int {
        return width
    }

    fun getDeviation(): Int {
        return deviation
    }

    fun getHeight(): Int {
        return height
    }

    fun getRatio(): Double {
        return ratio
    }

    fun dp2Px(dp: Int): Int {
        return (dp * ratio * density + 0.5f).toInt()
    }

    fun px2Px(px: Int): Int {
        return (px * ratio + 0.5f).toInt()
    }

    fun px2PxFont(px: Float): Float {
        return (px * ratio / scale).toFloat()
    }

    fun sp2Px(sp: Float): Float {
        return (sp * ratio * scale).toFloat()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(base)
    }
}