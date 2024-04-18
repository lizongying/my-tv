package com.lizongying.mytv

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lizongying.mytv.Utils.dpToPx
import com.lizongying.mytv.api.YSP
import com.lizongying.mytv.databinding.MenuBinding
import com.lizongying.mytv.databinding.RowBinding
import com.lizongying.mytv.models.ProgramType
import com.lizongying.mytv.models.TVListViewModel
import com.lizongying.mytv.models.TVViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainFragment : Fragment(), CardAdapter.ItemListener {

    private var itemPosition = 0

    private var rowList: MutableList<View> = mutableListOf()

    private var _binding: MenuBinding? = null
    private val binding get() = _binding!!

    var tvListViewModel = TVListViewModel()

    private var lastVideoUrl = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MenuBinding.inflate(inflater, container, false)

        binding.menu.setOnClickListener {
            hideSelf()
        }

        return binding.root
    }

    private fun hideSelf() {
        requireActivity().supportFragmentManager.beginTransaction()
            .hide(this)
            .commit()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        activity?.let { YSP.init(it) }

        itemPosition = SP.itemPosition

        view?.post {
            val content = binding.content

            var idx: Long = 0
            for ((k, v) in TVList.list) {
                val itemBinding: RowBinding =
                    RowBinding.inflate(layoutInflater, content, false)

                val tvListViewModelCurrent = TVListViewModel()
                for ((idx2, v1) in v.withIndex()) {
                    val tvViewModel = TVViewModel(v1)
                    tvViewModel.setRowPosition(idx.toInt())
                    tvViewModel.setItemPosition(idx2)
                    tvListViewModelCurrent.addTVViewModel(tvViewModel)
                    tvListViewModel.addTVViewModel(tvViewModel)
                }
                tvListViewModel.maxNum.add(v.size)

                val adapter =
                    CardAdapter(
                        itemBinding.rowItems,
                        this,
                        tvListViewModelCurrent,
                    )
                rowList.add(itemBinding.rowItems)

                adapter.setItemListener(this)

                itemBinding.rowHeader.text = k
                itemBinding.rowItems.tag = idx.toInt()
                itemBinding.rowItems.adapter = adapter

                itemBinding.rowItems.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        (activity as MainActivity).mainActive()
                    }
                })

                val itemDecoration = context?.let { GrayOverlayItemDecoration(it) }
                if (itemDecoration != null) {
                    itemBinding.rowItems.addItemDecoration(itemDecoration)
                }

                if (SP.grid) {
                    itemBinding.rowItems.layoutManager =
                        GridLayoutManager(context, 6)
                    itemBinding.rowItems.layoutParams.height =
                        dpToPx(92 * ((tvListViewModelCurrent.size() + 6 - 1) / 6))
                } else {
                    itemBinding.rowItems.layoutManager =
                        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                }

                val layoutParams = itemBinding.row.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.topMargin = dpToPx(11F)
                itemBinding.row.layoutParams = layoutParams
                itemBinding.row.setOnClickListener{
                    hideSelf()
                }
                content.addView(itemBinding.row)

                idx++
            }

            if (itemPosition >= tvListViewModel.size()) {
                itemPosition = 0
            }

            tvListViewModel.setItemPosition(itemPosition)
            tvListViewModel.tvListViewModel.value?.forEach { tvViewModel ->
                tvViewModel.errInfo.observe(viewLifecycleOwner) { _ ->
                    if (tvViewModel.errInfo.value != null
                        && tvViewModel.getTV().id == itemPosition
                    ) {
                        Toast.makeText(context, tvViewModel.errInfo.value, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                tvViewModel.ready.observe(viewLifecycleOwner) { _ ->

                    // not first time && channel not change
                    if (tvViewModel.ready.value != null
                        && tvViewModel.getTV().id == itemPosition
                        && check(tvViewModel)
                    ) {
                        Log.i(TAG, "ready ${tvViewModel.getTV().title}")
                        (activity as? MainActivity)?.play(tvViewModel)
                    }
                }
                tvViewModel.change.observe(viewLifecycleOwner) { _ ->
                    if (tvViewModel.change.value != null) {
                        val title = tvViewModel.getTV().title
                        Log.i(TAG, "switch $title")
                        if (tvViewModel.getTV().pid != "") {
                            Log.i(TAG, "request $title")
                            lifecycleScope.launch(Dispatchers.IO) {
                                tvViewModel.let { Request.fetchData(it) }
                            }
                            (activity as? MainActivity)?.showInfoFragment(tvViewModel)
                            setPosition(
                                tvViewModel.getRowPosition(), tvViewModel.getItemPosition()
                            )
                        } else {
                            if (check(tvViewModel)) {
                                // TODO lastVideoUrl
                                (activity as? MainActivity)?.play(tvViewModel)
                                (activity as? MainActivity)?.showInfoFragment(tvViewModel)
                                setPosition(
                                    tvViewModel.getRowPosition(), tvViewModel.getItemPosition()
                                )
                            }
                        }
                    }
                }
            }
            (activity as MainActivity).fragmentReady("MainFragment")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onKey(keyCode: Int): Boolean {
        if (this.isHidden) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    (activity as MainActivity).onKey(keyCode)
                    return true
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    (activity as MainActivity).onKey(keyCode)
                    return true
                }
            }
        }
        return false
    }

    override fun onItemHasFocus(tvViewModel: TVViewModel) {
        tvListViewModel.setItemPositionCurrent(tvViewModel.getTV().id)

        val row = tvViewModel.getRowPosition()

        for (i in rowList) {
            if (i.tag as Int != row) {
                ((i as RecyclerView).adapter as CardAdapter).clear()
            }
        }

        (activity as MainActivity).mainActive()
    }

    override fun onItemClicked(tvViewModel: TVViewModel) {
        Log.i(TAG, "onItemClicked")
        if (this.isHidden) {
            (activity as? MainActivity)?.switchMainFragment()
            return
        }

        if (itemPosition != tvViewModel.getTV().id) {
            itemPosition = tvViewModel.getTV().id
            tvListViewModel.setItemPosition(itemPosition)
            tvListViewModel.getTVViewModel(itemPosition)?.changed()
        }
        (activity as? MainActivity)?.switchMainFragment()
    }

    fun setPosition() {
        val tvViewModel = tvListViewModel.getTVViewModel(itemPosition) ?: return
        val rowPosition = tvViewModel.getRowPosition()
        val itemPosition = tvViewModel.getItemPosition()
        setPosition(rowPosition, itemPosition)
    }

    fun setPosition(rowPosition: Int, itemPosition: Int) {
        rowList[rowPosition].post {
            when (val layoutManager = (rowList[rowPosition] as RecyclerView).layoutManager) {
                is GridLayoutManager -> {
                    layoutManager.findViewByPosition(
                        itemPosition
                    )?.requestFocus()
                }

                is LinearLayoutManager -> {
                    layoutManager.findViewByPosition(
                        itemPosition
                    )?.requestFocus()
                }
            }
        }
    }

    fun check(tvViewModel: TVViewModel): Boolean {
        val title = tvViewModel.getTV().title
        val videoUrl = tvViewModel.videoIndex.value?.let { tvViewModel.videoUrl.value?.get(it) }
        if (videoUrl == null || videoUrl == "") {
            Log.e(TAG, "$title videoUrl is empty")
            return false
        }

        if (videoUrl == lastVideoUrl) {
            Log.e(TAG, "$title videoUrl is duplication")
            return false
        }

        return true
    }

    fun fragmentReady() {
//            request.fetchPage()
        tvListViewModel.getTVViewModel(itemPosition)?.changed()

        tvListViewModel.tvListViewModel.value?.forEach { tvViewModel ->
            updateEPG(tvViewModel)
        }
    }

    fun play(itemPosition: Int) {
        view?.post {
            if (itemPosition > -1 && itemPosition < tvListViewModel.size()) {
                this.itemPosition = itemPosition
                tvListViewModel.setItemPosition(itemPosition)
                tvListViewModel.getTVViewModel(itemPosition)?.changed()
            } else {
                Toast.makeText(context, "频道不存在", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun prev() {
        view?.post {
            itemPosition--
            if (itemPosition == -1) {
                itemPosition = tvListViewModel.size() - 1
            }
            tvListViewModel.setItemPosition(itemPosition)
            tvListViewModel.getTVViewModel(itemPosition)?.changed()
        }
    }

    fun next() {
        view?.post {
            itemPosition++
            if (itemPosition == tvListViewModel.size()) {
                itemPosition = 0
            }
            tvListViewModel.setItemPosition(itemPosition)
            tvListViewModel.getTVViewModel(itemPosition)?.changed()
        }
    }

    private fun updateEPG(tvViewModel: TVViewModel) {
        when (tvViewModel.getTV().programType) {
            ProgramType.Y_PROTO -> {
                Request.fetchYProtoEPG(tvViewModel)
            }

            ProgramType.Y_JCE -> {
                Request.fetchYJceEPG(tvViewModel)
            }

            ProgramType.F -> {
                Request.fetchFEPG(tvViewModel)
            }
        }
    }

    fun shouldHasFocus(tvModel: TVViewModel): Boolean {
        return tvModel == tvListViewModel.getTVViewModel(itemPosition)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            val tvModel = tvListViewModel.getTVViewModel(itemPosition)
            val rowPosition = tvModel?.getRowPosition()
            val itemPosition = tvModel?.getItemPosition()
            Log.i(TAG, "toPosition $rowPosition $itemPosition")
            for (i in rowList) {
                if (i.tag as Int == rowPosition) {
                    ((i as RecyclerView).adapter as CardAdapter).toPosition(itemPosition!!)
                    break
                }
            }
        } else {
            view?.post {
//                for (i in rowList) {
//                    ((i as RecyclerView).adapter as CardAdapter).visiable = false
//                }
            }
        }
    }

    override fun onResume() {
        Log.i(TAG, "onResume")
        super.onResume()
    }

    override fun onStop() {
        Log.i(TAG, "onStop")
        super.onStop()
        SP.itemPosition = itemPosition
        Log.i(TAG, "$POSITION $itemPosition saved")
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        super.onDestroy()
    }

    override fun onDestroyView() {
        Log.i(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "MainFragment"
        private const val POSITION = "position"
    }
}