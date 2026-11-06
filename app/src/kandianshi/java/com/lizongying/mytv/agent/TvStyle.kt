package com.lizongying.mytv.agent

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.widget.Button

object TvStyle {
    fun button(b: Button) {
        fun bg(focus: Boolean)=GradientDrawable().apply {
            setColor(Color.parseColor(if(focus) "#244537" else "#1A1E23")); cornerRadius=12*b.resources.displayMetrics.density
            setStroke((if(focus) 3 else 1)*b.resources.displayMetrics.density.toInt().coerceAtLeast(1),Color.parseColor(if(focus) "#53E0A0" else "#35404B"))
        }
        b.background=StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused),bg(true));addState(intArrayOf(android.R.attr.state_pressed),bg(true));addState(intArrayOf(),bg(false))
        }
        b.setTextColor(Color.parseColor("#F5F7FA")); b.textSize=16f
        b.minHeight=(52*b.resources.displayMetrics.density).toInt()
    }
}
