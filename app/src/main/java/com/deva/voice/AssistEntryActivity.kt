package com.deva.voice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat

class AssistEntryActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAssistLaunch(intent)
        // No UI — finish immediately
        finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleAssistLaunch(intent)
        finish()
    }

    private fun handleAssistLaunch(intent: Intent?) {
        Log.d("AssistEntryActivity", "Assistant invoked via ACTION_ASSIST, intent=$intent")

        // If agent already running, you can signal it to focus/show UI instead of starting again
        if (!ConversationalAgentService.isRunning) {
            val serviceIntent = Intent(this, ConversationalAgentService::class.java).apply {
                action = "com.deva.voice.ACTION_START_FROM_ASSIST"
                putExtra("source", "assist_gesture")       // optional metadata
            }
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            // e.g., tell the service to bring its overlay/mic UI to front
            sendBroadcast(Intent("com.deva.voice.ACTION_SHOW_OVERLAY"))
        }
    }
}




