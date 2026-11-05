package com.lizongying.mytv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.lizongying.mytv.databinding.EmptyBinding

class EmptyFragment : Fragment() {

    private var _binding: EmptyBinding? = null
    private val binding get() = _binding!!

    var message: String = ""
        set(value) {
            field = value
            _binding?.let { it.emptyText.text = value }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = EmptyBinding.inflate(inflater, container, false)
        binding.emptyText.text = message
        binding.emptySetting.setOnClickListener {
            (activity as? MainActivity)?.openSources()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.emptySetting.requestFocus()
    }

    fun focusSetting() {
        _binding?.emptySetting?.requestFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
