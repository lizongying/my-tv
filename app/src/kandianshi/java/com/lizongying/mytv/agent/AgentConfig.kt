package com.lizongying.mytv.agent

import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Calendar
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.security.auth.x500.X500Principal

data class AgentConfig(
    val enabled: Boolean = true,
    val aiEnabled: Boolean = false,
    val endpoint: String = "https://api.deepseek.com",
    val model: String = "deepseek-flash",
    val intervalHours: Int = 6,
    val sources: List<String> = emptyList(),
    val blocked: Set<String> = emptySet(),
    val logoAiEnabled: Boolean = true,
    val logoCacheKb: Int = 128,
    val logoLookupEnabled: Boolean = true,
    val logoImageMb: Int = 5
)

class AgentConfigStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("agent_config", Context.MODE_PRIVATE)
    fun load(): AgentConfig = runCatching {
        val o = JSONObject(prefs.getString("config", "{}").orEmpty())
        AgentConfig(o.optBoolean("enabled", true), o.optBoolean("aiEnabled", false), o.optString("endpoint", "https://api.deepseek.com"),
            o.optString("model", "deepseek-flash"), o.optInt("intervalHours", 6).takeIf { it in setOf(6, 12, 24) } ?: 6,
            strings(o.optJSONArray("sources")), strings(o.optJSONArray("blocked")).toSet(),
            o.optBoolean("logoAiEnabled", true), o.optInt("logoCacheKb", 128).takeIf { it in setOf(64,128,256) } ?: 128,
            o.optBoolean("logoLookupEnabled",true),RealLogoPolicy.limitMb(o.optInt("logoImageMb",5)))
    }.getOrDefault(AgentConfig())

    fun apiKey(): String = runCatching {
        val raw = prefs.getString("key", "").orEmpty()
        if (raw.isBlank()) "" else SecretBox.decrypt(raw)
    }.getOrDefault("")

    fun publicJson(): JSONObject {
        val c = load()
        return JSONObject().put("enabled", c.enabled).put("aiEnabled", c.aiEnabled)
            .put("endpoint", c.endpoint).put("model", c.model).put("intervalHours", c.intervalHours)
            .put("sources", JSONArray(c.sources)).put("blocked", JSONArray(c.blocked.toList()))
            .put("hasKey", apiKey().isNotBlank()).put("logoAiEnabled", c.logoAiEnabled).put("logoCacheKb", c.logoCacheKb)
            .put("logoLookupEnabled",c.logoLookupEnabled).put("logoImageMb",c.logoImageMb)
    }

    @Synchronized fun save(body: String) {
        require(body.toByteArray(Charsets.UTF_8).size <= 32768) { "配置内容过大" }
        val o = JSONObject(body)
        val endpoint = o.optString("endpoint").trim().trimEnd('/')
        if (endpoint.isNotBlank()) {
            val uri = NetworkPolicy.httpUrl(endpoint)
            require(uri.scheme == "https" && uri.query == null) { "AI API 必须是无查询参数的 HTTPS 地址" }
        }
        val model = o.optString("model").trim()
        require(model.length <= 128 && model.none { it.isISOControl() }) { "模型名称无效" }
        val sourceList = strings(o.optJSONArray("sources")).map(String::trim).filter(String::isNotEmpty).distinct()
        require(sourceList.size <= 20) { "最多添加 20 个来源" }
        sourceList.forEach { NetworkPolicy.publicUrl(it, false) }
        val blocked = strings(o.optJSONArray("blocked")).map(String::trim).filter(String::isNotEmpty).distinct()
        require(blocked.size <= 100 && blocked.all { it.length <= 2048 && it.none(Char::isWhitespace) }) { "屏蔽列表无效或超过 100 项" }
        val clear = o.optBoolean("clearKey", false)
        val supplied = o.optString("key").trim()
        require(supplied.length <= 4096 && supplied.none { it.isISOControl() }) { "密钥格式无效" }
        val key = if (clear) "" else supplied.ifBlank { apiKey() }
        val aiEnabled = o.optBoolean("aiEnabled", false)
        require(!aiEnabled || endpoint.isNotBlank() && model.isNotBlank() && key.isNotBlank()) { "启用 AI 前请填写 API 地址、模型和密钥" }
        val config = JSONObject().put("enabled", o.optBoolean("enabled", true)).put("aiEnabled", aiEnabled)
            .put("endpoint", endpoint).put("model", model)
            .put("intervalHours", o.optInt("intervalHours", 6).takeIf { it in setOf(6, 12, 24) } ?: 6)
            .put("sources", JSONArray(sourceList)).put("blocked", JSONArray(blocked))
            .put("logoAiEnabled", o.optBoolean("logoAiEnabled", load().logoAiEnabled))
            .put("logoCacheKb", o.optInt("logoCacheKb", load().logoCacheKb).takeIf { it in setOf(64,128,256) } ?: 128)
            .put("logoLookupEnabled",o.optBoolean("logoLookupEnabled",load().logoLookupEnabled))
            .put("logoImageMb",RealLogoPolicy.limitMb(o.optInt("logoImageMb",load().logoImageMb)))
        // Encrypt before committing anything: a Keystore failure leaves the previous configuration intact.
        val encrypted = if (key.isEmpty()) "" else SecretBox.encrypt(context, key)
        check(prefs.edit().putString("config", config.toString()).putString("key", encrypted).commit()) { "配置保存失败" }
    }

    companion object {
        fun strings(array: JSONArray?): List<String> = if (array == null) emptyList() else
            (0 until array.length()).map { array.getString(it) }
    }
}

