package com.jhainusa.video_collage.core.quality

import android.graphics.Bitmap
import android.graphics.Color
import com.jhainusa.video_collage.domain.model.FaceDetection
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Evaluates the quality of a detected face to determine how suitable it is as a representative shot.
 * Considers frontality, sharpness, eyes-open probability, smile probability, and edge clipping.
 */
class FaceQualityScorer {

    /**
     * Calculates a composite quality score for the given [detection].
     * 
     * @param detection The face detection to score.
     * @param frameWidth The width of the original video frame.
     * @param frameHeight The height of the original video frame.
     * @return A quality score in the range [0, 1].
     */
    fun score(detection: FaceDetection, frameWidth: Int, frameHeight: Int): Float {
        // 1. Edge clipping penalty: favor full-face-visible shots
        if (isClipped(detection, frameWidth, frameHeight)) {
            return CLIPPED_PENALTY_SCORE
        }

        // 2. Frontality score: favor faces looking directly at the camera
        val frontalityScore = calculateFrontalityScore(detection.headEulerAngleY, detection.headEulerAngleX)

        // 3. Sharpness score: favor clear, non-blurry shots
        val sharpnessScore = calculateSharpnessScore(detection.sourceFrame)

        // 4. Eyes-open score: favor shots with eyes open
        val eyesOpenScore = ((detection.leftEyeOpenProbability ?: DEFAULT_PROBABILITY) + 
                             (detection.rightEyeOpenProbability ?: DEFAULT_PROBABILITY)) / 2f

        // 5. Smile score: favor pleasant/smiling expressions
        val smileScore = detection.smilingProbability ?: DEFAULT_PROBABILITY

        // Weighted sum for the composite score
        return (frontalityScore * WEIGHT_FRONTALITY) +
                (eyesOpenScore * WEIGHT_EYES_OPEN) +
                (sharpnessScore * WEIGHT_SHARPNESS) +
                (smileScore * WEIGHT_SMILE)
    }

    private fun isClipped(detection: FaceDetection, frameWidth: Int, frameHeight: Int): Boolean {
        val bbox = detection.boundingBox
        val margin = 5 // pixels
        return bbox.left <= margin || bbox.top <= margin ||
                bbox.right >= frameWidth - margin || bbox.bottom >= frameHeight - margin
    }

    private fun calculateFrontalityScore(yaw: Float, pitch: Float): Float {
        // Smaller absolute angles (facing camera) result in higher scores.
        // We consider 45 degrees as the limit for a "good" shot.
        val clampedYaw = min(abs(yaw), 45f)
        val clampedPitch = min(abs(pitch), 45f)
        return 1f - (clampedYaw + clampedPitch) / 90f
    }

    private fun calculateSharpnessScore(bitmap: Bitmap): Float {
        // Downsample to 64x64 for speed and to reduce high-frequency noise sensitivity
        val scaledWidth = 64
        val scaledHeight = 64
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, false)

        val pixels = IntArray(scaledWidth * scaledHeight)
        scaledBitmap.getPixels(pixels, 0, scaledWidth, 0, 0, scaledWidth, scaledHeight)

        // Convert to grayscale using standard luminance weights
        val gray = FloatArray(pixels.size)
        for (i in pixels.indices) {
            val color = pixels[i]
            gray[i] = (Color.red(color) * 0.299f + Color.green(color) * 0.587f + Color.blue(color) * 0.114f)
        }

        // Apply a simple 3x3 Laplacian operator (4-neighbor) manually:
        // [ 0,  1,  0 ]
        // [ 1, -4,  1 ]
        // [ 0,  1,  0 ]
        val laplacian = FloatArray(gray.size)
        var sum = 0f
        var count = 0
        for (y in 1 until scaledHeight - 1) {
            for (x in 1 until scaledWidth - 1) {
                val idx = y * scaledWidth + x
                val valL = gray[idx - 1]
                val valR = gray[idx + 1]
                val valT = gray[idx - scaledWidth]
                val valB = gray[idx + scaledWidth]
                val valC = gray[idx]

                val lap = valL + valR + valT + valB - 4 * valC
                laplacian[idx] = lap
                sum += lap
                count++
            }
        }

        // Calculate variance of the Laplacian result
        val mean = sum / count
        var variance = 0f
        for (y in 1 until scaledHeight - 1) {
            for (x in 1 until scaledWidth - 1) {
                val idx = y * scaledWidth + x
                val diff = laplacian[idx] - mean
                variance += diff * diff
            }
        }
        variance /= count

        // Normalize variance against an empirical expected range (variance / 500f)
        return (variance / SHARPNESS_NORMALIZATION_FACTOR).coerceIn(0f, 1f)
    }

    companion object {
        // Weights for the composite quality score
        private const val WEIGHT_FRONTALITY = 0.35f
        private const val WEIGHT_EYES_OPEN = 0.30f
        private const val WEIGHT_SHARPNESS = 0.25f
        private const val WEIGHT_SMILE = 0.10f

        private const val DEFAULT_PROBABILITY = 0.5f
        private const val CLIPPED_PENALTY_SCORE = 0.01f
        private const val SHARPNESS_NORMALIZATION_FACTOR = 500f
    }
}
