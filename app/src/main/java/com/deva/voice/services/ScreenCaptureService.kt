package com.deva.voice.services

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.deva.voice.R
import kotlinx.coroutines.CompletableDeferred

class ScreenCaptureService : Service() {

    companion object {
        private const val CHANNEL_ID = "ScreenCaptureChannel"
        private const val NOTIFICATION_ID = 1001
        private const val VIRTUAL_DISPLAY_NAME = "DeVA_ScreenCapture"

        // Singleton deferred to return the result to the caller
         var captureResultDeferred: CompletableDeferred<Bitmap?>? = null
         var projectionIntent: Intent? = null
         var resultCode: Int = 0
    }

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var windowManager: WindowManager? = null
    private var screenDensity: Int = 0
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val metrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getMetrics(metrics)
        screenDensity = metrics.densityDpi
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (projectionIntent == null) {
            Log.e("ScreenCaptureService", "Projection intent is null. Cannot start capture.")
            captureResultDeferred?.complete(null) // Ensure we don't hang the caller
            stopSelf()
            return START_NOT_STICKY
        }

        startCapture()
        return START_NOT_STICKY
    }

    private fun startCapture() {
        try {
            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, projectionIntent!!)
            
            // Register callback to handle stop
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    stopSelf()
                }
            }, null)

            // Initialize ImageReader
            // We use a slightly lower resolution (e.g. half) if screen is huge to save memory/tokens, 
            // but for now let's capture full and resize later.
            imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )

            // Capture one frame after a short delay to allow the virtual display to render
            Handler(Looper.getMainLooper()).postDelayed({
                captureFrame()
            }, 500) 

        } catch (e: Exception) {
            Log.e("ScreenCaptureService", "Error starting capture", e)
            captureResultDeferred?.complete(null)
            stopSelf()
        }
    }

    private fun captureFrame() {
        try {
            val image = imageReader?.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * screenWidth

                // Create bitmap
                val bitmap = Bitmap.createBitmap(
                    screenWidth + rowPadding / pixelStride,
                    screenHeight,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()

                // Crop valid area (remove padding)
                val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
                
                // Return result
                captureResultDeferred?.complete(croppedBitmap)
            } else {
                Log.e("ScreenCaptureService", "Image is null")
                captureResultDeferred?.complete(null)
            }
        } catch (e: Exception) {
            Log.e("ScreenCaptureService", "Error capturing frame", e)
            captureResultDeferred?.complete(null)
        } finally {
            cleanup()
            stopSelf()
        }
    }

    private fun cleanup() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        mediaProjection = null
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture Service",
                NotificationManager.IMPORTANCE_LOW 
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DeVA Vision")
            .setContentText("Analyzing screen content...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure this resource exists or use default
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}




