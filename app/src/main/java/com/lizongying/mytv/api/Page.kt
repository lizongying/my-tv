package com.lizongying.mytv.api

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException

data class Page(
    val code: Int,
    val msg: String,
    val data: Data,
)

data class Data(
    val feedModuleList: List<Module>
)

data class Module(
    val dataTvChannelList: List<TvChannelListData>
)

data class TvChannelListData(
    val dataTvChannelList: List<TvChannelListData>
)