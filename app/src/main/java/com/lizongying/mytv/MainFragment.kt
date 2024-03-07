package com.lizongying.mytv

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lizongying.mytv.Utils.dpToPx
import com.lizongying.mytv.Utils.getDateTimestamp
import com.lizongying.mytv.databinding.RowBinding
import com.lizongying.mytv.databinding.ShowBinding
import com.lizongying.mytv.models.TVListViewModel
import com.lizongying.mytv.models.TVViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainFragment : Fragment(), CardAdapter.ItemListener {

    private var itemPosition = 0

    private var rowList: MutableList<View> = mutableListOf()

    private var _binding: ShowBinding? = null
    private val binding get() = _binding!!

    private var request = Request()

    var tvListViewModel = TVListViewModel()

    private var lastVideoUrl = ""

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var mUpdateProgramRunnable: UpdateProgramRunnable

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ShowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        activity?.let { request.initYSP(it) }

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
                        viewLifecycleOwner,
                        tvListViewModelCurrent,
                    )
                rowList.add(itemBinding.rowItems)

                adapter.setItemListener(this)

                itemBinding.rowHeader.text = k
                itemBinding.rowItems.tag = idx.toInt()
                itemBinding.rowItems.adapter = adapter
                itemBinding.rowItems.layoutManager =
                    GridLayoutManager(context, 6)
                itemBinding.rowItems.layoutParams.height =
                    dpToPx(92 * ((tvListViewModelCurrent.size() + 6 - 1) / 6))

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

                val layoutParams = itemBinding.row.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.topMargin = dpToPx(11F)
                itemBinding.row.layoutParams = layoutParams
                content.addView(itemBinding.row)

                idx++
            }

            mUpdateProgramRunnable = UpdateProgramRunnable()
            handler.post(mUpdateProgramRunnable)

            if (itemPosition >= tvListViewModel.size()) {
                itemPosition = 0
            }

            val tv = tvListViewModel.getTVViewModel(itemPosition)
            for (i in rowList) {
                if (i.tag as Int == tv?.getRowPosition()) {
                    ((i as RecyclerView).adapter as CardAdapter).defaultFocus = itemPosition
                    break
                }
            }

            tvListViewModel.setItemPosition(itemPosition)
            tvListViewModel.tvListViewModel.value?.forEach { tvViewModel ->
                tvViewModel.errInfo.observe(viewLifecycleOwner) { _ ->
                    if (tvViewModel.errInfo.value != null
                        && tvViewModel.id.value == itemPosition
                    ) {
                        Toast.makeText(context, tvViewModel.errInfo.value, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                tvViewModel.ready.observe(viewLifecycleOwner) { _ ->

                    // not first time && channel not change
                    if (tvViewModel.ready.value != null
                        && tvViewModel.id.value == itemPosition
                        && check(tvViewModel)
                    ) {
                        Log.i(TAG, "ready ${tvViewModel.title.value}")
                        (activity as? MainActivity)?.play(tvViewModel)
                    }
                }
                tvViewModel.change.observe(viewLifecycleOwner) { _ ->
                    if (tvViewModel.change.value != null) {
                        val title = tvViewModel.title.value
                        Log.i(TAG, "switch $title")
                        if (tvViewModel.pid.value != "") {
                            Log.i(TAG, "request $title")
                            lifecycleScope.launch(Dispatchers.IO) {
                                tvViewModel.let { request.fetchData(it) }
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
            (activity as MainActivity).fragmentReady()
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

    override fun onItemFocusChange(tvViewModel: TVViewModel, hasFocus: Boolean) {
        if (hasFocus) {
            tvListViewModel.setItemPositionCurrent(tvViewModel.id.value!!)

            val row = tvViewModel.getRowPosition()

            for (i in rowList) {
                if (i.tag as Int != row) {
                    ((i as RecyclerView).adapter as CardAdapter).clear()
                }
            }

            (activity as MainActivity).mainActive()
        }
    }

    override fun onItemClicked(tvViewModel: TVViewModel) {
        Log.i(TAG, "onItemClicked")
        if (this.isHidden) {
            (activity as? MainActivity)?.switchMainFragment()
            return
        }

        if (itemPosition != tvViewModel.id.value!!) {
            itemPosition = tvViewModel.id.value!!
            tvListViewModel.setItemPosition(itemPosition)
            tvListViewModel.getTVViewModel(itemPosition)?.changed()
        }
        (activity as? MainActivity)?.switchMainFragment()
    }

    fun setPosition() {
        val tvViewModel = tvListViewModel.getTVViewModel(itemPosition)
        rowList[tvViewModel!!.getRowPosition()].post {
            ((rowList[tvViewModel.getRowPosition()] as RecyclerView).layoutManager as GridLayoutManager).findViewByPosition(
                tvViewModel.getItemPosition()
            )?.requestFocus()
        }
    }

    fun setPosition(rowPosition: Int, itemPosition: Int) {
        rowList[rowPosition].post {
            ((rowList[rowPosition] as RecyclerView).layoutManager as GridLayoutManager).findViewByPosition(
                itemPosition
            )?.requestFocus()
        }

        ((rowList[rowPosition] as RecyclerView).adapter as CardAdapter).defaultFocus = itemPosition
    }

    fun check(tvViewModel: TVViewModel): Boolean {
        val title = tvViewModel.title.value
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

    fun updateProgram(tvViewModel: TVViewModel) {
        val timestamp = getDateTimestamp()
        if (timestamp - tvViewModel.programUpdateTime > 60) {
            if (tvViewModel.program.value!!.isEmpty()) {
                tvViewModel.programUpdateTime = timestamp
                request.fetchProgram(tvViewModel)
            } else {
                if (tvViewModel.program.value!!.last().et - timestamp < 600) {
                    tvViewModel.programUpdateTime = timestamp
                    request.fetchProgram(tvViewModel)
                }
            }
        }
    }

    inner class UpdateProgramRunnable : Runnable {
        override fun run() {
            tvListViewModel.tvListViewModel.value?.filter { it.programId.value != null && it.programId.value != "" }
                ?.forEach { tvViewModel ->
                    updateProgram(
                        tvViewModel
                    )
                }
            handler.postDelayed(this, 60000)
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
        if (::mUpdateProgramRunnable.isInitialized) {
            handler.removeCallbacks(mUpdateProgramRunnable)
        }
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