package com.lizongying.mytv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import com.lizongying.mytv.databinding.ErrorBinding

class ErrorFragment : Fragment() {
    private var _binding: ErrorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ErrorBinding.inflate(inflater, container, false)

        val application = requireActivity().applicationContext as MyTvApplication

        binding.logo.layoutParams.width = application.px2Px(binding.logo.layoutParams.width)
        binding.logo.layoutParams.height = application.px2Px(binding.logo.layoutParams.height)

        val layoutParams = binding.msg.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin = application.px2Px(binding.msg.marginTop)
        binding.msg.layoutParams = layoutParams

        binding.msg.textSize = application.px2PxFont(binding.msg.textSize)

        (activity as MainActivity).fragmentReady(TAG)
        return binding.root
    }

    fun show(msg: String) {
        binding.msg.text = msg
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "ErrorFragment"
    }
}