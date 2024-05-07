package com.lizongying.mytv

import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.marginEnd
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import com.lizongying.mytv.databinding.ChannelBinding
import com.lizongying.mytv.models.TVViewModel

class ChannelFragment : Fragment() {
    private var _binding: ChannelBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler()
    private val delay: Long = 5000
    private var channel = 0
    private var channelCount = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ChannelBinding.inflate(inflater, container, false)
        _binding!!.root.visibility = View.GONE

        val application = requireActivity().applicationContext as MyTvApplication

        binding.channel.layoutParams.width = application.px2Px(binding.channel.layoutParams.width)
        binding.channel.layoutParams.height = application.px2Px(binding.channel.layoutParams.height)

        val layoutParams = binding.channel.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin = application.px2Px(binding.channel.marginTop)
        layoutParams.marginEnd = application.px2Px(binding.channel.marginEnd)
        binding.channel.layoutParams = layoutParams

        binding.content.textSize = application.px2PxFont(binding.content.textSize)
        binding.time.textSize = application.px2PxFont(binding.time.textSize)

        binding.main.layoutParams.width = application.shouldWidthPx()
        binding.main.layoutParams.height = application.shouldHeightPx()

        (activity as MainActivity).fragmentReady(TAG)
        return binding.root
    }

    fun show(tvViewModel: TVViewModel) {
        handler.removeCallbacks(hideRunnable)
        handler.removeCallbacks(playRunnable)
        binding.content.text = (tvViewModel.getTV().id.plus(1)).toString()
        view?.visibility = View.VISIBLE
        handler.postDelayed(hideRunnable, delay)
    }

    fun show(channel: String) {
        if (channelCount > 1) {
            return
        }
        channelCount++
        this.channel = "${this.channel}$channel".toInt()
        handler.removeCallbacks(hideRunnable)
        handler.removeCallbacks(playRunnable)
        if (channelCount < 2) {
            binding.content.text = "${this.channel}"
            view?.visibility = View.VISIBLE
            handler.postDelayed(playRunnable, delay)
        } else {
            handler.postDelayed(playRunnable, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        if (view?.visibility == View.VISIBLE) {
            handler.postDelayed(hideRunnable, delay)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(hideRunnable)
        handler.removeCallbacks(playRunnable)
    }

    private val hideRunnable = Runnable {
        binding.content.text = ""
        view?.visibility = View.GONE
        channel = 0
        channelCount = 0
    }

    private val playRunnable = Runnable {
        (activity as MainActivity).play(channel - 1)
        binding.content.text = ""
        view?.visibility = View.GONE
        channel = 0
        channelCount = 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ChannelFragment"
    }
}