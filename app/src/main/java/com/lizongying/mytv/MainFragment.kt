package com.lizongying.mytv

import android.os.Bundle
import android.util.Log
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
import com.lizongying.mytv.source.ChannelListBuilder
import com.lizongying.mytv.source.SourceRepository
import com.lizongying.mytv.source.SourceStore
import kotlinx.coroutines.launch

class MainFragment : BrowseSupportFragment() {

    private var itemPosition = 0

    private var rowsAdapter: ArrayObjectAdapter? = null

    var tvListViewModel = TVListViewModel()

    var lastStatuses: List<SourceRepository.SourceStatus> = emptyList()
        private set

    private var lastVideoUrl = ""

    private var playingKey: String? = null

    private var rowsSignature: List<String> = emptyList()

    private var refreshed = false

    private val repository: SourceRepository by lazy {
        SourceRepository(requireContext().applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        headersState = HEADERS_DISABLED
    }

    override fun onStart() {
        Log.i(TAG, "onStart")
        super.onStart()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        bindRows(repository.loadCachedRows())
        setupEventListeners()
        refresh()
        (activity as MainActivity).fragmentReady("MainFragment")
    }

    override fun startHeadersTransition(withHeaders: Boolean) {
    }

    private fun bindRows(rows: Map<String, List<TV>>?) {
        val list = rows.orEmpty()
        val signature = ChannelListBuilder.signature(list)
        if (signature == rowsSignature) {
            (activity as? MainActivity)?.showEmpty(emptyMessage(list))
            return
        }

        val previousKey = tvListViewModel.getTVViewModel(itemPosition)?.getTV()?.let { keyOf(it) }
        releaseObservers()

        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        tvListViewModel = TVListViewModel()
        val cardPresenter = CardPresenter(requireContext())

        var idx: Long = 0
        for ((k, v) in list) {
            val listRowAdapter = ArrayObjectAdapter(cardPresenter)
            for ((idx2, v1) in v.withIndex()) {
                val tvViewModel = TVViewModel(v1)
                tvViewModel.setRowPosition(idx.toInt())
                tvViewModel.setItemPosition(idx2)
                tvListViewModel.addTVViewModel(tvViewModel)
                listRowAdapter.add(tvViewModel)
            }
            tvListViewModel.maxNum.add(v.size)
            rowsAdapter!!.add(ListRow(HeaderItem(idx, k), listRowAdapter))
            idx++
        }

        adapter = rowsAdapter
        rowsSignature = signature

        val restored = previousKey?.let { indexOf(it) } ?: -1
        itemPosition = if (restored >= 0) {
            restored
        } else {
            SP.itemPosition.coerceIn(0, (tvListViewModel.size() - 1).coerceAtLeast(0))
        }
        tvListViewModel.setItemPosition(itemPosition)

        val playing = playingKey
        if (playing != null && indexOf(playing) < 0) {
            playingKey = null
            (activity as? MainActivity)?.stopPlay()
        } else if (playingKey == null
            && tvListViewModel.size() > 0
            && (activity as? MainActivity)?.isReady() == true
        ) {
            tvListViewModel.getTVViewModel(itemPosition)?.changed()
        }

        observeAll()
        (activity as? MainActivity)?.showEmpty(emptyMessage(list))
    }

    fun refresh(onDone: ((List<SourceRepository.SourceStatus>) -> Unit)? = null) {
        if (!isAdded) {
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.refresh()
            lastStatuses = result.statuses
            refreshed = true
            bindRows(result.rows)
            onDone?.invoke(result.statuses)
        }
    }

    private fun observeAll() {
        tvListViewModel.tvListViewModel.value?.forEach { tvViewModel ->
            observe(tvViewModel)
        }
    }

    private fun observe(tvViewModel: TVViewModel) {
        tvViewModel.change.observe(viewLifecycleOwner) { _ ->
            if (tvViewModel.change.value != null) {
                val title = tvViewModel.getTV().title
                Log.i(TAG, "switch $title")
                if (check(tvViewModel)) {
                    playingKey = keyOf(tvViewModel.getTV())
                    (activity as? MainActivity)?.play(tvViewModel)
                    (activity as? MainActivity)?.showInfoFragment(tvViewModel)
                }
                setSelectedPosition(
                    tvViewModel.getRowPosition(), true,
                    SelectItemViewHolderTask(tvViewModel.getItemPosition())
                )
            }
        }
    }

    private fun releaseObservers() {
        tvListViewModel.tvListViewModel.value?.forEach { tvViewModel ->
            tvViewModel.change.removeObservers(viewLifecycleOwner)
        }
    }

    private fun keyOf(tv: TV): String {
        return "${tv.channel}\u0000${tv.title}\u0000${tv.videoUrl.firstOrNull().orEmpty()}"
    }

    private fun indexOf(key: String): Int {
        for (i in 0 until tvListViewModel.size()) {
            val tv = tvListViewModel.getTVViewModel(i)?.getTV() ?: continue
            if (keyOf(tv) == key) {
                return i
            }
        }
        return -1
    }

    private fun emptyMessage(list: Map<String, List<TV>>): String {
        if (list.isNotEmpty()) {
            return ""
        }
        return when {
            SourceStore.enabled().isEmpty() -> getString(R.string.empty_no_source)
            refreshed -> getString(R.string.empty_load_failed)
            else -> getString(R.string.empty_loading)
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
        if (tvListViewModel.size() == 0) {
            return
        }
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
            if (tvListViewModel.size() == 0) {
                return@post
            }
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
            if (tvListViewModel.size() == 0) {
                return@post
            }
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

    companion object {
        private const val TAG = "MainFragment"
        private const val POSITION = "position"
    }
}
