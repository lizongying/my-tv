package com.lizongying.mytv

import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.ImageView
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.Glide
import com.lizongying.mytv.models.TVViewModel

class CardPresenter(
    private val owner: LifecycleOwner,
) : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = object :
            ImageCardView(ContextThemeWrapper(parent.context, R.style.CustomImageCardTheme)) {}

        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val tvViewModel = item as TVViewModel
        val cardView = viewHolder.view as ImageCardView

        cardView.titleText = tvViewModel.title.value
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        cardView.tag = tvViewModel.videoUrl.value

        when (tvViewModel.title.value) {
            "CCTV8K 超高清" -> Glide.with(viewHolder.view.context)
                .load(R.drawable.cctv8k)
                .centerInside()
                .into(cardView.mainImageView)

            "天津卫视" -> Glide.with(viewHolder.view.context)
                .load(R.drawable.tianjin)
                .centerInside()
                .into(cardView.mainImageView)

            "新疆卫视" -> Glide.with(viewHolder.view.context)
                .load(R.drawable.xinjiang)
                .centerInside()
                .into(cardView.mainImageView)

            else -> Glide.with(viewHolder.view.context)
                .load(tvViewModel.logo.value)
                .centerInside()
                .into(cardView.mainImageView)
        }

        cardView.setBackgroundColor(Color.WHITE)
        cardView.setMainImageScaleType(ImageView.ScaleType.CENTER_INSIDE)

        tvViewModel.program.observe(owner) { _ ->
            val program = tvViewModel.getProgramOne()
            if (program != null) {
                cardView.contentText = program.name
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.mainImage = null
    }

    companion object {
        private const val TAG = "CardPresenter"
        private const val CARD_WIDTH = 300
        private const val CARD_HEIGHT = 101
    }
}