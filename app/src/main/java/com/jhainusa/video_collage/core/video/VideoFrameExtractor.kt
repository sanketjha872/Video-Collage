package com.jhainusa.video_collage.core.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Interface for extracting bitmaps from a video file at regular intervals.
 */
interface VideoFrameExtractor {
    /**
     * Extracts frames from the video at [FRAME_SAMPLE_INTERVAL_MS] intervals.
     * @param uri The content URI of the video.
     * @param onProgress Callback with progress from 0.0 to 1.0.
     * @return List of timestamp in milliseconds and the extracted Bitmap.
     */
    suspend fun extractFrames(
        uri: Uri,
        onProgress: (Float) -> Unit
    ): List<Pair<Long, Bitmap>>

    companion object {
        const val FRAME_SAMPLE_INTERVAL_MS = 140L
        private const val TAG = "VideoFrameExtractor"
    }
}

/**
 * Implementation of [VideoFrameExtractor] using [MediaMetadataRetriever].
 */
class MediaMetadataFrameExtractor(private val context: Context) : VideoFrameExtractor {

    override suspend fun extractFrames(
        uri: Uri,
        onProgress: (Float) -> Unit
    ): List<Pair<Long, Bitmap>> = withContext(Dispatchers.IO) {
        val frames = mutableListOf<Pair<Long, Bitmap>>()
        val retriever = MediaMetadataRetriever()
        
        try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLong() ?: 0L
            
            if (durationMs <= 0) {
                Log.e(TAG, "Invalid video duration: $durationMs")
                return@withContext emptyList()
            }

            for (timeMs in 0 until durationMs step VideoFrameExtractor.FRAME_SAMPLE_INTERVAL_MS) {
                try {
                    // MediaMetadataRetriever uses microseconds
                    val bitmap = retriever.getFrameAtTime(
                        timeMs * 1000,
                        MediaMetadataRetriever.OPTION_CLOSEST
                    )
                    
                    if (bitmap != null) {
                        frames.add(timeMs to bitmap)
                    } else {
                        Log.w(TAG, "Failed to decode frame at $timeMs ms")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error extracting frame at $timeMs ms: ${e.message}")
                }
                
                // Update progress
                onProgress(timeMs.toFloat() / durationMs)
            }
            
            onProgress(1.0f)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaMetadataRetriever for URI: $uri", e)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever", e)
            }
        }
        
        frames
    }

    companion object {
        private const val TAG = "MediaMetadataExtractor"
    }
}
