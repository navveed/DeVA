package com.deva.voice.utilities

import android.graphics.Bitmap
import android.util.Log
import kotlin.math.min

object GeminiVisionHelper {

    private const val MAX_DIMENSION = 1024

    fun prepareBitmapForGemini(original: Bitmap): Bitmap {
        val width = original.width
        val height = original.height
        
        // If image is already small enough, return it (or a copy if immutable)
        if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) {
            return original
        }

        val ratio = min(
            MAX_DIMENSION.toFloat() / width,
            MAX_DIMENSION.toFloat() / height
        )

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        Log.d("GeminiVisionHelper", "Resizing bitmap from ${width}x${height} to ${newWidth}x${newHeight}")

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
    }
}




