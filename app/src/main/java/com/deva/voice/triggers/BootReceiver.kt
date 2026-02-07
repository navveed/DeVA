package com.deva.voice.triggers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed. Rescheduling alarms.")
            val triggerManager = TriggerManager.getInstance(context)

            // It's good practice to do this work off the main thread
            // Start the TriggerMonitoringService
            val serviceIntent = Intent(context, TriggerMonitoringService::class.java)
            context.startService(serviceIntent)
            Log.d(TAG, "Started TriggerMonitoringService on boot.")

            CoroutineScope(Dispatchers.IO).launch {
                val triggers = triggerManager.getTriggers()
                val scheduledTriggers = triggers.filter { it.isEnabled && it.type == TriggerType.SCHEDULED_TIME }
                scheduledTriggers.forEach { trigger ->
                    // In the future, we might have different logic for rescheduling
                    // but for now, just calling schedule is fine as it will recreate the alarm.
                    triggerManager.updateTrigger(trigger)
                }
                Log.d(TAG, "Finished rescheduling ${scheduledTriggers.size} alarms.")
            }
        }
    }
}




