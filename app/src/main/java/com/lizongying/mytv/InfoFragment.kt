package com.lizongying.mytv

import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.lizongying.mytv.databinding.InfoBinding
import com.lizongying.mytv.models.TVViewModel

class InfoFragment : Fragment() {
    private var _binding: InfoBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler()
    private val delay: Long = 5000

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = InfoBinding.inflate(inflater, container, false)

        val application = requireActivity().applicationContext as MyTvApplication
        val width = application.getWidth()
        val height = application.getHeight()

        val ratio = 16f / 9f

        if (width.toFloat() / height < ratio) {
            val y =
                ((height - Resources.getSystem().displayMetrics.widthPixels / ratio) / 2).toInt()
            val originalLayoutParams = binding.info.layoutParams as ViewGroup.MarginLayoutParams
            originalLayoutParams.bottomMargin += y
            binding.info.layoutParams = originalLayoutParams
        }

        binding.info.layoutParams.width = application.px2Px(binding.info.layoutParams.width)
        binding.info.layoutParams.height = application.px2Px(binding.info.layoutParams.height)
        val layoutParams = binding.info.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.bottomMargin = application.px2Px(binding.info.marginBottom)
        binding.info.layoutParams = layoutParams
        binding.logo.layoutParams.width = application.px2Px(binding.logo.layoutParams.width)
        binding.logo.setPadding(application.px2Px(binding.logo.paddingTop))
        binding.main.layoutParams.width = application.px2Px(binding.main.layoutParams.width)
        binding.main.setPadding(application.px2Px(binding.main.paddingTop))
        val layoutParamsMain = binding.main.layoutParams as ViewGroup.MarginLayoutParams
        layoutParamsMain.leftMargin = application.px2Px(binding.main.marginLeft)
        binding.main.layoutParams = layoutParamsMain

        val layoutParamsDesc = binding.desc.layoutParams as ViewGroup.MarginLayoutParams
        layoutParamsDesc.topMargin = application.px2Px(binding.desc.marginTop)
        binding.desc.layoutParams = layoutParamsDesc
        binding.title.textSize = application.px2PxFont(binding.title.textSize)
        binding.desc.textSize = application.px2PxFont(binding.desc.textSize)

        _binding!!.root.visibility = View.GONE

        (activity as MainActivity).fragmentReady("InfoFragment")
        return binding.root
    }

    fun show(tvViewModel: TVViewModel) {
        binding.title.text = tvViewModel.getTV().title

        Glide.with(this)
            .load(tvViewModel.getTV().logo)
            .into(binding.logo)

        Log.i(TAG, "${tvViewModel.getTV().title} ${tvViewModel.epg.value}")
        val epg = tvViewModel.epg.value?.filter { it.beginTime < Utils.getDateTimestamp() }
        if (!epg.isNullOrEmpty()) {
            binding.desc.text = epg.last().title
        } else {
            binding.desc.text = ""
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