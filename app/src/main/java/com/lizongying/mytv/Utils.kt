package com.lizongying.mytv

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {
    fun getDateFormat(format: String): String {
        return SimpleDateFormat(format, Locale.CHINA).format(Date())
    }

    fun getDateTimestamp(): Long {
        return Date().time / 1000
    }
}