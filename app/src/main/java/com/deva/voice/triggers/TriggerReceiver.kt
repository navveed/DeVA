package com.deva.voice.triggers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.deva.voice.v2.AgentService
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class TriggerReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_EXECUTE_TASK = "com.deva.voice.action.EXECUTE_TASK"
        const val EXTRA_TASK_INSTRUCTION = "com.deva.voice.EXTRA_TASK_INSTRUCTION"
        const val EXTRA_TRIGGER_ID = "com.deva.voice.EXTRA_TRIGGER_ID"
        private const val TAG = "TriggerReceiver"
        private const val DEBOUNCE_INTERVAL_MS = 60 * 1000 // 1 minute

        // Cache to store recent task instructions and their timestamps
        private val recentTasks = ConcurrentHashMap<String, Long>()
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.w(TAG, "Received null context or intent, cannot proceed.")
            return
        }

        if (intent.action == ACTION_EXECUTE_TASK) {
            val taskInstruction = intent.getStringExtra(EXTRA_TASK_INSTRUCTION)

            if (taskInstruction.isNullOrBlank()) {
                Log.e(TAG, "Received execute task action but instruction was null or empty.")
                return
            }

            val currentTime = System.currentTimeMillis()
            val lastExecutionTime = recentTasks[taskInstruction]

            if (lastExecutionTime != null && (currentTime - lastExecutionTime) < DEBOUNCE_INTERVAL_MS) {
                Log.d(TAG, "Debouncing duplicate task: '$taskInstruction'")
                return
            }

            // Update the cache with the new execution time
            recentTasks[taskInstruction] = currentTime

            Log.d(TAG, "Received task to execute: '$taskInstruction'")

            // Directly start the v2 AgentService
            AgentService.start(context, taskInstruction)

            // Reschedule the alarm for the next day
            val triggerId = intent.getStringExtra(EXTRA_TRIGGER_ID)
            if (triggerId != null) {
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    TriggerManager.getInstance(context).rescheduleTrigger(triggerId)
                }
            }

            // Clean up old entries from the cache
            cleanupRecentTasks(currentTime)
        }
    }

    private fun cleanupRecentTasks(currentTime: Long) {
        // For simplicity, this example cleans up tasks older than the debounce interval.
        // A more sophisticated approach might use a background thread or a more complex cache eviction policy.
        val iterator = recentTasks.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if ((currentTime - entry.value) > DEBOUNCE_INTERVAL_MS) {
                iterator.remove()
            }
        }
    }
}




