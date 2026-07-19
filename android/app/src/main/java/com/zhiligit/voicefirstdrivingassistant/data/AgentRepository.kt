package com.zhiligit.voicefirstdrivingassistant.data

import com.zhiligit.voicefirstdrivingassistant.model.ActionPlan
import com.zhiligit.voicefirstdrivingassistant.model.PlannedAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AgentRepository(private val baseUrl: String) {
    val isMockMode: Boolean = baseUrl.isBlank()

    suspend fun createPlan(transcript: String): ActionPlan = withContext(Dispatchers.IO) {
        if (isMockMode) return@withContext mockPlan(transcript)

        val connection = URL("${baseUrl.trimEnd('/')}/agent/plan")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 30_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            val payload = JSONObject()
                .put("transcript", transcript)
                .put("timezone", "Europe/Berlin")
                .toString()
            connection.outputStream.bufferedWriter().use { it.write(payload) }

            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = stream.bufferedReader().use { it.readText() }
            if (connection.responseCode !in 200..299) {
                error("Backend returned ${connection.responseCode}: $body")
            }
            parsePlan(body)
        } finally {
            connection.disconnect()
        }
    }

    internal fun parsePlan(json: String): ActionPlan {
        val root = JSONObject(json)
        val actionsJson = root.getJSONArray("actions")
        val actions = buildList {
            for (index in 0 until actionsJson.length()) {
                val item = actionsJson.getJSONObject(index)
                add(
                    when (item.getString("type")) {
                        "CREATE_NOTE" -> PlannedAction.CreateNote(
                            title = item.getString("title"),
                            content = item.getString("content")
                        )
                        "CREATE_REMINDER" -> PlannedAction.CreateReminder(
                            title = item.getString("title"),
                            scheduledAt = item.getString("scheduled_at")
                        )
                        else -> PlannedAction.NoAction(
                            reason = item.optString("reason", "Unsupported action")
                        )
                    }
                )
            }
        }
        return ActionPlan(
            summary = root.getString("summary"),
            requiresConfirmation = root.getBoolean("requires_confirmation"),
            actions = actions
        )
    }

    private fun mockPlan(transcript: String): ActionPlan {
        val title = transcript.substringAfter("titled", "Project Idea")
            .substringBefore("containing")
            .trim()
            .ifBlank { "Project Idea" }
        val content = transcript.substringAfter("containing", transcript).trim()
        return ActionPlan(
            summary = "Create note ‘$title’",
            requiresConfirmation = true,
            actions = listOf(PlannedAction.CreateNote(title, content))
        )
    }
}
