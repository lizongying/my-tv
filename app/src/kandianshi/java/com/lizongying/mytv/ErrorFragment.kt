package com.lizongying.mytv

import android.view.View
import androidx.core.content.ContextCompat
import androidx.leanback.app.ErrorSupportFragment

class ErrorFragment : ErrorSupportFragment() {

    internal fun setErrorContent(message: String) {
        imageDrawable =
            ContextCompat.getDrawable(context!!, androidx.leanback.R.drawable.lb_ic_sad_cloud)
        this.message = message
        setDefaultBackground(TRANSLUCENT)
        backgroundDrawable = ContextCompat.getDrawable(
            context!!,
            R.color.black
        )

        buttonText = resources.getString(R.string.dismiss_error)
        buttonClickListener = View.OnClickListener {
            fragmentManager!!.beginTransaction().remove(this@ErrorFragment).commit()
        }
    }

    companion object {
        private const val TRANSLUCENT = false
    }
}