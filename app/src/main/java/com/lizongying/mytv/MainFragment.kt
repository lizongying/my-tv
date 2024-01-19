package com.lizongying.mytv

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lizongying.mytv.Utils.dpToPx
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

    private var request: Request = Request()

    var tvListViewModel = TVListViewModel()

    private var sharedPref: SharedPreferences? = null

    private var lastVideoUrl: String = ""

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var mUpdateProgramRunnable: UpdateProgramRunnable

    private var ready = 0

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
        sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)

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

                val adapter = CardAdapter(viewLifecycleOwner, tvListViewModelCurrent)
                rowList.add(itemBinding.rowItems)

                adapter.setItemListener(this)

                itemBinding.rowHeader.text = k
                itemBinding.rowItems.adapter = adapter
                itemBinding.rowItems.layoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

                val layoutParams = itemBinding.row.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.topMargin = dpToPx(11F)
                itemBinding.row.layoutParams = layoutParams
                content.addView(itemBinding.row)

                idx++
            }

            mUpdateProgramRunnable = UpdateProgramRunnable()
            handler.post(mUpdateProgramRunnable)

            itemPosition = sharedPref?.getInt(POSITION, 0)!!
            if (itemPosition >= tvListViewModel.size()) {
                itemPosition = 0
            }
            tvListViewModel.setItemPosition(itemPosition)
            setPosition()
            tvListViewModel.getTVListViewModel().value?.forEach { tvViewModel ->
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
                            Log.i(TAG, "request $title ${tvViewModel.pid.value}")
                            lifecycleScope.launch(Dispatchers.IO) {
                                tvViewModel.let { request.fetchData(it) }
                            }
                            (activity as? MainActivity)?.showInfoFragment(tvViewModel)
                            setPosition(
                                tvViewModel.getRowPosition(), tvViewModel.getItemPosition()
                            )
                        } else {
                            if (check(tvViewModel)) {
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

            fragmentReady()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onItemFocusChange(tvViewModel: TVViewModel, hasFocus: Boolean) {
        if (hasFocus) {
            tvListViewModel.setItemPositionCurrent(tvViewModel.id.value!!)
            (activity as MainActivity).keepRunnable()
        }
    }

    override fun onItemClicked(tvViewModel: TVViewModel) {
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
            ((rowList[tvViewModel.getRowPosition()] as RecyclerView).layoutManager as LinearLayoutManager).findViewByPosition(
                tvViewModel.getItemPosition()
            )?.requestFocus()
        }
    }

    fun setPosition(rowPosition: Int, itemPosition: Int) {
        rowList[rowPosition].post {
            ((rowList[rowPosition] as RecyclerView).layoutManager as LinearLayoutManager).findViewByPosition(
                itemPosition
            )?.requestFocus()
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

    fun updateProgram(tvViewModel: TVViewModel) {
        val timestamp = Utils.getDateTimestamp()
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

    override fun onStop() {
        Log.i(TAG, "onStop")
        super.onStop()
        with(sharedPref!!.edit()) {
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

    override fun onDestroyView() {
        Log.i(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
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