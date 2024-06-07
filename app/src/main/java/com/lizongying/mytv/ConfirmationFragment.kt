package com.lizongying.mytv

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.lizongying.mytv.databinding.ConfirmationBinding

class ConfirmationFragment(
    private val listener: ConfirmationListener,
    private val message: String,
    private val update: Boolean
) : DialogFragment() {
//
//    private var _binding: ConfirmationBinding? = null
//    private val binding get() = _binding!!
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = ConfirmationBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    fun updateProgress(progress: Int) {
//        binding.progressBar.progress = progress
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(message)
            if (update) {
                builder.setMessage("确定更新吗？")
                    .setPositiveButton(
                        "确定"
                    ) { _, _ ->
                        listener.onConfirm()
                    }
                    .setNegativeButton(
                        "取消"
                    ) { _, _ ->
                        listener.onCancel()
                    }
            } else {
                builder.setMessage("")
                    .setNegativeButton(
                        "好的"
                    ) { _, _ ->
                    }
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    interface ConfirmationListener {
        fun onConfirm()
        fun onCancel()
    }
}

