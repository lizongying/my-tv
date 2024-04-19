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
            val originalLayoutParams = binding.time.layoutParams as ViewGroup.MarginLayoutParams
            originalLayoutParams.rightMargin += x
            binding.time.layoutParams = originalLayoutParams
        }

        if (screenWidth / screenHeight < ratio) {
            val y = ((screenHeight - screenWidth / ratio) / 2).toInt()
            val originalLayoutParams = binding.time.layoutParams as ViewGroup.MarginLayoutParams
            originalLayoutParams.topMargin += y
            binding.time.layoutParams = originalLayoutParams
        }

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
            binding.content.text = getDateFormat("HH:mm")
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