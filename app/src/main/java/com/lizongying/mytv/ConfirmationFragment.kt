package com.lizongying.mytv

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class ConfirmationFragment(
    private val listener: ConfirmationListener,
    private val message: String
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(message)
            if (message != "版本获取失败") {
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
                builder.setNegativeButton(
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

