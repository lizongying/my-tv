package com.lizongying.mytv

import android.content.res.Resources
import android.os.Build
import android.util.TypedValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {

    fun getDateFormat(format: String): String {
        return SimpleDateFormat(
            format,
            Locale.CHINA
        ).format(Date(System.currentTimeMillis()))
    }

    fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().displayMetrics
        ).toInt()
    }

    fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), Resources.getSystem().displayMetrics
        ).toInt()
    }

    fun pxToDp(px: Float): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (px / scale).toInt()
    }

    fun pxToDp(px: Int): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (px / scale).toInt()
    }

    fun isTmallDevice() = Build.MANUFACTURER.equals("Tmall", ignoreCase = true)
}
