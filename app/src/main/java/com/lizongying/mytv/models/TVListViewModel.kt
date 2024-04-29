package com.lizongying.mytv.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lizongying.mytv.SP

class TVListViewModel : ViewModel() {

    var maxNum = mutableListOf<Int>()

    private val _tvListViewModel = MutableLiveData<MutableList<TVViewModel>>()
    val tvListViewModel: LiveData<MutableList<TVViewModel>>
        get() = _tvListViewModel

    private val _itemPosition = MutableLiveData<Int>()
    val itemPosition: LiveData<Int>
        get() = _itemPosition

    fun addTVViewModel(tvViewModel: TVViewModel) {
        if (_tvListViewModel.value == null) {
            _tvListViewModel.value = mutableListOf(tvViewModel)
        } else {
            _tvListViewModel.value?.add(tvViewModel)
        }
    }

    fun getTVViewModel(id: Int): TVViewModel? {
        return _tvListViewModel.value?.get(id)
    }

    fun setItemPosition(position: Int) {
        _itemPosition.value = position
        SP.itemPosition = position
    }

    fun size(): Int {
        if (_tvListViewModel.value == null) {
            return 0
        }

        return _tvListViewModel.value!!.size
    }
}