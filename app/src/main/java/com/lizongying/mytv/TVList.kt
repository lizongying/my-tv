package com.lizongying.mytv

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object TVList {
    lateinit var list: Map<String, List<TV>>

    fun init(context: Context){
        if(::list.isInitialized){
            return
        }
        synchronized(this){
            if(::list.isInitialized){
                return
            }
            list = setupTV(context)
        }
    }


    private fun setupTV(context:Context): Map<String, List<TV>> {
        val map: MutableMap<String, MutableList<TV>> = mutableMapOf()
        val appDirectory = Utils.getAppDirectory(context)

        //检查当前目录下是否存在channels.json
        var file = File(appDirectory, "channels.json")
        if (!file.exists()) {
            //不存在则从assets中拷贝
            file = File(appDirectory, "channels.json")
            file.createNewFile()
            context.assets.open("channels.json").use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        //读取channels.json，并转换为Map<String,LIst<TV>>
        val json = file.readText()
        //防止类型擦除
        val type = object : TypeToken<Array<TV>>() {}.type
        Gson().fromJson<Array<TV>>(json, type).forEach {
            if (map.containsKey(it.channel)) {
                map[it.channel]?.add(it)
            } else {
                map[it.channel] = mutableListOf(it)
            }
        }
        return map
    }
}