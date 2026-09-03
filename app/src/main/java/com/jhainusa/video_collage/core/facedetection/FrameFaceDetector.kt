package com.jhainusa.video_collage.core.facedetection

import android.graphics.Bitmap
import com.jhainusa.video_collage.domain.model.FaceDetection

/**
 * Interface for detecting faces within video frames.
 */
interface FrameFaceDetector {
    /**
     * Detects faces in a sequence of frames.
     * 
     * @param frames List of timestamp and bitmap pairs.
     * @param onProgress Callback for progress (0f to 1f).
     * @return Flattened list of all detected faces across all frames.
     */
    suspend fun detectFaces(
        frames: List<Pair<Long, Bitmap>>,
        onProgress: (Float) -> Unit
    ): List<FaceDetection>
}
