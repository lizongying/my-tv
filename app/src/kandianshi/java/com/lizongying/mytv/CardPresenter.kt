package com.lizongying.mytv

import android.content.Context
import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.ImageView
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.lizongying.mytv.models.TVViewModel

class CardPresenter(
    private val context: Context,
) : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = object :
            ImageCardView(ContextThemeWrapper(parent.context, R.style.CustomImageCardTheme)) {}

        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        val tvViewModel = item as TVViewModel
        val cardView = viewHolder.view as ImageCardView

        cardView.titleText = tvViewModel.getTV().title
        cardView.findViewById<android.widget.TextView>(androidx.leanback.R.id.title_text)?.apply { setTextColor(Color.WHITE); textSize=18f }
        cardView.findViewById<android.widget.TextView>(androidx.leanback.R.id.content_text)?.setTextColor(Color.rgb(169,179,191))
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        cardView.tag = tvViewModel.videoUrl.value

        cardView.mainImageView?.let {
            ChannelLogo.bind(it, tvViewModel.getTV().title, tvViewModel.getTV().logo)
        }

        cardView.setBackgroundColor(Color.rgb(26,30,35))
        cardView.setInfoAreaBackgroundColor(Color.rgb(26,30,35))
        // Preserve Leanback's focus listener and channel-selection behavior.
        cardView.foreground = android.graphics.drawable.StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused),android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                cornerRadius=12*context.resources.displayMetrics.density
                setStroke((3*context.resources.displayMetrics.density).toInt(),Color.rgb(83,224,160))
            })
            addState(intArrayOf(),android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        }
        cardView.setMainImageScaleType(ImageView.ScaleType.CENTER_INSIDE)

//        cardView.setOnFocusChangeListener { v, hasFocus ->
//            run {
//                if (hasFocus) {
//                    if (v != null) {
//                        (v as ImageCardView).setInfoAreaBackgroundColor(context.resources.getColor(R.color.focus))
//                    }
//                } else {
//                    if (v != null) {
//                        (v as ImageCardView).setInfoAreaBackgroundColor(context.resources.getColor(R.color.ic_launcher_background))
//                    }
//                }
//            }
//        }

        val epg = tvViewModel.epg.value?.filter { it.beginTime < Utils.getDateTimestamp() }
        if (!epg.isNullOrEmpty()) {
            cardView.contentText = epg.last().title
        } else {
            cardView.contentText = ""
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.mainImageView?.let(ChannelLogo::clear)
    }

    companion object {
        private const val TAG = "CardPresenter"
        private const val CARD_WIDTH = 300
        private const val CARD_HEIGHT = 101
    }
}
