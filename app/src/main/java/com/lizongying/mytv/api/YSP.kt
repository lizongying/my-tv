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

    private var encryptor: Encryptor? = null
    private var sharedPref: SharedPreferences? = null

    init {
        if (context is MainActivity) {
            encryptor = Encryptor()
            encryptor!!.init(context)

            sharedPref = (context as MainActivity).getPreferences(Context.MODE_PRIVATE)
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

//        guid = "lq3oqitm_1e15dnzgjnb"
//        randStr = "BfcCPQp8Hq"
//        timeStr = "1702166501"

        cKey =
            encryptor!!.encrypt(cnlid, timeStr, appVer, guid, platform)
        signature = getSignature()
        return """{"cnlid":"$cnlid","livepid":"$livepid","stream":"$stream","guid":"$guid","cKey":"$cKey","adjust":$adjust,"sphttps":"$sphttps","platform":"$platform","cmd":"$cmd","encryptVer":"$encryptVer","dtype":"$dtype","devid":"$devid","otype":"$otype","appVer":"$appVer","app_version":"$appVersion","rand_str":"$randStr","channel":"$channel","defn":"$defn","signature":"$signature"}"""
//        return """{"cnlid":"2000203803","livepid":"600001801","stream":"2","guid":"lq1y36mb_ccfwmja9zan","cKey":"--01A9F5E89BB86A0C61F4025BEDE15309B6913A79FD1AF1EE7F5EC9C7605F377D1D2281488385C32DEB9E7D0DD3559CB700BD7AF44DD5C9DE0AE14D94B8214027B5D664C108AEE23532348DCC61B86F7C8FBB6CF14D588E6093A25E97DF6D66F4882AB28F17016472DD43D45EF076B7F505176A5E8DEDF2662E5F9AB12B69CB20BCE1579BE724091F3AF6826AE34B713906F3FE139C3783F80EECBD08416DC525E1","adjust":1,"sphttps":"1","platform":"5910204","cmd":"2","encryptVer":"8.1","dtype":"1","devid":"devid","otype":"ojson","appVer":"V1.0.0","app_version":"V1.0.0","rand_str":"IOS2soOw44","channel":"ysp_tx","defn":"fhd","signature":"6ca945f651817de8c6e6910457ceafd6"}"""
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
        var guid = sharedPref?.getString("guid", "")
        if (guid == null || guid.length < 18) {
            guid = generateGuid()
            with(sharedPref!!.edit()) {
                putString("guid", guid)
                apply()
            }
        }
        return guid
    }

    private fun newGuid(): String {
        guid = generateGuid()
        with(sharedPref!!.edit()) {
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

    companion object {
        private const val TAG = "YSP"
    }
}