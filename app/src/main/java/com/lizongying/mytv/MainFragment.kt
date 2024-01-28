package com.lizongying.mytv

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

    private var itemPosition = 0

    private var rowsAdapter: ArrayObjectAdapter? = null

    private var request = Request()

    var tvListViewModel = TVListViewModel()

    private lateinit var sharedPref: SharedPreferences

    private var lastVideoUrl = ""

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
        sharedPref = (activity as? MainActivity)?.sharedPref!!

        loadRows()

        setupEventListeners()

        mUpdateProgramRunnable = UpdateProgramRunnable()
        handler.post(mUpdateProgramRunnable)

        tvListViewModel.tvListViewModel.value?.forEach { tvViewModel ->
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
                    if (tvViewModel.pid.value != "") {
                        Log.i(TAG, "request $title")
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

        itemPosition = sharedPref.getInt(POSITION, 0)
        if (itemPosition >= tvListViewModel.size()) {
            itemPosition = 0
        }
        tvListViewModel.setItemPosition(itemPosition)
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
        ready++
        Log.i(TAG, "ready $ready")
        if (ready == 4) {
//            request.fetchPage()
            tvListViewModel.getTVViewModel(itemPosition)?.changed()
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
            tvListViewModel.tvListViewModel.value?.filter { it.programId.value != null }
                ?.forEach { tvViewModel ->
                    updateProgram(
                        tvViewModel
                    )
                }
            handler.postDelayed(this, 60000)
        }
    }

    override fun onStop() {
        Log.i(TAG, "onStop")
        super.onStop()
        with(sharedPref.edit()) {
            putInt(POSITION, itemPosition)
            apply()
        }
        Log.i(TAG, "POSITION saved")
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        super.onDestroy()
        handler.removeCallbacks(mUpdateProgramRunnable)
    }

    override fun onResume() {
        super.onResume()
        view?.post { view?.requestFocus() }
    }

    companion object {
        private const val TAG = "MainFragment"
        private const val POSITION = "position"
    }
}