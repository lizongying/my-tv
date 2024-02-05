package com.lizongying.mytv

import android.os.Bundle
import android.os.Handler
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ChannelBinding.inflate(inflater, container, false)
        _binding!!.root.visibility = View.GONE
        (activity as MainActivity).fragmentReady()
        return binding.root
    }

    fun show(tvViewModel: TVViewModel) {
        handler.removeCallbacks(hideRunnable)
        handler.removeCallbacks(playRunnable)
        binding.channelContent.text = (tvViewModel.id.value?.plus(1)).toString()
        view?.visibility = View.VISIBLE
        handler.postDelayed(hideRunnable, delay)
    }

    fun show(channel: String) {
        this.channel = "${binding.channelContent.text}$channel".toInt()
        handler.removeCallbacks(hideRunnable)
        handler.removeCallbacks(playRunnable)
        if (binding.channelContent.text == "") {
            binding.channelContent.text = channel
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
    }

    private val playRunnable = Runnable {
        (activity as MainActivity).play(channel - 1)
        binding.channelContent.text = ""
        view?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ChannelFragment"
    }
}