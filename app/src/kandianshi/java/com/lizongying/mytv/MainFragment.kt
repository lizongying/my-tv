package com.lizongying.mytv

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ListRowPresenter.SelectItemViewHolderTask
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.lifecycleScope
import com.lizongying.mytv.models.TVListViewModel
import com.lizongying.mytv.models.TVViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle

class MainFragment : BrowseSupportFragment() {

    private var itemPosition = 0

    private var rowsAdapter: ArrayObjectAdapter? = null

    var tvListViewModel = TVListViewModel()
    private var refreshing = false
    private var seenRevision = 0L

    private var lastVideoUrl = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        headersState = HEADERS_DISABLED
    }

//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        val rootView = super.onCreateView(inflater, container, savedInstanceState)
//        rootView?.setOnClickListener {
//            Log.i(TAG, "main on click")
//            fragmentManager!!.beginTransaction().hide(this).commit()
//        }
//        mainFragment.view?.setOnClickListener {
//            Log.i(TAG, "mainFragment on click")
//            fragmentManager!!.beginTransaction().hide(this).commit()
//        }
//        getRowsSupportFragment().view?.setOnClickListener {
//            Log.i(TAG, "getRowsSupportFragment on click")
//            fragmentManager!!.beginTransaction().hide(this).commit()
//        }
//
//
//        return rootView
//    }

    override fun onStart() {
        Log.i(TAG, "onStart")
        super.onStart()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            val context = requireContext().applicationContext
            val channels = withContext(Dispatchers.IO) { TVList.load(context) }
            loadRows(channels)
            setupEventListeners()
            observeChannels()
            (activity as? MainActivity)?.fragmentReady("MainFragment")
            refreshSources()
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    val revision = context.getSharedPreferences("agent_state", android.content.Context.MODE_PRIVATE).getLong("revision", 0)
                    if (revision != seenRevision) {
                        seenRevision = revision
                        reloadMaintainedChannels()
                    }
                    delay(10000L)
                }
            }
        }
    }

    private fun observeChannels() {
        tvListViewModel.tvListViewModel.value?.forEach { model ->
            model.change.observe(viewLifecycleOwner) {
                if (model.getTV().id == itemPosition && check(model)) {
                    (activity as? MainActivity)?.play(model)
                    (activity as? MainActivity)?.showInfoFragment(model)
                    setSelectedPosition(model.getRowPosition(), false,
                        SelectItemViewHolderTask(model.getItemPosition()))
                }
            }
        }
    }

    fun refreshSources(manual: Boolean = false) {
        if (manual) {
            com.lizongying.mytv.agent.AgentScheduler.runNow(requireContext().applicationContext)
            Toast.makeText(context, "更新与检测已安排，可在 AI 维护页查看进度", Toast.LENGTH_LONG).show()
            return
        }
        if (!com.lizongying.mytv.agent.AgentConfigStore(requireContext()).load().enabled) return
        if (refreshing) return
        refreshing = true
        if (manual) Toast.makeText(context, "正在更新直播源", Toast.LENGTH_SHORT).show()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = TVList.refresh(requireContext().applicationContext, manual)
                if (result.updated > 0) {
                    loadRows(result.channels)
                    observeChannels()
                    tvListViewModel.getTVViewModel(itemPosition)?.changed()
                }
                if (manual) Toast.makeText(context,
                    if (result.updated > 0) "直播源已更新，共 " + result.channels.size + " 个频道"
                    else "更新暂不可用，已保留现有频道", Toast.LENGTH_LONG).show()
            } finally { refreshing = false }
        }
    }

    private fun reloadMaintainedChannels() {
        if (rowsAdapter == null || refreshing) return
        refreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val channels = withContext(Dispatchers.IO) { TVList.load(requireContext().applicationContext) }
                loadRows(channels)
                if (channels.isNotEmpty()) {
                    observeChannels()
                    tvListViewModel.getTVViewModel(itemPosition)?.changed()
                } else {
                    (activity as? MainActivity)?.clearPlayback()
                    Toast.makeText(context, "当前没有可用频道，请在手机配置页检查屏蔽列表与来源", Toast.LENGTH_LONG).show()
                }
            } finally { refreshing = false }
        }
    }

    fun toLastPosition() {
        if (tvListViewModel.maxNum.isEmpty()) return
        setSelectedPosition(
            selectedPosition, false,
            SelectItemViewHolderTask(tvListViewModel.maxNum[selectedPosition] - 1)
        )
    }

    fun toFirstPosition() {
        if (tvListViewModel.maxNum.isEmpty()) return
        setSelectedPosition(
            selectedPosition, false,
            SelectItemViewHolderTask(0)
        )
    }

    override fun startHeadersTransition(withHeaders: Boolean) {
    }

    private fun loadRows(channels: List<PlaylistChannel>) {
        val selectedUrl = (activity as? MainActivity)?.currentStreamUrl().orEmpty()
        val selectedTitle = tvListViewModel.getTVViewModel(itemPosition)?.getTV()?.title ?: SP.channelTitle
        tvListViewModel.tvListViewModel.value?.forEach { it.change.removeObservers(viewLifecycleOwner) }
        tvListViewModel = TVListViewModel()
        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

        val cardPresenter = CardPresenter(context!!)

        var idx: Long = 0
        for ((k, v) in TVList.grouped(channels)) {
            val listRowAdapter = ArrayObjectAdapter(cardPresenter)
            for ((idx2, v1) in v.withIndex()) {
                val tvViewModel = TVViewModel(v1)
                tvViewModel.setRowPosition(idx.toInt())
                tvViewModel.setItemPosition(idx2)
                tvListViewModel.addTVViewModel(tvViewModel)
                listRowAdapter.add(tvViewModel)
            }
            tvListViewModel.maxNum.add(v.size)
            val header = HeaderItem(idx, k)
            rowsAdapter!!.add(ListRow(header, listRowAdapter))
            idx++
        }

        adapter = rowsAdapter

        itemPosition = tvListViewModel.tvListViewModel.value?.indexOfFirst {
            it.getTV().title == selectedTitle
        }?.takeIf { it >= 0 } ?: SP.itemPosition.coerceAtLeast(0)
        if (itemPosition >= tvListViewModel.size()) {
            itemPosition = 0
        }
        tvListViewModel.setItemPosition(itemPosition)
        tvListViewModel.getTVViewModel(itemPosition)?.let { model ->
            val kept = model.videoUrl.value.orEmpty().indexOf(selectedUrl)
            if (kept >= 0) model.setVideoIndex(kept)
        }
    }

    fun prevSource() {
        view?.post {
            val tvViewModel = tvListViewModel.getTVViewModel(itemPosition)
            if (tvViewModel != null) {
                if (tvViewModel.videoUrl.value!!.size > 1) {
                    val videoIndex = tvViewModel.videoIndex.value?.minus(1)
                    if (videoIndex == -1) {
                        tvViewModel.setVideoIndex(tvViewModel.videoUrl.value!!.size - 1)
                    } else {
                        tvViewModel.setVideoIndex(videoIndex ?: 0)
                    }
                    tvViewModel.changed()
                }
            }
        }
    }

    fun nextSource() {
        view?.post {
            val tvViewModel = tvListViewModel.getTVViewModel(itemPosition)
            if (tvViewModel != null) {
                if (tvViewModel.videoUrl.value!!.size > 1) {
                    val videoIndex = tvViewModel.videoIndex.value?.plus(1)
                    if (videoIndex == tvViewModel.videoUrl.value!!.size) {
                        tvViewModel.setVideoIndex(0)
                    } else {
                        tvViewModel.setVideoIndex(videoIndex ?: 0)
                    }
                    tvViewModel.changed()
                }
            }
        }
    }

    private fun setupEventListeners() {
        onItemViewClickedListener = ItemViewClickedListener()
        onItemViewSelectedListener = ItemViewSelectedListener()
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
        ) {
            if (item is TVViewModel) {
                if (itemPosition != item.getTV().id) {
                    itemPosition = item.getTV().id
                    tvListViewModel.setItemPosition(itemPosition)
                    tvListViewModel.getTVViewModel(itemPosition)?.changed()
                }
                (activity as? MainActivity)?.switchMainFragment()
            }
        }
    }

    private inner class ItemViewSelectedListener : OnItemViewSelectedListener {
        override fun onItemSelected(
            itemViewHolder: Presenter.ViewHolder?, item: Any?,
            rowViewHolder: RowPresenter.ViewHolder, row: Row
        ) {
            if (item is TVViewModel) {
                tvListViewModel.setItemPositionCurrent(item.getTV().id)
                (activity as MainActivity).mainActive()
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
            if (tvListViewModel.size() == 0) return@post
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
            if (tvListViewModel.size() == 0) return@post
            itemPosition++
            if (itemPosition == tvListViewModel.size()) {
                itemPosition = 0
            }
            tvListViewModel.setItemPosition(itemPosition)
            tvListViewModel.getTVViewModel(itemPosition)?.changed()
        }
    }

    override fun onResume() {
        Log.i(TAG, "onResume")
        super.onResume()
        reloadMaintainedChannels()
    }

    override fun onStop() {
        Log.i(TAG, "onStop")
        super.onStop()
        SP.itemPosition = itemPosition
        SP.channelTitle = tvListViewModel.getTVViewModel(itemPosition)?.getTV()?.title.orEmpty()
        Log.i(TAG, "$POSITION $itemPosition saved")
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MainFragment"
        private const val POSITION = "position"
    }
}
