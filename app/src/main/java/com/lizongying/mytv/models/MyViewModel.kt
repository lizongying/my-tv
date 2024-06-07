package com.lizongying.mytv.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MyViewModel : ViewModel() {
    private var _downloadProgress = MutableLiveData<Int>()
    val downloadProgress: LiveData<Int>
        get() = _downloadProgress

    fun setDownloadProgress(progress: Int) {
        _downloadProgress.value = progress
    }

    init {
        _downloadProgress.value = 0
    }
}