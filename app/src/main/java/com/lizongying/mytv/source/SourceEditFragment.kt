package com.lizongying.mytv.source

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.lizongying.mytv.R
import com.lizongying.mytv.databinding.SourceEditBinding

class SourceEditFragment : DialogFragment() {

    private var _binding: SourceEditBinding? = null
    private val binding get() = _binding!!

    var onSaved: (() -> Unit)? = null

    private var sourceId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = SourceEditBinding.inflate(inflater, container, false)
        sourceId = arguments?.getString(ARG_ID).orEmpty()
        val existing = SourceStore.list().firstOrNull { it.id == sourceId }

        binding.editTitle.setText(
            if (existing == null) R.string.source_add else R.string.source_edit
        )
        binding.editName.setText(existing?.name.orEmpty())
        binding.editUrl.setText(existing?.url.orEmpty())

        binding.editOk.setOnClickListener { save(existing) }
        binding.editCancel.setOnClickListener { dismiss() }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.editUrl.requestFocus()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun save(existing: Source?) {
        val name = binding.editName.text.toString().trim()
        val url = binding.editUrl.text.toString().trim()

        val error: String? = if (existing == null) {
            SourceStore.add(name, url).exceptionOrNull()?.message
        } else {
            existing.name = name
            existing.url = url
            SourceStore.update(existing).exceptionOrNull()?.message
        }

        if (error == null) {
            onSaved?.invoke()
            dismiss()
        } else {
            binding.editError.text = error
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SourceEditFragment"
        private const val ARG_ID = "id"

        fun newInstance(id: String?): SourceEditFragment {
            val fragment = SourceEditFragment()
            fragment.arguments = Bundle().apply { putString(ARG_ID, id) }
            return fragment
        }
    }
}
