package com.lizongying.mytv.agent

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.ByteArrayOutputStream

class VisionClient(private val context: Context) {
    fun inspect(title: String, frames: List<String>): VisionVerdict {
        val config = AgentConfigStore(context); val c = config.load(); val key = config.apiKey()
        require(c.aiEnabled && key.isNotBlank() && c.model.isNotBlank()) { "请先通过手机配置并启用 AI" }
        val base = NetworkPolicy.httpUrl(c.endpoint.trim().trimEnd('/'))
        require(base.scheme == "https" && base.query == null && key.length <= 4096 && key.none { it.isISOControl() }) { "AI 配置无效" }
        require(!base.host.equals("api.deepseek.com", true) || c.model in setOf("deepseek-flash", "deepseek-v4-flash-vision-exp")) {
            "图片巡检请将 DeepSeek 模型设为 deepseek-flash"
        }
        val body = VisionPolicy.request(c.model, title, frames)
        if (base.host.equals("api.deepseek.com", true)) body.put("thinking", JSONObject().put("type", "disabled"))
        val bytes = body.toString().toByteArray(Charsets.UTF_8)
        val address = if (base.path.endsWith("/chat/completions")) base.toString() else base.toString() + "/chat/completions"
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 8000; connection.readTimeout = 25000
        connection.requestMethod = "POST"; connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.setRequestProperty("Authorization", "Bearer " + key)
        try {
            VisionStore(context).reserve()
            connection.setFixedLengthStreamingMode(bytes.size)
            connection.outputStream.use { it.write(bytes) }
            require(connection.responseCode == 200) {
                "图片 API 返回 HTTP " + connection.responseCode + "；请检查模型是否支持图片、额度和密钥。线路未移除"
            }
            val text = connection.inputStream.use { input ->
                val out = ByteArrayOutputStream(); val buffer = ByteArray(4096)
                while (true) {
                    val n = input.read(buffer); if (n < 0) break
                    require(out.size() + n <= 128 * 1024) { "图片识别响应过大" }
                    out.write(buffer, 0, n)
                }
                out.toString("UTF-8")
            }
            return VisionPolicy.parse(AiClient.answerContent(text))
        } finally { connection.disconnect() }
    }
}