private object SecretBox {
    private const val AES_ALIAS = "kandianshi.ai.aes.v1"
    private const val RSA_ALIAS = "kandianshi.ai.rsa.v1"
    private fun store(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    @Synchronized fun encrypt(context: Context, plain: String): String {
        val ks = store()
        if (Build.VERSION.SDK_INT >= 23) {
            if (!ks.containsAlias(AES_ALIAS)) {
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                    init(KeyGenParameterSpec.Builder(AES_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
                    generateKey()
                }
            }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, ks.getKey(AES_ALIAS, null) as SecretKey)
            return "aes:" + encode(cipher.iv) + ":" + encode(cipher.doFinal(plain.toByteArray(Charsets.UTF_8)))
        }
        // Android 5/5.1: protect a fresh AES key with a non-exportable Keystore RSA private key.
        if (!ks.containsAlias(RSA_ALIAS)) {
            val end = Calendar.getInstance().apply { add(Calendar.YEAR, 30) }.time
            @Suppress("DEPRECATION")
            val spec = KeyPairGeneratorSpec.Builder(context).setAlias(RSA_ALIAS).setKeySize(2048)
                .setSubject(X500Principal("CN=KanDianshi AI")).setSerialNumber(BigInteger.ONE)
                .setStartDate(Calendar.getInstance().time).setEndDate(end).build()
            KeyPairGenerator.getInstance("RSA", "AndroidKeyStore").apply { initialize(spec); generateKeyPair() }
        }
        val aes = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, aes)
        val rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding"); rsa.init(Cipher.ENCRYPT_MODE, ks.getCertificate(RSA_ALIAS).publicKey)
        return "rsa:" + encode(rsa.doFinal(aes.encoded)) + ":" + encode(cipher.iv) + ":" + encode(cipher.doFinal(plain.toByteArray(Charsets.UTF_8)))
    }

    fun decrypt(value: String): String {
        val parts = value.split(':'); val ks = store()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        if (parts[0] == "aes" && parts.size == 3) {
            cipher.init(Cipher.DECRYPT_MODE, ks.getKey(AES_ALIAS, null) as SecretKey, GCMParameterSpec(128, decode(parts[1])))
            return cipher.doFinal(decode(parts[2])).toString(Charsets.UTF_8)
        }
        require(parts[0] == "rsa" && parts.size == 4)
        val rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding"); rsa.init(Cipher.DECRYPT_MODE, ks.getKey(RSA_ALIAS, null))
        val key = javax.crypto.spec.SecretKeySpec(rsa.doFinal(decode(parts[1])), "AES")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, decode(parts[2])))
        return cipher.doFinal(decode(parts[3])).toString(Charsets.UTF_8)
    }

    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(value: String) = Base64.decode(value, Base64.NO_WRAP)
}
