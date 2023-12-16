package com.lizongying.mytv.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lizongying.mytv.TV

class TVListViewModel : ViewModel() {
    private val tvListLiveData = MutableLiveData<MutableList<TV>>()

    private val tvModelListLiveData = MutableLiveData<MutableList<TVViewModel>>()

    fun getListLiveData(): MutableLiveData<MutableList<TVViewModel>> {
        return tvModelListLiveData
    }

    fun addTV(tv: TV) {
        val currentList = tvListLiveData.value ?: mutableListOf()
        currentList.add(tv)
        tvListLiveData.value = currentList

        val currentModelList = tvModelListLiveData.value ?: mutableListOf()
        currentModelList.add(TVViewModel(tv))
        tvModelListLiveData.value = currentModelList
    }

    fun updateTV(tv: TV) {
        val currentList = tvListLiveData.value ?: mutableListOf()
        currentList[tv.id] = tv
        tvListLiveData.value = currentList

        val currentModelList = tvModelListLiveData.value ?: mutableListOf()
        currentModelList[tv.id].update(tv)
        tvModelListLiveData.value = currentModelList
    }

    fun getTV(id: Int): TV? {
        return tvListLiveData.value?.get(id)
    }

    fun getTVModel(id: Int): TVViewModel? {
        return tvModelListLiveData.value?.get(id)
    }

    fun size(): Int {
        return tvListLiveData.value!!.size
    }
}