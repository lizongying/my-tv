package com.lizongying.mytv

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.lizongying.mytv.databinding.ChannelBinding
import com.lizongying.mytv.models.TVViewModel

class ChannelFragment : Fragment() {
    private var _binding: ChannelBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler()
    private val delay: Long = 3000
    private var channel = 0
    private var channelCount = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ChannelBinding.inflate(inflater, container, false)
        _binding!!.root.visibility = View.GONE

        val activity = requireActivity()
        val application = activity.applicationContext as MyApplication
        val displayMetrics = application.getDisplayMetrics()

        displayMetrics.density

        var screenWidth = displayMetrics.widthPixels
        var screenHeight = displayMetrics.heightPixels
        if (screenHeight > screenWidth) {
            screenWidth = displayMetrics.heightPixels
            screenHeight = displayMetrics.widthPixels
        }

        val ratio = 16f / 9f

        if (screenWidth / screenHeight > ratio) {
            val x = ((screenWidth - screenHeight * ratio) / 2).toInt()
            val originalLayoutParams =
                binding.channelFragment.layoutParams as ViewGroup.MarginLayoutParams
            originalLayoutParams.rightMargin += x
            binding.channelFragment.layoutParams = originalLayoutParams
        }

        if (screenWidth / screenHeight < ratio) {
            val y = ((screenHeight - screenWidth / ratio) / 2).toInt()
            val originalLayoutParams =
                binding.channelFragment.layoutParams as ViewGroup.MarginLayoutParams
            originalLayoutParams.topMargin += y
            binding.channelFragment.layoutParams = originalLayoutParams
        }

        (activity as MainActivity).fragmentReady("ChannelFragment")
        return binding.root
    }

    fun show(tvViewModel: TVViewModel) {
        handler.removeCallbacks(hideRunnable)
        handler.removeCallbacks(playRunnable)
        binding.channelContent.text = (tvViewModel.getTV().id.plus(1)).toString()
        view?.visibility = View.VISIBLE
        handler.postDelayed(hideRunnable, delay)
    }

    fun show(channel: String) {
        if (channelCount > 1) {
            return
        }
        channelCount++
        Log.i(TAG, "channelCount ${channelCount}")
        this.channel = "${this.channel}$channel".toInt()
        Log.i(TAG, "this.channel ${this.channel}")
        handler.removeCallbacks(hideRunnable)
        handler.removeCallbacks(playRunnable)
        if (channelCount < 2) {
            binding.channelContent.text = "${this.channel}"
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
        binding.channelContent.text = ""
        view?.visibility = View.GONE
        channel = 0
        channelCount = 0
        Log.i(TAG, "hideRunnable")
    }

    private val playRunnable = Runnable {
        (activity as MainActivity).play(channel - 1)
        binding.channelContent.text = ""
        view?.visibility = View.GONE
        channel = 0
        channelCount = 0
        Log.i(TAG, "playRunnable")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ChannelFragment"
    }
}