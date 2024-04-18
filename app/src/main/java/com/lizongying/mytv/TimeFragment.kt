package com.lizongying.mytv

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.lizongying.mytv.Utils.getDateFormat
import com.lizongying.mytv.databinding.TimeBinding

class TimeFragment : Fragment() {
    private var _binding: TimeBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler()
    private val delay: Long = 1000

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(TAG, "onCreateView")
        _binding = TimeBinding.inflate(inflater, container, false)
        (activity as MainActivity).fragmentReady("TimeFragment")
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (view?.visibility == View.VISIBLE) {
            handler.removeCallbacks(showRunnable)
            handler.postDelayed(showRunnable, 0)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(showRunnable)
    }

    private val showRunnable: Runnable = Runnable {
        run {
            binding.channelContent.text = getDateFormat("HH:mm")
            handler.postDelayed(showRunnable, delay)
        }
    }

    fun show() {
        view?.visibility = View.VISIBLE
        handler.removeCallbacks(showRunnable)
        handler.postDelayed(showRunnable, 0)
    }

    fun hide() {
        view?.visibility = View.GONE
        handler.removeCallbacks(showRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "TimeFragment"
    }
}