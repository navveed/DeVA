package com.deva.voice.triggers

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeVANotificationListenerService : NotificationListenerService() {

    private val TAG = "DeVANotification"
    private lateinit var triggerManager: TriggerManager

    override fun onCreate() {
        super.onCreate()
        triggerManager = TriggerManager.getInstance(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        Log.d(TAG, "Notification posted from package: $packageName")

        if (packageName == this.packageName) {
            Log.d(TAG, "Ignoring notification from own package.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val notificationTriggers = triggerManager.getTriggers()
                .filter { it.type == TriggerType.NOTIFICATION && it.isEnabled }

            // First, check for the "All Applications" trigger
            var matchingTrigger = notificationTriggers.find { it.packageName == "*" }

            // If no "All Applications" trigger is found, check for a specific app trigger
            if (matchingTrigger == null) {
                matchingTrigger = notificationTriggers.find { it.packageName == packageName }
            }

            if (matchingTrigger != null) {
                val extras = sbn.notification.extras
                val title = extras.getString("android.title") ?: ""
                val text = extras.getCharSequence("android.text")?.toString() ?: ""
                val notificationContent = "Notification Content: $title - $text"
                val finalInstruction = "${matchingTrigger.instruction}\n\n$notificationContent"

                Log.d(TAG, "Found matching trigger for package: $packageName. Executing instruction: $finalInstruction")
                // Use the TriggerReceiver to start the agent service
                val intent = android.content.Intent(this@DeVANotificationListenerService, TriggerReceiver::class.java).apply {
                    action = TriggerReceiver.ACTION_EXECUTE_TASK
                    putExtra(TriggerReceiver.EXTRA_TASK_INSTRUCTION, finalInstruction)
                }
                sendBroadcast(intent)
            }
        }
    }
}




