package com.jhainusa.video_collage.domain.usecase

import android.net.Uri
import android.util.Log
import com.jhainusa.video_collage.core.collage.CollageRenderer
import com.jhainusa.video_collage.core.facedetection.FrameFaceDetector
import com.jhainusa.video_collage.core.quality.FaceQualityScorer
import com.jhainusa.video_collage.core.video.VideoFrameExtractor
import com.jhainusa.video_collage.domain.model.FaceDetection
import com.jhainusa.video_collage.domain.model.Person
import com.jhainusa.video_collage.domain.model.ProcessingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import java.util.UUID

/**
 * Orchestrates the video processing pipeline.
 * Simplified version: Detects faces and groups them by tracking ID.
 */
class ProcessVideoUseCase(
    private val videoFrameExtractor: VideoFrameExtractor,
    private val faceDetector: FrameFaceDetector,
    private val qualityScorer: FaceQualityScorer,
    private val collageRenderer: CollageRenderer
) {

    /**
     * Executes the pipeline and emits the current [ProcessingState].
     */
    operator fun invoke(uri: Uri): Flow<ProcessingState> = channelFlow {
        try {
            // 1. Frame Extraction
            send(ProcessingState.ExtractingFrames(0f))
            val frames = videoFrameExtractor.extractFrames(uri) { progress ->
                trySend(ProcessingState.ExtractingFrames(progress))
            }

            if (frames.isEmpty()) {
                send(ProcessingState.Error("No frames could be extracted from the video."))
                return@channelFlow
            }

            val frameWidth = frames[0].second.width
            val frameHeight = frames[0].second.height

            // 2. Face Detection
            send(ProcessingState.DetectingFaces(0f))
            val detections = faceDetector.detectFaces(frames) { progress ->
                trySend(ProcessingState.DetectingFaces(progress))
            }

            if (detections.isEmpty()) {
                send(ProcessingState.Error("No faces detected in the video."))
                return@channelFlow
            }

            // 3. Quality Scoring & Initial Grouping
            val scoredDetections = detections.map { detection ->
                val quality = qualityScorer.score(detection, frameWidth, frameHeight)
                detection.copy(qualityScore = quality)
            }

            // 4. Grouping by Tracking ID (Simple Identity)
            // If tracking ID is null, we treat it as a unique person for now.
            val persons = scoredDetections
                .groupBy { it.trackingId ?: UUID.randomUUID().hashCode() }
                .map { (trackingId, faceDetections) ->
                    val bestDetection = faceDetections.maxByOrNull { it.qualityScore ?: 0f } 
                        ?: faceDetections.first()
                    
                    Person(
                        id = trackingId.toString(),
                        trackingId = if (trackingId < 0 && faceDetections.first().trackingId == null) null else trackingId,
                        detections = faceDetections,
                        appearanceCount = faceDetections.size,
                        representativeShot = bestDetection.faceCrop,
                        representativeQualityScore = bestDetection.qualityScore ?: 0f
                    )
                }

            if (persons.isEmpty()) {
                send(ProcessingState.Error("No distinct persons could be identified."))
                return@channelFlow
            }

            // 5. Building Collage
            send(ProcessingState.BuildingCollage)
            val collage = collageRenderer.render(persons)

            Log.d(TAG, "Pipeline complete. Unique persons identified: ${persons.size}")

            send(ProcessingState.Complete(persons, collage))

        } catch (e: Exception) {
            Log.e(TAG, "Pipeline failed", e)
            send(ProcessingState.Error(e.message ?: "An unknown error occurred"))
        }
    }.flowOn(Dispatchers.Default)

    companion object {
        private const val TAG = "ProcessVideoUseCase"
    }
}
