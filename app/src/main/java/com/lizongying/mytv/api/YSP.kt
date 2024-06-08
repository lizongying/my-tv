package com.lizongying.mytv.api

import android.content.Context
import android.util.Log
import com.lizongying.mytv.SP
import com.lizongying.mytv.Utils
import com.lizongying.mytv.Utils.getDateTimestamp
import com.lizongying.mytv.models.TVViewModel
import java.security.MessageDigest
import kotlin.math.floor
import kotlin.random.Random

object YSP {
    private const val TAG = "YSP"

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
    var token2 = ""

    var yspsdkinput = ""
    var openapi_signature = ""
    var yspticket = ""

    var nseqId = 1
    var nrequest_id = ""

    private var encryptor = Encryptor()

    fun init(context: Context) {
        encryptor.init(context)
        guid = getGuid()
    }

    fun switch(tvModel: TVViewModel): String {
        livepid = tvModel.getTV().pid
        cnlid = tvModel.getTV().sid
        defn = "fhd"

        randStr = getRand()

        if (tvModel.retryTimes > 0) {
            guid = newGuid()
        }

        timeStr = getTimeStr()

        cKey =
            encryptor.encrypt(cnlid, timeStr, appVer, guid, platform)
        randStr = getRand()
        signature = getSignature()
        yspticket = encryptor.encrypt2("$livepid&$timeStr&$guid&519748109").lowercase()

        return """{"cnlid":"$cnlid","livepid":"$livepid","stream":"$stream","guid":"$guid","cKey":"$cKey","adjust":$adjust,"sphttps":"$sphttps","platform":"$platform","cmd":"$cmd","encryptVer":"$encryptVer","dtype":"$dtype","devid":"$devid","otype":"$otype","appVer":"$appVer","app_version":"$appVersion","rand_str":"$randStr","channel":"$channel","defn":"$defn","signature":"$signature"}"""
    }

    fun getTokenData(tvModel: TVViewModel) {
        livepid = tvModel.getTV().pid
        cnlid = tvModel.getTV().sid
        defn = "fhd"

        randStr = getRand()

        if (tvModel.retryTimes > 0) {
            guid = newGuid()
        }

        cKey =
            encryptor.encrypt(cnlid, timeStr, appVer, guid, platform)

        yspsdkinput =
            md("adjust=$adjust&app_version=$appVersion&appVer=$appVer&channel=$channel&cKey=$cKey&cmd=$cmd&cnlid=$cnlid&defn=$defn&devid=$devid&dtype=$dtype&encryptVer=$encryptVer&guid=$guid&livepid=$livepid&otype=$otype&platform=$platform&sphttps=$sphttps&stream=$stream")

        nseqId++

        nrequest_id = "999999" + getRand() + getTimeStr()

        openapi_signature =
            md("yspappid:519748109;host:www.yangshipin.cn;protocol:https:;token:$token2;input:$yspsdkinput-$guid-$nseqId-$nrequest_id;")
    }

    fun md(str: String): String {
        val md = MessageDigest.getInstance("MD5")
        md.update(str.toByteArray())
        val digest = md.digest()
        return digest.let { it -> it.joinToString("") { "%02x".format(it) } }
    }

    fun getAuthData(tvModel: TVViewModel): String {
        livepid = tvModel.getTV().pid

        randStr = getRand()

        if (tvModel.retryTimes > 0) {
            guid = newGuid()
        }

        signature = getAuthSignature()
        return """pid=$livepid&guid=$guid&appid=$appid&rand_str=$randStr&signature=$signature"""
    }

    fun getTimeStr(): String {
        return getDateTimestamp().toString()
    }

    fun generateGuid(): String {
        val timestamp = (System.currentTimeMillis()).toString(36)
        val randomPart = Random.nextLong().toString(36).take(11)
        return timestamp + "_" + "0".repeat(11 - randomPart.length) + randomPart
    }

    fun getGuid(): String {
        var guid = SP.guid
        if (guid.length < 18) {
            guid = generateGuid()
            this.guid = guid
            SP.guid = guid
        }
        return guid
    }

    private fun newGuid(): String {
        guid = generateGuid()
        SP.guid = guid
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
        val hashedData = encryptor.hash(e) ?: return ""
        return hashedData.let { it -> it.joinToString("") { "%02x".format(it) } }
    }

    private fun getAuthSignature(): String {
        val e =
            "appid=${appid}&guid=${guid}&pid=${livepid}&rand_str=${randStr}${Utils.b}".toByteArray()
        val hashedData = encryptor.hash3(e) ?: return ""
        return hashedData.let { it -> it.joinToString("") { "%02x".format(it) } }
    }

    fun getAuthSignature(e: String): String {
        val hashedData = encryptor.hash3("$e${Utils.b}".toByteArray()) ?: return ""
        return hashedData.let { it -> it.joinToString("") { "%02x".format(it) } }
    }
}