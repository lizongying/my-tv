package com.lizongying.mytv

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.log

object TVList {
    @Volatile
    var list: Map<String, List<TV>>? = null
        get():Map<String, List<TV>>? {
            //等待初始化完成
            while (this.list === null) {
                Thread.sleep(10)
            }
            return this.list
        }


    /**
     * 初始化
     *
     * @param context Context
     */
    fun init(context: Context) {
        CoroutineScope(Dispatchers.Default).launch {
            //获取本地版本号
            val localVersion = ChannelUtils.getLocalVersion(context)
            //获取服务器版本号
            val serverVersion = try {
                ChannelUtils.getServerVersion(context)
            } catch (e: IOException) {
                Log.e("TVList", "无法从服务器获取版本信息", e)
                Integer.MIN_VALUE
            }
            //频道列表
            val channelTVMap: MutableMap<String, MutableList<TV>> = mutableMapOf()
            //是否从服务器更新
            var updateFromServer = false
            //获取频道列表
            val tvList: List<TV> = if (localVersion < serverVersion) {
                //获取服务器地址
                val url = ChannelUtils.getServerUrl(context)
                //是否从服务器更新
                updateFromServer = true
                Log.i("TVList", "从服务器获取频道信息")
                try {
                    ChannelUtils.getServerChannel(url)
                } catch (e: IOException) {
                    Log.e("TVList", "无法从服务器获取频道信息", e)
                    updateFromServer = false
                    ChannelUtils.getLocalChannel(context)
                }
            } else {
                Log.i("TVList", "从本地获取频道信息")
                //获取本地频道
                ChannelUtils.getLocalChannel(context)
            }
            //按频道分类
            for (tv in tvList) {
                val key = tv.channel
                if (channelTVMap.containsKey(key)) {
                    val list = channelTVMap[key]!!
                    list.add(tv)
                    channelTVMap[key] = list
                } else {
                    channelTVMap[key] = mutableListOf(tv)
                }
            }
            //保存频道列表
            list = channelTVMap
            //保存版本号
            if (updateFromServer) {
                ChannelUtils.updateLocalChannel(context, serverVersion, tvList)
            }
        }
    }
}