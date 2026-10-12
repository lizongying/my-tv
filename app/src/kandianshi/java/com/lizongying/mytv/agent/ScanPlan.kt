package com.lizongying.mytv.agent

import org.json.JSONArray
import org.json.JSONObject

/** Persist a complete batch before advancing; only failed routes enter the following round. */
class ScanPlan(val state: JSONObject) {
    val queue: List<String> get() = AgentConfigStore.strings(state.getJSONArray("queue"))
    val cursor: Int get() = state.optInt("cursor")
    val round: Int get() = state.optInt("round")
    val done: Boolean get() = state.optBoolean("done")
    init {
        require(round in 1..3 && cursor in 0..queue.size && queue.size <= 2000)
    }
    fun pending(limit: Int = 12): List<String> = queue.drop(cursor).take(limit)
    fun accept(results: List<Pair<String, Boolean>>) {
        require(!done && results.isNotEmpty() && results.map { it.first } == pending(results.size))
        val failed = AgentConfigStore.strings(state.getJSONArray("failed")) + results.filterNot { it.second }.map { it.first }
        state.put("failed", JSONArray(failed)).put("cursor", cursor + results.size)
            .put("checked", state.optInt("checked") + results.size)
    }
    fun finishRound(): Boolean {
        require(cursor == queue.size)
        val failed = AgentConfigStore.strings(state.getJSONArray("failed"))
        if (failed.isEmpty() || round == 3) { state.put("done", true); return false }
        state.put("queue", JSONArray(failed)).put("failed", JSONArray()).put("cursor", 0).put("round", round + 1)
        return true
    }
    companion object {
        fun create(urls: List<String>, now: Long): ScanPlan {
            val distinct = urls.distinct().take(2000)
            return ScanPlan(JSONObject().put("queue", JSONArray(distinct)).put("failed", JSONArray()).put("cursor", 0)
                .put("round", 1).put("total", distinct.size).put("checked", 0).put("done", false).put("started", now))
        }
    }
}
