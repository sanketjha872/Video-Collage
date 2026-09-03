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

    fun score(detection: FaceDetection, frameWidth: Int, frameHeight: Int): Float {
        // 1. Edge clipping penalty
        if (isClipped(detection, frameWidth, frameHeight)) {
            return 0.01f // Hard penalty for clipped faces
        }

        // 2. Frontality score (35%)
        val frontalityScore = calculateFrontalityScore(detection.headEulerAngleY, detection.headEulerAngleX)

        // 3. Sharpness score (25%)
        val sharpnessScore = calculateSharpnessScore(detection.sourceFrame)

        // 4. Eyes-open score (30%)
        val eyesOpenScore = ((detection.leftEyeOpenProbability ?: 0.5f) + (detection.rightEyeOpenProbability ?: 0.5f)) / 2f

        // 5. Smile score (10%)
        val smileScore = detection.smilingProbability ?: 0.5f

        // Weighted sum
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
        // Smaller absolute angles = higher score.
        // Normalize: 1 - (min(abs(yaw), 45) + min(abs(pitch), 45)) / 90
        val clampedYaw = min(abs(yaw), 45f)
        val clampedPitch = min(abs(pitch), 45f)
        return 1f - (clampedYaw + clampedPitch) / 90f
    }

    private fun calculateSharpnessScore(bitmap: Bitmap): Float {
        // Downsample for speed and to reduce noise sensitivity
        val scaledWidth = 64
        val scaledHeight = 64
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, false)

        val pixels = IntArray(scaledWidth * scaledHeight)
        scaledBitmap.getPixels(pixels, 0, scaledWidth, 0, 0, scaledWidth, scaledHeight)

        // Convert to grayscale
        val gray = FloatArray(pixels.size)
        for (i in pixels.indices) {
            val color = pixels[i]
            gray[i] = (Color.red(color) * 0.299f + Color.green(color) * 0.587f + Color.blue(color) * 0.114f)
        }

        // Apply Laplacian operator:
        // [ 0,  1,  0 ]
        // [ 1, -4,  1 ]
        // [ 0,  1,  0 ]
        val laplacian = FloatArray(gray.size)
        var sum = 0f
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
            }
        }

        val mean = sum / laplacian.size
        var variance = 0f
        for (value in laplacian) {
            variance += (value - mean) * (value - mean)
        }
        variance /= laplacian.size

        // Normalize: variance / 500f clamped to 0..1
        // (500 is an empirical starting point for Laplacian variance on 64x64 face crops)
        return (variance / 500f).coerceIn(0f, 1f)
    }

    companion object {
        private const val WEIGHT_FRONTALITY = 0.35f
        private const val WEIGHT_EYES_OPEN = 0.30f
        private const val WEIGHT_SHARPNESS = 0.25f
        private const val WEIGHT_SMILE = 0.10f
    }
}
