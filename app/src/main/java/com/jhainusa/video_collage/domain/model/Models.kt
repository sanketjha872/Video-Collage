package com.jhainusa.video_collage.domain.model

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * Represents a single face detected in a specific video frame.
 */
data class FaceDetection(
    /** Timestamp in the video when this face was detected. */
    val frameTimestampMs: Long,
    /** The bounding box of the face within the source frame. */
    val boundingBox: Rect,
    /** Optional tracking ID assigned by the detector if available. */
    val trackingId: Int?,
    /** Head pose yaw angle in degrees. Used for frontality scoring. */
    val headEulerAngleY: Float,
    /** Head pose pitch angle in degrees. Used for head pose scoring. */
    val headEulerAngleX: Float,
    /** Probability that the left eye is open [0, 1]. */
    val leftEyeOpenProbability: Float?,
    /** Probability that the right eye is open [0, 1]. */
    val rightEyeOpenProbability: Float?,
    /** Probability that the person is smiling [0, 1]. */
    val smilingProbability: Float?,
    /** Feature vector representing the face, used for identity clustering. Filled in later by FaceEmbedder. */
    val embedding: FloatArray?,
    /** Aggregated quality score based on frontality, sharpness, eyes-open, and smile. Filled in later by QualityScorer. */
    val qualityScore: Float?,
    /** A tight crop of the face, used for generating embeddings. */
    val faceCrop: Bitmap,
    /** The source frame from the video or a generous crop for display. */
    val sourceFrame: Bitmap
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FaceDetection
        return frameTimestampMs == other.frameTimestampMs && trackingId == other.trackingId
    }

    override fun hashCode(): Int {
        var result = frameTimestampMs.hashCode()
        result = 31 * result + (trackingId ?: 0)
        return result
    }
}

/**
 * Represents a continuous visible segment of a person in the video.
 * An appearance starts when a person's face becomes clearly visible and ends when it's no longer visible.
 */
data class Appearance(
    val id: String,
    /** The tracking ID from the detector that defines this continuous segment. */
    val trackingId: Int?,
    val startMs: Long,
    val endMs: Long,
    /** All detections that make up this continuous appearance. */
    val detections: List<FaceDetection>,
    /** The embedding of the highest quality detection in this segment, used for cross-segment clustering. */
    val representativeEmbedding: FloatArray,
    /** The detection with the highest quality score in this segment. */
    val bestDetection: FaceDetection
)

/**
 * Represents a unique person identified across multiple separate appearances in the video.
 */
data class Person(
    val id: String,
    /** List of separate appearances of this same person (e.g. leaves frame, comes back later). */
    val appearances: List<Appearance>,
    /** Total number of separate appearances. */
    val appearanceCount: Int,
    /** The best representative shot for this person across all their appearances. */
    val representativeShot: Bitmap,
    /** The quality score of the representative shot. */
    val representativeQualityScore: Float
)

/**
 * Represents the current state of the video processing pipeline, allowing UI to show progress.
 */
sealed class ProcessingState {
    object Idle : ProcessingState()
    data class ExtractingFrames(val progress: Float) : ProcessingState()
    data class DetectingFaces(val progress: Float) : ProcessingState()
    data class GeneratingEmbeddings(val progress: Float) : ProcessingState()
    object ClusteringIdentities : ProcessingState()
    object BuildingCollage : ProcessingState()
    data class Complete(val persons: List<Person>, val collage: Bitmap) : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}
