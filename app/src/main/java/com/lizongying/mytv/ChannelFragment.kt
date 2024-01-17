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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ChannelBinding.inflate(inflater, container, false)
        (activity as MainActivity).fragmentReady()
        return binding.root
    }

    fun show(tvViewModel: TVViewModel) {
        binding.channelContent.text = tvViewModel.id.value.toString()
        handler.removeCallbacks(removeRunnable)
        view?.visibility = View.VISIBLE
        handler.postDelayed(removeRunnable, delay)
    }

    fun show(channel: String) {
        if (binding.channelContent.text == "") {
            binding.channelContent.text = channel
            handler.removeCallbacks(removeRunnable)
            view?.visibility = View.VISIBLE
            handler.postDelayed(removeRunnable, delay)
        } else {
            val ch = "${binding.channelContent.text}$channel".toInt()
            Log.i(TAG, "channel $ch")
            (activity as MainActivity).play(ch)
            binding.channelContent.text = ""
            view?.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(removeRunnable, delay)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(removeRunnable)
    }

    private val removeRunnable = Runnable {
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