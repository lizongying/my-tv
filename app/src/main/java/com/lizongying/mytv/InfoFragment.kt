package com.lizongying.mytv

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.lizongying.mytv.databinding.InfoBinding
import com.lizongying.mytv.models.TVViewModel

class InfoFragment : Fragment() {
    private var _binding: InfoBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler()
    private val delay: Long = 3000

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = InfoBinding.inflate(inflater, container, false)

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

        if (screenWidth / screenHeight < ratio) {
            val y = ((screenHeight - screenWidth / ratio) / 2).toInt()
            val originalLayoutParams = binding.info.layoutParams as ViewGroup.MarginLayoutParams
            originalLayoutParams.bottomMargin += y
            binding.info.layoutParams = originalLayoutParams
        }

        _binding!!.root.visibility = View.GONE

        (activity as MainActivity).fragmentReady("InfoFragment")
        return binding.root
    }

    fun show(tvViewModel: TVViewModel) {
        binding.textView.text = tvViewModel.getTV().title

        Glide.with(this)
            .load(tvViewModel.getTV().logo)
            .into(binding.infoLogo)

        Log.i(TAG, "${tvViewModel.getTV().title} ${tvViewModel.epg.value}")
        val epg = tvViewModel.epg.value?.filter { it.beginTime < Utils.getDateTimestamp() }
        if (!epg.isNullOrEmpty()) {
            binding.infoDesc.text = epg.last().title
        } else {
            binding.infoDesc.text = ""
        }

        handler.removeCallbacks(removeRunnable)
        view?.visibility = View.VISIBLE
        handler.postDelayed(removeRunnable, delay)
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
        view?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "InfoFragment"
    }
}