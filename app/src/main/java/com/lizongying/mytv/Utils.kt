package com.lizongying.mytv

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {
    fun getDateFormat(format: String): String {
        return SimpleDateFormat(format, Locale.CHINA).format(Date())
    }

    fun getDateTimestamp(): Int {
        return (Date().time / 1000).toInt()
    }
}