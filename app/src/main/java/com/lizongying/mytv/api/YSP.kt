package com.lizongying.mytv.api

import android.content.Context
import android.content.SharedPreferences
import com.lizongying.mytv.Encryptor
import com.lizongying.mytv.MainActivity
import com.lizongying.mytv.Utils.getDateTimestamp
import com.lizongying.mytv.models.TVViewModel
import kotlin.math.floor
import kotlin.random.Random

class YSP(var context: Context) {
    private var cnlid = ""

    private var livepid = ""

    private var stream = "2"

    private var guid = ""

    private var cKey = ""

    private var adjust = 1

    private val sphttps = "1"

    private var platform = "5910204"

    private val cmd = "2"

    private val encryptVer = "8.1"

    private val dtype = "1"

    private val devid = "devid"

    private val otype = "ojson"

    private val appVer = "V1.0.0"

    private val appVersion = "V1.0.0"

    private var randStr = ""

    private val channel = "ysp_tx"

    private var defn = ""

    private var timeStr = ""

    private var signature = ""

    private var appid = "ysp_pc"
    var token = ""

    private var encryptor: Encryptor? = null
    private lateinit var sharedPref: SharedPreferences

    init {
        if (context is MainActivity) {
            encryptor = Encryptor()
            encryptor!!.init(context)

            sharedPref = (context as MainActivity).sharedPref
        }

        guid = getGuid()
    }

    fun switch(tvModel: TVViewModel): String {
        livepid = tvModel.pid.value!!
        cnlid = tvModel.sid.value!!
        defn = "fhd"

        randStr = getRand()

        if (tvModel.retryTimes > 0) {
            guid = newGuid()
        }

        timeStr = getTimeStr()

        cKey =
            encryptor!!.encrypt(cnlid, timeStr, appVer, guid, platform)
        signature = getSignature()
        return """{"cnlid":"$cnlid","livepid":"$livepid","stream":"$stream","guid":"$guid","cKey":"$cKey","adjust":$adjust,"sphttps":"$sphttps","platform":"$platform","cmd":"$cmd","encryptVer":"$encryptVer","dtype":"$dtype","devid":"$devid","otype":"$otype","appVer":"$appVer","app_version":"$appVersion","rand_str":"$randStr","channel":"$channel","defn":"$defn","signature":"$signature"}"""
    }

    fun getAuthData(tvModel: TVViewModel): String {
        livepid = tvModel.pid.value!!

        randStr = getRand()

        if (tvModel.retryTimes > 0) {
            guid = newGuid()
        }

        signature = getAuthSignature()
        return """pid=$livepid&guid=$guid&appid=$appid&rand_str=$randStr&signature=$signature"""
    }

    private fun getTimeStr(): String {
        return getDateTimestamp().toString()
    }

    fun generateGuid(): String {
        val timestamp = (System.currentTimeMillis()).toString(36)
        val randomPart = Random.nextLong().toString(36).take(11)
        return timestamp + "_" + "0".repeat(11 - randomPart.length) + randomPart
    }

    fun getGuid(): String {
        var guid = sharedPref.getString("guid", "")
        if (guid == null || guid.length < 18) {
            guid = generateGuid()
            with(sharedPref.edit()) {
                putString("guid", guid)
                apply()
            }
        }
        return guid
    }

    private fun newGuid(): String {
        guid = generateGuid()
        with(sharedPref.edit()) {
            putString("guid", guid)
            apply()
        }
        return guid
    }

    fun getRand(): String {
        var n = ""
        val e = "ABCDEFGHIJKlMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val r = e.length
        for (i in 0 until 10) {
            n += e[floor(Math.random() * r).toInt()]
        }
        return n
    }

    private fun getSignature(): String {
        val e =
            "adjust=${adjust}&appVer=${appVer}&app_version=$appVersion&cKey=$cKey&channel=$channel&cmd=$cmd&cnlid=$cnlid&defn=${defn}&devid=${devid}&dtype=${dtype}&encryptVer=${encryptVer}&guid=${guid}&livepid=${livepid}&otype=${otype}&platform=${platform}&rand_str=${randStr}&sphttps=${sphttps}&stream=${stream}".toByteArray()
        val hashedData = encryptor?.hash(e) ?: return ""
        return hashedData.let { it -> it.joinToString("") { "%02x".format(it) } }
    }

    private fun getAuthSignature(): String {
        val e =
            "appid=${appid}&guid=${guid}&pid=${livepid}&rand_str=${randStr}".toByteArray()
        val hashedData = encryptor?.hash2(e) ?: return ""
        return hashedData.let { it -> it.joinToString("") { "%02x".format(it) } }
    }

    companion object {
        private const val TAG = "YSP"
    }
}