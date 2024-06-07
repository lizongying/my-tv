package com.lizongying.mytv

import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.multidex.MultiDex
import com.lizongying.mytv.models.MyViewModel


class MyTVApplication : Application() {
    private lateinit var displayMetrics: DisplayMetrics
    private lateinit var realDisplayMetrics: DisplayMetrics

    private lateinit var windowManager: WindowManager

    private var width = 0
    private var height = 0
    private var shouldWidth = 0
    private var shouldHeight = 0
    private var deviation = 0
    private var ratio = 1.0
    private var density = 2.0f
    private var scale = 1.0f

    lateinit var myViewModel: MyViewModel

    override fun onCreate() {
        super.onCreate()

        displayMetrics = DisplayMetrics()
        realDisplayMetrics = DisplayMetrics()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
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

        if ((width.toDouble() / height) < (16.0 / 9.0)) {
            ratio = width * 2 / 1920.0 / density
            shouldWidth = width
            shouldHeight = (width * 9.0 / 16.0).toInt()
        } else {
            ratio = height * 2 / 1080.0 / density
            shouldHeight = height
            shouldWidth = (height * 16.0 / 9.0).toInt()
        }

        deviation = width - Resources.getSystem().displayMetrics.widthPixels
        scale = Resources.getSystem().displayMetrics.scaledDensity

        myViewModel = ViewModelProvider(
            ViewModelStore(),
            ViewModelProvider.NewInstanceFactory()
        )[MyViewModel::class.java]
    }

    fun getDisplayMetrics(): DisplayMetrics {
        return displayMetrics
    }

    fun getRealDisplayMetrics(): DisplayMetrics {
        return realDisplayMetrics
    }

    fun shouldWidthPx(): Int {
        return shouldWidth
    }

    fun shouldHeightPx(): Int {
        return shouldHeight
    }

    fun getWidth(): Int {
        return width
    }

    fun getHeight(): Int {
        return height
    }

    fun getDeviation(): Int {
        return deviation
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