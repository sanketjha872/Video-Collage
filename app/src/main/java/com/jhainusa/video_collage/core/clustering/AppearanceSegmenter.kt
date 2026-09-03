package com.jhainusa.video_collage.core.clustering

import com.jhainusa.video_collage.domain.model.Appearance
import com.jhainusa.video_collage.domain.model.FaceDetection
import java.util.UUID

/**
 * Interface for segmenting face detections into continuous appearances.
 */
interface AppearanceSegmenter {
    fun segment(detections: List<FaceDetection>): List<Appearance>
}

/**
 * Segments a sequence of face detections into continuous appearances based on tracking IDs
 * and temporal proximity.
 */
class TrackingBasedAppearanceSegmenter : AppearanceSegmenter {

    override fun segment(detections: List<FaceDetection>): List<Appearance> {
        if (detections.isEmpty()) return emptyList()

        // 1. Group by trackingId
        val detectionsById = detections.filter { it.trackingId != null }
            .groupBy { it.trackingId!! }

        val allAppearances = mutableListOf<Appearance>()

        for ((trackingId, idDetections) in detectionsById) {
            val sortedDetections = idDetections.sortedBy { it.frameTimestampMs }
            
            var currentSegment = mutableListOf<FaceDetection>()
            
            for (i in sortedDetections.indices) {
                val detection = sortedDetections[i]
                
                if (currentSegment.isEmpty()) {
                    currentSegment.add(detection)
                } else {
                    val prevDetection = currentSegment.last()
                    val gap = detection.frameTimestampMs - prevDetection.frameTimestampMs
                    
                    if (gap <= MAX_GAP_TOLERANCE_MS) {
                        currentSegment.add(detection)
                    } else {
                        // Gap too large, close current segment and start new one
                        val appearance = createAppearance(currentSegment, trackingId)
                        if (isSignificant(appearance)) {
                            allAppearances.add(appearance)
                        }
                        currentSegment = mutableListOf(detection)
                    }
                }
            }
            
            if (currentSegment.isNotEmpty()) {
                val appearance = createAppearance(currentSegment, trackingId)
                if (isSignificant(appearance)) {
                    allAppearances.add(appearance)
                }
            }
        }

        return allAppearances.sortedBy { it.startMs }
    }

    private fun createAppearance(detections: List<FaceDetection>, trackingId: Int): Appearance {
        val bestDetection = detections.maxByOrNull { it.qualityScore ?: 0f } ?: detections.first()
        
        return Appearance(
            id = UUID.randomUUID().toString(),
            trackingId = trackingId,
            startMs = detections.first().frameTimestampMs,
            endMs = detections.last().frameTimestampMs,
            detections = detections,
            representativeEmbedding = bestDetection.embedding ?: FloatArray(0),
            bestDetection = bestDetection
        )
    }

    private fun isSignificant(appearance: Appearance): Boolean {
        val duration = appearance.endMs - appearance.startMs
        return duration >= MIN_APPEARANCE_DURATION_MS
    }

    companion object {
        /**
         * Maximum allowed gap between consecutive detections of the same tracking ID
         * to be considered the same appearance. Set to 500ms to tolerate ~3-5 missed frames
         * depending on sampling rate.
         */
        private const val MAX_GAP_TOLERANCE_MS = 500L

        /**
         * Minimum duration for an appearance to be considered valid.
         * Detections shorter than this are likely artifacts (e.g. blurred whip-pans).
         */
        private const val MIN_APPEARANCE_DURATION_MS = 200L
    }
}
