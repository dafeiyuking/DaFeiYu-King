package com.sukisu.ultra.deepseek

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 用用户自己的 key 和 DeepSeek 的 API 说话：聊天补全接口，以及这个 key 所属账户的余额。
 * 除了发给 DeepSeek，什么都不发到别处；key 也只存在手机上。
 */
object DeepSeekClient {

    private const val BASE = "https://api.deepseek.com"
    private const val MODEL = "deepseek-chat"

    /** 值得显示给用户的失败：能拿到 API 自己的说明就用它。 */
    class ApiError(message: String) : Exception(message)

    /** 账户还能花多少，按 DeepSeek 报的算。 */
    data class Balance(val available: Boolean, val currency: String, val total: String)

    fun balance(apiKey: String): Balance {
        val json = JSONObject(request("$BASE/user/balance", apiKey, null))
        val infos = json.optJSONArray("balance_infos")
        val first = if (infos != null && infos.length() > 0) infos.getJSONObject(0) else null
        return Balance(
            available = json.optBoolean("is_available", false),
            currency = first?.optString("currency")?.ifBlank { "CNY" } ?: "CNY",
            total = first?.optString("total_balance")?.ifBlank { "-" } ?: "-",
        )
    }

    /** 到目前为止这段对话的一个回答。每一项是一对角色（"user" 或 "assistant"）和它的文字。 */
    fun chat(apiKey: String, history: List<Pair<String, String>>): String {
        val messages = JSONArray()
        history.forEach { (role, content) ->
            messages.put(JSONObject().put("role", role).put("content", content))
        }
        val payload = JSONObject()
            .put("model", MODEL)
            .put("messages", messages)
            .put("stream", false)
        val json = JSONObject(request("$BASE/chat/completions", apiKey, payload.toString()))
        val answer = json.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            .orEmpty()
        if (answer.isBlank()) {
            throw ApiError("DeepSeek 返回了空回答")
        }
        return answer
    }

    private fun request(url: String, apiKey: String, payload: String?): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = if (payload == null) "GET" else "POST"
            connectTimeout = 20_000
            readTimeout = 120_000
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Accept", "application/json")
            if (payload != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        try {
            if (payload != null) {
                connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (code !in 200..299) {
                throw ApiError(messageOf(text, code))
            }
            return text
        } finally {
            connection.disconnect()
        }
    }

    private fun messageOf(body: String, code: Int): String {
        val fromApi = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message")
        }.getOrNull()
        return fromApi?.takeIf { it.isNotBlank() } ?: "HTTP $code"
    }
}
