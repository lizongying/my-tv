package com.lizongying.mytv

import android.content.Context
import android.content.SharedPreferences
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainFragment : BrowseSupportFragment() {
    var itemPosition: Int = 0

    private var request: Request? = null

    private var rowsAdapter: ArrayObjectAdapter? = null

    var tvListViewModel = TVListViewModel()

    private var sharedPref: SharedPreferences? = null

    private var lastVideoUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        headersState = HEADERS_DISABLED
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)

        request = activity?.let { Request(it) }
        loadRows()
        setupEventListeners()

        view?.post {
//            request?.fetchPage()
//            tvListViewModel.getTVViewModel(0)?.let { request?.fetchProgram(it) }
        }
        tvListViewModel.getTVListViewModel().value?.forEach { tvViewModel ->
            tvViewModel.ready.observe(viewLifecycleOwner) { _ ->

                // not first time && channel not change
                if (tvViewModel.ready.value != null
                    && tvViewModel.id.value == itemPosition
                    && check(tvViewModel)
                ) {
                    Log.i(TAG, "ready ${tvViewModel.title.value}")
                    (activity as? MainActivity)?.play(tvViewModel)
//                        (activity as? MainActivity)?.switchInfoFragment(item)
                }
            }
            tvViewModel.change.observe(viewLifecycleOwner) { _ ->
                if (tvViewModel.change.value != null && check(tvViewModel)) {
                    val title = tvViewModel.title.value
                    Log.i(TAG, "switch $title")
                    if (tvViewModel.ysp() != null) {
                        Log.i(TAG, "request $title")
                        lifecycleScope.launch(Dispatchers.IO) {
                            tvViewModel.let { request?.fetchData(it) }
                        }
                    } else {
                        (activity as? MainActivity)?.play(tvViewModel)
//                        (activity as? MainActivity)?.switchInfoFragment(item)
                    }
                    setSelectedPosition(
                        tvViewModel.getRowPosition(), true,
                        SelectItemViewHolderTask(tvViewModel.getItemPosition())
                    )
                    Toast.makeText(
                        activity,
                        title,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            tvViewModel.program.observe(viewLifecycleOwner) { _ ->
                if (tvViewModel.program.value!!.isEmpty()) {
                    if (tvViewModel.programId.value != null) {
                        Log.i(TAG, "get program ${tvViewModel.title.value}")
                        request?.fetchProgram(tvViewModel)
                    }
                }
            }
        }
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

    fun toLastPosition() {
        setSelectedPosition(
            selectedPosition, false,
            SelectItemViewHolderTask(tvListViewModel.maxNum[selectedPosition] - 1)
        )
    }

    fun toFirstPosition() {
        setSelectedPosition(
            selectedPosition, false,
            SelectItemViewHolderTask(0)
        )
    }

    override fun startHeadersTransition(withHeaders: Boolean) {
    }

    private fun loadRows() {
        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

        val cardPresenter = CardPresenter(viewLifecycleOwner)

        var idx: Long = 0
        for ((k, v) in TVList.list) {
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

        itemPosition = sharedPref?.getInt("position", 0)!!
        if (itemPosition >= tvListViewModel.size()) {
            itemPosition = 0
            savePosition(0)
        }

        val tvViewModel = tvListViewModel.getTVViewModel(itemPosition)
        tvViewModel?.changed()

        (activity as? MainActivity)?.switchMainFragment()
    }

    fun focus() {
        if (!view?.isFocused!!) {
            view?.requestFocus()
        }
    }

    fun savePosition(position: Int) {
        tvListViewModel.setItemPosition(position)
        with(sharedPref!!.edit()) {
            putInt("position", position)
            apply()
        }
    }

    fun prev() {
        view?.post {
            itemPosition--
            if (itemPosition == -1) {
                itemPosition = tvListViewModel.size() - 1
            }
            savePosition(itemPosition)

            val tvViewModel = tvListViewModel.getTVViewModel(itemPosition)
            tvViewModel?.changed()
        }
    }

    fun next() {
        view?.post {
            itemPosition++
            if (itemPosition == tvListViewModel.size()) {
                itemPosition = 0
            }
            savePosition(itemPosition)

            val tvViewModel = tvListViewModel.getTVViewModel(itemPosition)
            tvViewModel?.changed()
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
                    }
                    tvViewModel.changed()
                }
            }
        }
    }

    fun tvViewModel(): TVViewModel? {
        return tvListViewModel.getTVViewModel(itemPosition)
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
            Log.i(TAG, "onSingleTapConfirmed")
            if (item is TVViewModel) {
                itemPosition = item.id.value!!
                savePosition(itemPosition)

                val tvViewModel = tvListViewModel.getTVViewModel(itemPosition)
                tvViewModel?.changed()

                (activity as? MainActivity)?.switchMainFragment()
            }
        }
    }

    private inner class ItemViewSelectedListener : OnItemViewSelectedListener {
        override fun onItemSelected(
            itemViewHolder: Presenter.ViewHolder?, item: Any?,
            rowViewHolder: RowPresenter.ViewHolder, row: Row
        ) {
            Log.i(TAG, "onSingleTapConfirmed1111")
            if (item is TVViewModel) {
                tvListViewModel.setItemPositionCurrent(item.id.value!!)
            }
        }
    }

    companion object {
        private const val TAG = "MainFragment"
    }
}