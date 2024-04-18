package com.lizongying.mytv

import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import androidx.core.view.updatePadding
import androidx.leanback.widget.ImageCardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lizongying.mytv.models.TVListViewModel
import com.lizongying.mytv.models.TVViewModel


class CardAdapter(
    private val recyclerView: RecyclerView,
    private val mainFragment: MainFragment,
    private var tvListViewModel: TVListViewModel,
) :
    RecyclerView.Adapter<CardAdapter.ViewHolder>() {

    private var listener: ItemListener? = null
    private var focused: View? = null

    var visiable = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardView = object :
            ImageCardView(ContextThemeWrapper(parent.context, R.style.CustomImageCardTheme)) {}
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        cardView.updatePadding(1, 0, 1, 0)
        return ViewHolder(cardView)
    }

    fun clear() {
        focused?.clearFocus()
        recyclerView.invalidate()
    }

    private fun startScaleAnimation(view: View, fromScale: Float, toScale: Float, duration: Long) {
        val scaleAnimation = ScaleAnimation(
            fromScale, toScale,
            fromScale, toScale,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        )
        scaleAnimation.duration = duration
        scaleAnimation.fillAfter = false
        view.startAnimation(scaleAnimation)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val item = tvListViewModel.getTVViewModel(position)

        val tvViewModel = item as TVViewModel
        val cardView = viewHolder.view as ImageCardView
        cardView.tag = tvViewModel

        val onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                listener?.onItemHasFocus(item)
                focused = cardView

                startScaleAnimation(cardView, 0.9f, 1.0f, 200)
//
//                if (mainFragment.shouldHasFocus(view.tag as TVViewModel)) {
//                }

//                if (visiable) {
//                    startScaleAnimation(cardView, 0.9f, 1.0f, 200)
//                } else {
//                    visiable = true
//                }
            }
        }

        cardView.onFocusChangeListener = onFocusChangeListener

        cardView.setOnClickListener { _ ->
            listener?.onItemClicked(item)
        }

        cardView.setOnKeyListener { _, keyCode, event: KeyEvent? ->
            if (event?.action == KeyEvent.ACTION_DOWN) {
                return@setOnKeyListener listener?.onKey(keyCode) ?: false
            }
            false
        }

        cardView.titleText = tvViewModel.getTV().title

        Glide.with(viewHolder.view.context)
            .load(tvViewModel.getTV().logo)
            .centerInside()
            .into(cardView.mainImageView)

        cardView.mainImageView.setBackgroundColor(Color.WHITE)
        cardView.setMainImageScaleType(ImageView.ScaleType.CENTER_INSIDE)

        val epg = tvViewModel.epg.value?.filter { it.beginTime < Utils.getDateTimestamp() }
        if (!epg.isNullOrEmpty()) {
            cardView.contentText = epg.last().title
        } else {
            cardView.contentText = ""
        }
    }

    override fun getItemCount() = tvListViewModel.size()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val view = itemView
    }

    fun toPosition(position: Int) {
        recyclerView.post {
            recyclerView.scrollToPosition(position)
            recyclerView.getChildAt(position)?.isSelected
            recyclerView.getChildAt(position)?.requestFocus()
        }
    }

    interface ItemListener {
        fun onItemHasFocus(tvViewModel: TVViewModel)
        fun onItemClicked(tvViewModel: TVViewModel)
        fun onKey(keyCode: Int): Boolean
    }

    fun setItemListener(listener: ItemListener) {
        this.listener = listener
    }

    companion object {
        private const val TAG = "CardAdapter"
    }
}

