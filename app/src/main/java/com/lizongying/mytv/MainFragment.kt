package com.lizongying.mytv

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.lizongying.mytv.Utils.getDateTimestamp
import com.lizongying.mytv.models.TVListViewModel
import com.lizongying.mytv.models.TVViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MainFragment : BrowseSupportFragment() {
    var itemPosition: Int = 0

    private var request: Request = Request()

    private var rowsAdapter: ArrayObjectAdapter? = null

    var tvListViewModel = TVListViewModel()

    private var sharedPref: SharedPreferences? = null

    private var lastVideoUrl: String = ""

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var mUpdateProgramRunnable: UpdateProgramRunnable

    private var ready = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        headersState = HEADERS_DISABLED
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        activity?.let { request.initYSP(it) }

        sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)

        loadRows()

        setupEventListeners()

        mUpdateProgramRunnable = UpdateProgramRunnable()
        handler.post(mUpdateProgramRunnable)

        tvListViewModel.getTVListViewModel().value?.forEach { tvViewModel ->
            tvViewModel.errInfo.observe(viewLifecycleOwner) { _ ->
                if (tvViewModel.errInfo.value != null
                    && tvViewModel.id.value == itemPosition
                ) {
                    Toast.makeText(context, tvViewModel.errInfo.value, Toast.LENGTH_SHORT).show()
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
                        Log.i(TAG, "request $title ${tvViewModel.pid.value}")
                        lifecycleScope.launch(Dispatchers.IO) {
                            tvViewModel.let { request.fetchData(it) }
                        }
                        (activity as? MainActivity)?.showInfoFragment(tvViewModel)
                        setSelectedPosition(
                            tvViewModel.getRowPosition(), true,
                            SelectItemViewHolderTask(tvViewModel.getItemPosition())
                        )
                    } else {
                        if (check(tvViewModel)) {
                            (activity as? MainActivity)?.play(tvViewModel)
                            (activity as? MainActivity)?.showInfoFragment(tvViewModel)

                            setSelectedPosition(
                                tvViewModel.getRowPosition(), true,
                                SelectItemViewHolderTask(tvViewModel.getItemPosition())
                            )
                        }
                    }
                }
            }
        }

        fragmentReady()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(mUpdateProgramRunnable)
        with(sharedPref!!.edit()) {
            putInt("position", itemPosition)
            apply()
        }
    }

    fun updateProgram(tvViewModel: TVViewModel) {
        val timestamp = getDateTimestamp()
        if (timestamp - tvViewModel.programUpdateTime > 60) {
            if (tvViewModel.program.value!!.isEmpty()) {
                tvViewModel.programUpdateTime = timestamp
                request.fetchProgram(tvViewModel)
            } else {
                if (timestamp - tvViewModel.program.value!!.last().et < 600) {
                    tvViewModel.programUpdateTime = timestamp
                    request.fetchProgram(tvViewModel)
                }
            }
        }
    }

    inner class UpdateProgramRunnable : Runnable {
        override fun run() {
            tvListViewModel.getTVListViewModel().value?.filter { it.programId.value != null }
                ?.forEach { tvViewModel ->
                    updateProgram(
                        tvViewModel
                    )
                }
            handler.postDelayed(this, 60000)
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
        }
        tvListViewModel.setItemPosition(itemPosition)
    }

    fun fragmentReady() {
        ready++
        Log.i(TAG, "ready $ready")
        if (ready == 4) {
//            request.fetchPage()
            val tvViewModel = tvListViewModel.getTVViewModel(itemPosition)
            tvViewModel?.changed()

            (activity as? MainActivity)?.switchMainFragment()
        }
    }

    fun play(itemPosition: Int) {
        view?.post {
            if (itemPosition < tvListViewModel.size()) {
                this.itemPosition = itemPosition
                tvListViewModel.setItemPosition(itemPosition)
                tvListViewModel.getTVViewModel(itemPosition)?.changed()
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
                if (itemPosition != item.id.value!!) {
                    itemPosition = item.id.value!!
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
                tvListViewModel.setItemPositionCurrent(item.id.value!!)
                (activity as MainActivity).keepRunnable()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        view!!.requestFocus()
    }

    companion object {
        private const val TAG = "MainFragment"
    }
}