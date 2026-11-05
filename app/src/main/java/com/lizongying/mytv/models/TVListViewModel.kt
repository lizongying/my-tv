package com.lizongying.mytv.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TVListViewModel : ViewModel() {

    var maxNum = mutableListOf<Int>()

    private val _tvListViewModel = MutableLiveData<MutableList<TVViewModel>>()
    val tvListViewModel: LiveData<MutableList<TVViewModel>>
        get() = _tvListViewModel

    private val _itemPosition = MutableLiveData<Int>()
    val itemPosition: LiveData<Int>
        get() = _itemPosition

    private val _itemPositionCurrent = MutableLiveData<Int>()
    val itemPositionCurrent: LiveData<Int>
        get() = _itemPositionCurrent

    fun addTVViewModel(tvViewModel: TVViewModel) {
        if (_tvListViewModel.value == null) {
            _tvListViewModel.value = mutableListOf(tvViewModel)
        } else {
            _tvListViewModel.value?.add(tvViewModel)
        }
    }

    fun getTVViewModel(id: Int): TVViewModel? {
        val list = _tvListViewModel.value ?: return null
        return if (id in list.indices) list[id] else null
    }

    fun getTVViewModelCurrent(): TVViewModel? {
        val position = _itemPositionCurrent.value ?: return null
        return getTVViewModel(position)
    }

    fun setItemPosition(position: Int) {
        _itemPosition.value = position
        _itemPositionCurrent.value = position
    }

    fun setItemPositionCurrent(position: Int) {
        _itemPositionCurrent.value = position
    }

    fun size(): Int {
        if (_tvListViewModel.value == null) {
            return 0
        }
        return _tvListViewModel.value!!.size
    }
}