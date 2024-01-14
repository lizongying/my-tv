package com.lizongying.mytv

import android.graphics.Color
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import androidx.leanback.widget.ImageCardView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lizongying.mytv.models.TVListViewModel
import com.lizongying.mytv.models.TVViewModel


class CardAdapter(private val owner: LifecycleOwner, private var tvListViewModel: TVListViewModel) :
    RecyclerView.Adapter<CardAdapter.ViewHolder>() {

    private var listener: ItemListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardView = object :
            ImageCardView(ContextThemeWrapper(parent.context, R.style.CustomImageCardTheme)) {}
        startScaleAnimation(cardView, 1.0f, 0.9f)
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true

        return ViewHolder(cardView)
    }

    private fun startScaleAnimation(view: View, fromScale: Float, toScale: Float) {
        val scaleAnimation = ScaleAnimation(
            fromScale, toScale,
            fromScale, toScale,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        )
        scaleAnimation.duration = 200
        scaleAnimation.fillAfter = true

        view.startAnimation(scaleAnimation)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val item = tvListViewModel.getTVViewModel(position)

        val tvViewModel = item as TVViewModel
        val cardView = viewHolder.view as ImageCardView

        cardView.setOnFocusChangeListener { _, hasFocus ->
            listener?.onItemFocusChange(item, hasFocus)
            if (hasFocus) {
                startScaleAnimation(cardView, 0.9f, 1.0f)
            } else {
                startScaleAnimation(cardView, 1.0f, 0.9f)
            }
        }

        cardView.setOnClickListener { _ ->
            listener?.onItemClicked(item)
        }

        cardView.titleText = tvViewModel.title.value
        cardView.tag = tvViewModel.videoUrl.value

        if (tvViewModel.logo.value != null) {
            if (tvViewModel.title.value == "CCTV8K 超高清") {
                Glide.with(viewHolder.view.context)
                    .load(R.drawable.cctv8k)
                    .centerInside()
                    .into(cardView.mainImageView)
            } else {
                Glide.with(viewHolder.view.context)
                    .load(tvViewModel.logo.value)
                    .centerInside()
                    .into(cardView.mainImageView)
            }

            cardView.mainImageView.setBackgroundColor(Color.WHITE)
            cardView.setMainImageScaleType(ImageView.ScaleType.CENTER_INSIDE)
        }

        tvViewModel.program.observe(owner) { _ ->
            val program = tvViewModel.getProgramOne()
            if (program != null) {
                cardView.contentText = program.name
            }
        }
    }

    override fun getItemCount() = tvListViewModel.size()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val view = itemView
    }

    interface ItemListener {
        fun onItemFocusChange(tvViewModel: TVViewModel, hasFocus: Boolean)
        fun onItemClicked(tvViewModel: TVViewModel)
    }

    fun setItemListener(listener: ItemListener) {
        this.listener = listener
    }

    companion object {
        private const val TAG = "CardAdapter"
    }
}

