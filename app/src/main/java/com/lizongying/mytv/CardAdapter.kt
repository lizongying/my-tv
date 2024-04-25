package com.lizongying.mytv

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.ScaleAnimation
import androidx.core.view.marginTop
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lizongying.mytv.databinding.CardBinding
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

    var focusable = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = CardBinding.inflate(inflater, parent, false)

        val application = mainFragment.requireActivity().applicationContext as MyTvApplication

        binding.card.layoutParams.width = application.px2Px(binding.card.layoutParams.width)
        binding.card.layoutParams.height = application.px2Px(binding.card.layoutParams.height)
        binding.icon.layoutParams.height = application.px2Px(binding.icon.layoutParams.height)
        binding.icon.setPadding(application.px2Px(binding.icon.paddingTop))
        binding.main.layoutParams.height = application.px2Px(binding.main.layoutParams.height)
        binding.main.setPadding(application.px2Px(binding.main.paddingTop))
        binding.title.textSize = application.px2PxFont(binding.title.textSize)
        binding.desc.textSize = application.px2PxFont(binding.desc.textSize)
        val layoutParams = binding.desc.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin = application.px2Px(binding.desc.marginTop)
        binding.desc.layoutParams = layoutParams

        val cardView = binding.root
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        return ViewHolder(cardView, binding)
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
        val cardView = viewHolder.view
        cardView.tag = tvViewModel

        val onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                listener?.onItemHasFocus(item)
                focused = cardView

                if (focusable) {
                    startScaleAnimation(cardView, 0.9f, 1.0f, 200)
                    viewHolder.focus(true)
                }
            } else {
                viewHolder.focus(false)
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

        viewHolder.binding.title.text = tvViewModel.getTV().title

        viewHolder.binding.icon.let {
            Glide.with(viewHolder.view.context)
                .load(tvViewModel.getTV().logo)
                .centerInside()
                .into(it)
        }

        val epg = tvViewModel.epg.value?.filter { it.beginTime < Utils.getDateTimestamp() }
        if (!epg.isNullOrEmpty()) {
            viewHolder.binding.desc.text = epg.last().title
        } else {
            viewHolder.binding.desc.text = ""
        }
    }

    override fun getItemCount() = tvListViewModel.size()

    class ViewHolder(itemView: View, var binding: CardBinding) : RecyclerView.ViewHolder(itemView) {
        val view = itemView

        fun focus(hasFocus: Boolean) {
            if (hasFocus) {
                binding.main.setBackgroundResource(R.drawable.rounded_light_bottom)
            } else {
                binding.main.setBackgroundResource(R.drawable.rounded_dark_bottom)
            }
        }
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

