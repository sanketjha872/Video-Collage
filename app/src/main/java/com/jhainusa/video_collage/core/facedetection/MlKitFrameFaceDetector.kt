package com.jhainusa.video_collage.core.facedetection

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.jhainusa.video_collage.domain.model.FaceDetection as DomainFaceDetection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Implementation of [FrameFaceDetector] using Google ML Kit.
 */
class MlKitFrameFaceDetector : FrameFaceDetector {

    override suspend fun detectFaces(
        frames: List<Pair<Long, Bitmap>>,
        onProgress: (Float) -> Unit
    ): List<DomainFaceDetection> = withContext(Dispatchers.Default) {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()

        val detector = FaceDetection.getClient(options)
        val allDetections = mutableListOf<DomainFaceDetection>()
        
        try {
            frames.forEachIndexed { index, (timestampMs, bitmap) ->
                try {
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val mlKitFaces = detector.process(image).await()
                    
                    val frameDetections = mlKitFaces.map { face ->
                        val generousCrop = createGenerousCrop(bitmap, face.boundingBox)
                        
                        DomainFaceDetection(
                            frameTimestampMs = timestampMs,
                            boundingBox = face.boundingBox,
                            trackingId = face.trackingId,
                            headEulerAngleY = face.headEulerAngleY,
                            headEulerAngleX = face.headEulerAngleX,
                            leftEyeOpenProbability = face.leftEyeOpenProbability,
                            rightEyeOpenProbability = face.rightEyeOpenProbability,
                            smilingProbability = face.smilingProbability,
                            embedding = null,
                            qualityScore = null,
                            sourceFrame = generousCrop
                        )
                    }
                    allDetections.addAll(frameDetections)
                } catch (e: Exception) {
                    Log.e(TAG, "Error detecting faces at $timestampMs ms", e)
                }
                
                // Emit progress across all frames
                onProgress((index + 1).toFloat() / frames.size)
            }
        } finally {
            try {
                detector.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing ML Kit FaceDetector", e)
            }
        }
        
        allDetections
    }

    /**
     * Creates a generous crop around the detected face bounding box.
     * Expands the box by 70% in each direction (clamped to bitmap bounds).
     */
    private fun createGenerousCrop(bitmap: Bitmap, boundingBox: Rect): Bitmap {
        val width = boundingBox.width()
        val height = boundingBox.height()
        
        // Expand by 70% in each direction
        val expansionFactor = 0.7f
        val deltaW = (width * expansionFactor).toInt()
        val deltaH = (height * expansionFactor).toInt()
        
        val left = max(0, boundingBox.left - deltaW)
        val top = max(0, boundingBox.top - deltaH)
        val right = min(bitmap.width, boundingBox.right + deltaW)
        val bottom = min(bitmap.height, boundingBox.bottom + deltaH)
        
        val cropWidth = right - left
        val cropHeight = bottom - top
        
        return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
    }

    companion object {
        private const val TAG = "MlKitFrameFaceDetector"
    }
}
