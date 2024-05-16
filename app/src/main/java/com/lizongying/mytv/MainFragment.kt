package com.lizongying.mytv

import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.marginBottom
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lizongying.mytv.api.YSP
import com.lizongying.mytv.databinding.MenuBinding
import com.lizongying.mytv.databinding.RowBinding
import com.lizongying.mytv.models.ProgramType
import com.lizongying.mytv.models.TVList
import com.lizongying.mytv.models.TVListViewModel
import com.lizongying.mytv.models.TVViewModel
import com.lizongying.mytv.requests.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainFragment : Fragment(), CardAdapter.ItemListener {

    private var itemPosition = 0

    private var rowList: MutableList<View> = mutableListOf()

    private var _binding: MenuBinding? = null
    private val binding get() = _binding!!

    var tvListViewModel = TVListViewModel()

    private var lastVideoUrl = ""

    private lateinit var application: MyTvApplication

    private lateinit var gestureDetector: GestureDetector

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        var context = requireContext()
        _binding = MenuBinding.inflate(inflater, container, false)

        application = requireActivity().applicationContext as MyTvApplication

        binding.menu.layoutParams.width = application.shouldWidthPx()
        binding.menu.layoutParams.height = application.shouldHeightPx()

        binding.container.setOnClickListener {
            hideSelf()
        }

        gestureDetector = GestureDetector(context, GestureListener())

        return binding.root
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            Log.i(TAG, "onSingleTapConfirmed")
            hideSelf()
            return true
        }
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
                        itemBinding.items,
                        this,
                        tvListViewModelCurrent,
                    )
                rowList.add(itemBinding.items)

                adapter.setItemListener(this)

                itemBinding.header.text = k
                itemBinding.items.tag = idx.toInt()
                itemBinding.items.adapter = adapter

                itemBinding.items.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        (activity as MainActivity).mainActive()
                    }
                })

                val itemDecoration = context?.let { ItemDecoration(it) }
                if (itemDecoration != null) {
                    itemBinding.items.addItemDecoration(itemDecoration)
                }

                if (SP.grid) {
                    itemBinding.items.layoutManager =
                        GridLayoutManager(context, 6)
                    itemBinding.items.layoutParams.height =
                        application.dp2Px(110 * ((tvListViewModelCurrent.size() + 6 - 1) / 6) + 5)
                } else {
                    itemBinding.items.layoutManager =
                        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                }

                val layoutParams = itemBinding.row.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.topMargin = application.dp2Px(11)
                itemBinding.row.layoutParams = layoutParams
                itemBinding.row.setOnClickListener {
                    hideSelf()
                }

                itemBinding.items.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                        gestureDetector.onTouchEvent(e)
                        return false
                    }

                    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                        TODO("Not yet implemented")
                    }

                    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                        TODO("Not yet implemented")
                    }
                })

                val layoutParamsHeader =
                    itemBinding.header.layoutParams as ViewGroup.MarginLayoutParams
                layoutParamsHeader.topMargin = application.px2Px(itemBinding.header.marginTop)
                layoutParamsHeader.bottomMargin = application.px2Px(itemBinding.header.marginBottom)
                layoutParamsHeader.marginStart = application.px2Px(itemBinding.header.marginStart)
                itemBinding.header.layoutParams = layoutParamsHeader
                itemBinding.header.textSize = application.px2PxFont(itemBinding.header.textSize)

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
                        if (tvViewModel.errInfo.value == "") {
                            (activity as? MainActivity)?.showPlayerFragment()
                            (activity as? MainActivity)?.hideErrorFragment()
                            (activity as? MainActivity)?.hideLoadingFragment()
                        } else {
                            (activity as? MainActivity)?.hidePlayerFragment()
                            (activity as? MainActivity)?.hideLoadingFragment()
                            (activity as? MainActivity)?.showErrorFragment(tvViewModel.errInfo.value.toString())
                        }
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
            (activity as MainActivity).fragmentReady(TAG)
        }
    }

    fun changeMenu() {
        if (SP.grid) {
            for (i in rowList) {
                if (i is RecyclerView) {
                    i.layoutManager = GridLayoutManager(context, 6)
                    i.layoutParams.height =
                        application.dp2Px(110 * (((i.adapter as CardAdapter).getItemCount() + 6 - 1) / 6) + 5)
                }
            }
        } else {
            for (i in rowList) {
                if (i is RecyclerView) {
                    i.layoutManager =
                        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                    i.layoutParams.height = application.dp2Px(110 + 5)
                }
            }
        }
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
        val row = tvViewModel.getRowPosition()

        for (i in rowList) {
            if (i.tag as Int != row) {
                ((i as RecyclerView).adapter as CardAdapter).focusable = false
                (i.adapter as CardAdapter).clear()
            } else {
                ((i as RecyclerView).adapter as CardAdapter).focusable = true
            }
        }

        (activity as MainActivity).mainActive()
    }

    override fun onItemClicked(tvViewModel: TVViewModel) {
        if (this.isHidden) {
            (activity as? MainActivity)?.switchMainFragment()
            return
        }

        if (itemPosition != tvViewModel.getTV().id) {
            itemPosition = tvViewModel.getTV().id
            tvListViewModel.setItemPosition(itemPosition)
            tvListViewModel.getTVViewModel(itemPosition)?.changed("menu")
            Log.i(TAG, "onItemClicked ${tvViewModel.getTV().id}")
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
        tvListViewModel.getTVViewModel(itemPosition)?.changed("init")

        tvListViewModel.tvListViewModel.value?.forEach { tvViewModel ->
            updateEPG(tvViewModel)
        }
    }

    fun play(itemPosition: Int) {
        view?.post {
            if (itemPosition > -1 && itemPosition < tvListViewModel.size()) {
                this.itemPosition = itemPosition
                tvListViewModel.setItemPosition(itemPosition)
                tvListViewModel.getTVViewModel(itemPosition)?.changed("num")
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
            tvListViewModel.getTVViewModel(itemPosition)?.changed("prev")
        }
    }

    fun next() {
        view?.post {
            itemPosition++
            if (itemPosition == tvListViewModel.size()) {
                itemPosition = 0
            }
            tvListViewModel.setItemPosition(itemPosition)
            tvListViewModel.getTVViewModel(itemPosition)?.changed("next")
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
        Log.i(TAG, "onHiddenChanged $hidden")
        super.onHiddenChanged(hidden)
        if (!hidden) {
            val tvModel = tvListViewModel.getTVViewModel(itemPosition)
            val rowPosition = tvModel?.getRowPosition()
            val itemPosition = tvModel?.getItemPosition()
            Log.i(TAG, "toPosition $rowPosition $itemPosition")
            for (i in rowList) {
                if (i.tag as Int == rowPosition) {
                    ((i as RecyclerView).adapter as CardAdapter).updateEPG()
                    (i.adapter as CardAdapter).focusable = true
                    (i.adapter as CardAdapter).toPosition(itemPosition!!)
                    break
                }
            }
        } else {
            view?.post {
                for (i in rowList) {
                    ((i as RecyclerView).adapter as CardAdapter).focusable = false
                }
            }
        }
    }

    override fun onResume() {
        Log.i(TAG, "onResume")
        super.onResume()
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