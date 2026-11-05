package com.lizongying.mytv.source

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.lizongying.mytv.MainActivity
import com.lizongying.mytv.R
import com.lizongying.mytv.databinding.SourceBinding
import com.lizongying.mytv.databinding.SourceItemBinding

class SourceFragment : DialogFragment() {

    private var _binding: SourceBinding? = null
    private val binding get() = _binding!!

    private var statuses: Map<String, SourceRepository.SourceStatus> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = SourceBinding.inflate(inflater, container, false)

        binding.sourceAdd.setOnClickListener { openEdit(null) }
        binding.sourceRefresh.setOnClickListener { refresh() }
        binding.sourceClose.setOnClickListener { dismiss() }

        statuses = (activity as? MainActivity)?.sourceStatuses().orEmpty().associateBy { it.id }
        render()
        return binding.root
    }

    private fun render() {
        if (_binding == null) {
            return
        }
        val container = binding.sourceList
        container.removeAllViews()

        val sources = SourceStore.list()
        if (sources.isEmpty()) {
            val empty = TextView(requireContext())
            empty.text = getString(R.string.source_empty)
            empty.setTextColor(0xFFEEEEEE.toInt())
            empty.textSize = 16f
            container.addView(empty)
            return
        }

        for (source in sources) {
            val item = SourceItemBinding.inflate(layoutInflater, container, false)
            item.itemName.text = source.name
            item.itemUrl.text = source.url
            item.itemStatus.text = statusText(statuses[source.id])
            item.itemEnabled.isChecked = source.enabled
            item.itemEnabled.setOnCheckedChangeListener { _, checked ->
                SourceStore.setEnabled(source.id, checked)
                refresh()
            }
            item.itemEdit.setOnClickListener { openEdit(source) }
            item.itemDelete.setOnClickListener {
                SourceStore.remove(source.id)
                statuses = statuses - source.id
                view?.post {
                    render()
                    refresh()
                }
            }
            container.addView(item.root)
        }
    }

    private fun statusText(status: SourceRepository.SourceStatus?): String {
        if (status == null) {
            return getString(R.string.source_status_unknown)
        }
        return if (status.ok) {
            getString(R.string.source_status_ok, status.count)
        } else {
            getString(R.string.source_status_fail, status.error, status.count)
        }
    }

    private fun refresh() {
        val activity = activity as? MainActivity ?: return
        activity.refreshSources { result ->
            statuses = result.associateBy { it.id }
            render()
        }
    }

    private fun openEdit(source: Source?) {
        val fragment = SourceEditFragment.newInstance(source?.id)
        fragment.onSaved = {
            render()
            refresh()
        }
        fragment.show(requireActivity().supportFragmentManager, SourceEditFragment.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SourceFragment"
    }
}
