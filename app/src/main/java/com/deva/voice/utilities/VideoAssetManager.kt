package com.deva.voice.utilities

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * Manages downloading and storing the wake word demo video from a remote URL.
 */
object VideoAssetManager {
    private const val TAG = "VideoAssetManager"
    private const val VIDEO_FILE_NAME = "wake_up_demo.mp4"

    /**
     * The public-facing function to get the video file.
     * It ensures the video is downloaded if not already present.
     */
    suspend fun getVideoFile(context: Context, url: String): File? {
        val videoFile = File(context.filesDir, VIDEO_FILE_NAME)
        if (!videoFile.exists()) {
            Log.d(TAG, "Video not found locally. Attempting to download.")
            val success = downloadVideo(context, url)
            if (!success) {
                return null // Return null if download fails
            }
        } else {
            Log.d(TAG, "Video already exists locally.")
        }
        return videoFile
    }

    /**
     * Handles the actual download logic.
     * This version is corrected to fix the "Unreachable code" error and uses a more robust
     * method for writing the file.
     */
    private suspend fun downloadVideo(context: Context, url: String): Boolean = withContext(Dispatchers.IO) {
        val videoFile = File(context.filesDir, VIDEO_FILE_NAME)
        val tempFile = File.createTempFile("video_download", ".tmp", context.cacheDir)

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to download video. Code: ${response.code}")
                response.close() // Ensure response is closed on failure
                return@withContext false
            }

            // This is the main logic block. It correctly handles a potentially null response body.
            response.body?.let { body ->
                // The 'use' block automatically closes the streams, even if an error occurs.
                body.byteStream().use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        // A simple and robust way to copy the downloaded bytes to the temp file.
                        inputStream.copyTo(outputStream)
                    }
                }

                // IMPORTANT: This logic is now INSIDE the 'let' block.
                // It will only be reached if the body was not null and the copy was successful.
                tempFile.renameTo(videoFile)
                Log.d(TAG, "Successfully downloaded and saved video to ${videoFile.absolutePath}")
                return@withContext true // Success!

            } ?: run {
                // This block runs only if response.body was null.
                Log.e(TAG, "Response body was null.")
                return@withContext false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading video", e)
            // Clean up the temp file if it exists after an error.
            if (tempFile.exists()) {
                tempFile.delete()
            }
            return@withContext false
        }
    }
}




