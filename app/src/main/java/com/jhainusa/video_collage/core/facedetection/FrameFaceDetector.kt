package com.jhainusa.video_collage.core.facedetection

import android.graphics.Bitmap
import com.jhainusa.video_collage.domain.model.FaceDetection

/**
 * Interface for detecting faces within a single video frame.
 */
interface FrameFaceDetector {
    /**
     * Detects faces in the provided [frame] at the given [timestampMs].
     *
     * @param frame The video frame to process.
     * @param timestampMs The timestamp of the frame in the source video.
     * @return A list of [FaceDetection] objects found in the frame.
     */
    suspend fun detectFaces(frame: Bitmap, timestampMs: Long): List<FaceDetection>
}

class FaceDetectorWrapper : FrameFaceDetector {
    override suspend fun detectFaces(frame: Bitmap, timestampMs: Long): List<FaceDetection> {
        TODO("Not yet implemented")
    }
}
