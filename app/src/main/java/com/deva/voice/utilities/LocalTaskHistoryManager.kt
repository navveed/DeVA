package com.deva.voice.utilities

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

/**
 * A local storage manager for task history.
 * Uses SharedPreferences with JSON serialization to store task history locally.
 */
class LocalTaskHistoryManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "deva_task_history"
        private const val KEY_TASK_HISTORY = "task_history_json"
        private const val MAX_HISTORY_SIZE = 100 // Keep last 100 tasks
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Data class for storing task history locally (doesn't depend on Firebase Timestamp)
     */
    data class LocalTaskHistoryItem(
        val task: String,
        val status: String,
        val startedAtMillis: Long,
        val completedAtMillis: Long?,
        val success: Boolean?,
        val errorMessage: String?
    ) {
        fun getStatusEmoji(): String {
            return when (status.lowercase()) {
                "started" -> "🔄"
                "completed" -> if (success == true) "✅" else "❌"
                "failed" -> "❌"
                else -> "⏳"
            }
        }

        fun getFormattedStartTime(): String {
            val date = Date(startedAtMillis)
            val formatter = java.text.SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", java.util.Locale.getDefault())
            return formatter.format(date)
        }

        fun getFormattedCompletionTime(): String {
            return completedAtMillis?.let {
                val date = Date(it)
                val formatter = java.text.SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", java.util.Locale.getDefault())
                formatter.format(date)
            } ?: "Not completed"
        }

        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("task", task)
                put("status", status)
                put("startedAtMillis", startedAtMillis)
                put("completedAtMillis", completedAtMillis ?: JSONObject.NULL)
                put("success", success ?: JSONObject.NULL)
                put("errorMessage", errorMessage ?: JSONObject.NULL)
            }
        }

        companion object {
            fun fromJson(json: JSONObject): LocalTaskHistoryItem {
                return LocalTaskHistoryItem(
                    task = json.optString("task", ""),
                    status = json.optString("status", ""),
                    startedAtMillis = json.optLong("startedAtMillis", 0),
                    completedAtMillis = if (json.isNull("completedAtMillis")) null else json.optLong("completedAtMillis"),
                    success = if (json.isNull("success")) null else json.optBoolean("success"),
                    errorMessage = if (json.isNull("errorMessage")) null else json.optString("errorMessage")
                )
            }
        }
    }

    /**
     * Add a new task to history (when task starts)
     */
    fun addTask(task: String): LocalTaskHistoryItem {
        val item = LocalTaskHistoryItem(
            task = task,
            status = "started",
            startedAtMillis = System.currentTimeMillis(),
            completedAtMillis = null,
            success = null,
            errorMessage = null
        )
        
        val history = getTaskHistory().toMutableList()
        history.add(0, item) // Add at beginning (most recent first)
        
        // Trim to max size
        val trimmed = history.take(MAX_HISTORY_SIZE)
        saveHistory(trimmed)
        
        return item
    }

    /**
     * Update a task's completion status
     */
    fun updateTaskCompletion(task: String, success: Boolean, errorMessage: String? = null) {
        val history = getTaskHistory().toMutableList()
        
        // Find the most recent matching task (should be at the beginning)
        val index = history.indexOfFirst { it.task == task && it.status == "started" }
        
        if (index >= 0) {
            val original = history[index]
            val updated = original.copy(
                status = "completed",
                completedAtMillis = System.currentTimeMillis(),
                success = success,
                errorMessage = errorMessage
            )
            history[index] = updated
            saveHistory(history)
        } else {
            // If not found, still add a completed entry
            val item = LocalTaskHistoryItem(
                task = task,
                status = "completed",
                startedAtMillis = System.currentTimeMillis(),
                completedAtMillis = System.currentTimeMillis(),
                success = success,
                errorMessage = errorMessage
            )
            history.add(0, item)
            saveHistory(history.take(MAX_HISTORY_SIZE))
        }
    }

    /**
     * Get all task history items, sorted by most recent first
     */
    fun getTaskHistory(): List<LocalTaskHistoryItem> {
        val jsonString = prefs.getString(KEY_TASK_HISTORY, null) ?: return emptyList()
        
        return try {
            val jsonArray = JSONArray(jsonString)
            (0 until jsonArray.length()).mapNotNull { i ->
                try {
                    LocalTaskHistoryItem.fromJson(jsonArray.getJSONObject(i))
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Clear all task history
     */
    fun clearHistory() {
        prefs.edit().remove(KEY_TASK_HISTORY).apply()
    }

    private fun saveHistory(history: List<LocalTaskHistoryItem>) {
        val jsonArray = JSONArray()
        history.forEach { item ->
            jsonArray.put(item.toJson())
        }
        prefs.edit().putString(KEY_TASK_HISTORY, jsonArray.toString()).apply()
    }
}




