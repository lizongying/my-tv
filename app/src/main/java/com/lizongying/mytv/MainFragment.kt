package com.lizongying.mytv

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainFragment : BrowseSupportFragment() {

    var itemPosition: Int = 0

    private val list2: MutableList<Info> = mutableListOf()

    private var request: Request? = null

    private var rowsAdapter: ArrayObjectAdapter? = null

    private var tvListViewModel = TVListViewModel()

    private var sharedPref: SharedPreferences? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)

        setupUIElements()
        request = activity?.let { Request(it) }
        loadRows()
        setupEventListeners()

        view?.post {
            request?.fetchPage()
        }

        tvListViewModel.getListLiveData().value?.forEach { tvViewModel ->
            tvViewModel.videoUrl.observe(viewLifecycleOwner) { _ ->
                Log.i(TAG, "tv ${tvViewModel.getTV()}")
                if (tvViewModel.updateByYSP()) {
                    val tv = tvViewModel.getTV()
                    if (tv.id == itemPosition) {
                        (activity as? MainActivity)?.play(tv)
//                        (activity as? MainActivity)?.switchInfoFragment(tv)
                    }
                }
            }
        }
    }

    private fun setupUIElements() {
        brandColor = ContextCompat.getColor(context!!, R.color.fastlane_background)
//        headersState = HEADERS_DISABLED
    }

    private fun updateRows(tv: TV) {
// 获取适配器中的数据
        val dataList = rowsAdapter?.replace(tv.id, tv)
//
//// 修改数据
//// 这里假设 dataList 是一个可变的列表
//        dataList[position] = updatedData
//
//// 刷新适配器
//        rowsAdapter.notifyItemChanged(position)
//        rowsAdapter.notifyItemRangeChanged()
    }

    private fun loadRows() {
        val list = TVList.list

        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

//        val cardPresenter = CardPresenter(lifecycleScope, viewLifecycleOwner)
        val cardPresenter = CardPresenter(lifecycleScope)

        var idx: Long = 0
        for ((k, v) in list) {
            val listRowAdapter = ArrayObjectAdapter(cardPresenter)
            var idx2 = 0
            for ((_, v1) in v) {
                list2.add(
                    Info(
                        idx.toInt(), idx2, v1
                    )
                )
                listRowAdapter.add(v1)
                idx2++
                tvListViewModel.addTV(v1)
            }
            val header = HeaderItem(idx, k)
            rowsAdapter!!.add(ListRow(header, listRowAdapter))
            idx++
        }

        adapter = rowsAdapter

        itemPosition = sharedPref?.getInt("position", 0)!!

        val tvModel = tvListViewModel.getTVModel(itemPosition)
        if (tvModel?.ysp() != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                tvModel.let { request?.fetchData(it) }
            }
        } else {
            (activity as? MainActivity)?.play(list2[itemPosition].item)
//            (activity as? MainActivity)?.switchInfoFragment(list2[itemPosition].item)
        }

        (activity as? MainActivity)?.switchMainFragment()
    }

    fun focus() {
        if (!view?.isFocused!!) {
            view?.requestFocus()
        }
    }

    fun savePosition(position: Int) {
        with(sharedPref!!.edit()) {
            putInt("position", position)
            apply()
        }
    }

    fun prev() {
        view?.post {
            itemPosition--
            if (itemPosition == -1) {
                itemPosition = list2.size - 1
            }
            savePosition(itemPosition)

            val l = list2[itemPosition]

            val tvModel = tvListViewModel.getTVModel(itemPosition)
            if (tvModel?.ysp() != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    tvModel.let { request?.fetchData(it) }
                }
            } else {
                l.item.let { (activity as? MainActivity)?.play(it) }
            }

            setSelectedPosition(
                l.rowPosition, false,
                SelectItemViewHolderTask(l.itemPosition)
            )
            Toast.makeText(
                activity,
                l.item.title,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun next() {
        view?.post {
            itemPosition++
            if (itemPosition == list2.size) {
                itemPosition = 0
            }
            savePosition(itemPosition)

            val l = list2[itemPosition]

            val tvModel = tvListViewModel.getTVModel(itemPosition)
            if (tvModel?.ysp() != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    tvModel.let { request?.fetchData(it) }
                }
            } else {
                l.item.let { (activity as? MainActivity)?.play(it) }
            }

            setSelectedPosition(
                l.rowPosition, false,
                SelectItemViewHolderTask(l.itemPosition)
            )
            Toast.makeText(
                activity,
                l.item.title,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun prevSource() {
        view?.post {
            val tv = list2[itemPosition].item
            if (tv.videoUrl.size > 1) {
                tv.videoIndex--
                if (tv.videoIndex == -1) {
                    tv.videoIndex = tv.videoUrl.size - 1
                }

                (activity as? MainActivity)?.play(tv)
//                (activity as? MainActivity)?.switchInfoFragment(tv)
            }
        }
    }

    fun nextSource() {
        view?.post {
            val tv = list2[itemPosition].item

            if (tv.videoUrl.size > 1) {
                tv.videoIndex++
                if (tv.videoIndex == tv.videoUrl.size) {
                    tv.videoIndex = 0
                }

                (activity as? MainActivity)?.play(tv)
//                (activity as? MainActivity)?.switchInfoFragment(tv)
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
            if (item is TV) {
                itemPosition = item.id
                savePosition(itemPosition)

                val tvModel = tvListViewModel.getTVModel(itemPosition)
                if (tvModel?.ysp() != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        tvModel.let { request?.fetchData(it) }
                    }
                } else {
                    (activity as? MainActivity)?.play(item)
//                    (activity as? MainActivity)?.switchInfoFragment(item)
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
            if (itemViewHolder == null) {
                view?.post {
                    val it = list2[itemPosition]
                    setSelectedPosition(
                        it.rowPosition, false,
                        SelectItemViewHolderTask(it.itemPosition)
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainFragment"
    }
}