package com.deva.voice.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.deva.voice.R
import com.deva.voice.MainActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VisionOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(1002, createNotification())
        addOverlayView()
    }

    private fun addOverlayView() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        overlayView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null)
        
        // Initialize Views
        val closeBtn = overlayView?.findViewById<View>(R.id.close_btn)
        val iconBtn = overlayView?.findViewById<View>(R.id.floating_icon)
        val resultCard = overlayView?.findViewById<View>(R.id.result_card)
        val resultText = overlayView?.findViewById<android.widget.TextView>(R.id.result_text)

        // Close Button Logic
        closeBtn?.setOnClickListener {
            stopSelf()
        }

        // Main Trigger Logic
        iconBtn?.setOnClickListener {
            // Show "Scanning" state
            resultCard?.visibility = View.VISIBLE
            resultText?.text = "👀 Looking..."
            
            // Start Capture Process in Background
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                try {
                    // 1. Trigger Screen Capture Service
                    com.deva.voice.services.ScreenCaptureService.captureResultDeferred = kotlinx.coroutines.CompletableDeferred()
                    val intent = Intent(this@VisionOverlayService, com.deva.voice.services.ScreenCaptureService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }

                    // 2. Wait for Bitmap with Timeout
                    val bitmap = kotlinx.coroutines.withTimeoutOrNull(5000) {
                        com.deva.voice.services.ScreenCaptureService.captureResultDeferred?.await()
                    }
                    
                    if (bitmap != null) {
                        resultText?.text = "🧠 Thinking..."
                        
                        // 3. Process with Gemini (Background Thread)
                         kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                             try {
                                 val processedBitmap = com.deva.voice.utilities.GeminiVisionHelper.prepareBitmapForGemini(bitmap)
                                 
                                 val response = com.deva.voice.api.GeminiApi.generateContent(
                                     prompt = """
                                         You are DeVA, a smart, casual, and witty AI friend.
                                         Look at this screenshot and tell me what is happening or what it says as if you are sitting next to me.
                                         RULES:
                                         1. If there is text/article: SUMMARIZE it.
                                         2. If image/meme: REACT to it.
                                         3. IGNORE system bars/battery.
                                         4. Be extremely concise (1-2 sentences).
                                     """.trimIndent(),
                                     image = processedBitmap,
                                     context = this@VisionOverlayService
                                 )
                                 
                                 // 4. Update UI
                                 kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                     resultText?.text = response ?: "I couldn't see that clearly."
                                     
                                     // Optional: Auto-hide after 15 seconds (longer reading time)
                                     android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                         resultCard?.visibility = View.GONE
                                     }, 15000)
                                 }
                             } catch (e: Exception) {
                                  kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                     resultText?.text = "🧠 Brain Freeze: ${e.localizedMessage}"
                                  }
                             }
                         }
                    } else {
                        resultText?.text = "❌ Capture Failed. Try opening the app and granting permission again."
                    }
                } catch (e: Exception) {
                    resultText?.text = "❌ Error: ${e.localizedMessage}"
                }
            }
        }

        windowManager?.addView(overlayView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (overlayView != null) windowManager?.removeView(overlayView)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "OverlayServiceChannel",
                "Vision Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "OverlayServiceChannel")
            .setContentTitle("DeVA Vision Active")
            .setContentText("Tap the floating icon to analyze.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }
}




