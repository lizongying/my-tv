package com.lizongying.mytv.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TVListViewModel : ViewModel() {

    var maxNum = mutableListOf<Int>()

    private val tvListViewModel = MutableLiveData<MutableList<TVViewModel>>()

    private val _itemPosition = MutableLiveData<Int>()
    val itemPosition: LiveData<Int>
        get() = _itemPosition

    private val _itemPositionCurrent = MutableLiveData<Int>()
    val itemPositionCurrent: LiveData<Int>
        get() = _itemPositionCurrent

    fun getTVListViewModel(): MutableLiveData<MutableList<TVViewModel>> {
        return tvListViewModel
    }

    fun addTVViewModel(tvViewModel: TVViewModel) {
        val currentTVModelList = tvListViewModel.value ?: mutableListOf()
        currentTVModelList.add(tvViewModel)
        tvListViewModel.value = currentTVModelList
    }

    fun getTVViewModel(id: Int): TVViewModel? {
        return tvListViewModel.value?.get(id)
    }

    fun getTVViewModelCurrent(): TVViewModel? {
        return _itemPositionCurrent.value?.let { tvListViewModel.value?.get(it) }
    }

    fun setItemPosition(position: Int) {
        _itemPosition.value = position
        _itemPositionCurrent.value = position
    }

    fun setItemPositionCurrent(position: Int) {
        _itemPositionCurrent.value = position
    }

    fun size(): Int {
        return tvListViewModel.value!!.size
    }
}