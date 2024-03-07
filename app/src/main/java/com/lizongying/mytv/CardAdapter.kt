package com.lizongying.mytv

import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.KeyEvent
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


class CardAdapter(
    private val recyclerView: RecyclerView,
    private val owner: LifecycleOwner,
    private var tvListViewModel: TVListViewModel,
) :
    RecyclerView.Adapter<CardAdapter.ViewHolder>() {

    private var listener: ItemListener? = null
    private var focused: View? = null
    private var defaultFocused = false
    var defaultFocus: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardView = object :
            ImageCardView(ContextThemeWrapper(parent.context, R.style.CustomImageCardTheme)) {}
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
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

        if (!defaultFocused && item.id.value == defaultFocus) {
            cardView.requestFocus()
            defaultFocused = true
        }

        val onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            listener?.onItemFocusChange(item, hasFocus)

//            if (hasFocus && defaultFocus == item.id.value) {
            if (hasFocus) {
                focused = cardView
                startScaleAnimation(cardView, 0.9f, 1.0f, 200)
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

        cardView.titleText = tvViewModel.title.value

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

            "兵团卫视" -> Glide.with(viewHolder.view.context)
                .load(R.drawable.bingtuan)
                .centerInside()
                .into(cardView.mainImageView)

            "CETV1" -> Glide.with(viewHolder.view.context)
                .load(R.drawable.cetv1)
                .centerInside()
                .into(cardView.mainImageView)

            else -> Glide.with(viewHolder.view.context)
                .load(tvViewModel.logo.value)
                .centerInside()
                .into(cardView.mainImageView)
        }

        cardView.mainImageView.setBackgroundColor(Color.WHITE)
        cardView.setMainImageScaleType(ImageView.ScaleType.CENTER_INSIDE)

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
        fun onKey(keyCode: Int): Boolean
    }

    fun setItemListener(listener: ItemListener) {
        this.listener = listener
    }

    companion object {
        private const val TAG = "CardAdapter"
    }
}

