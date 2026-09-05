package com.jhainusa.video_collage.domain.usecase

import android.net.Uri
import android.util.Log
import com.jhainusa.video_collage.core.clustering.AppearanceSegmenter
import com.jhainusa.video_collage.core.clustering.PersonClusterer
import com.jhainusa.video_collage.core.collage.CollageRenderer
import com.jhainusa.video_collage.core.embedding.FaceEmbedder
import com.jhainusa.video_collage.core.facedetection.FrameFaceDetector
import com.jhainusa.video_collage.core.quality.FaceQualityScorer
import com.jhainusa.video_collage.core.video.VideoFrameExtractor
import com.jhainusa.video_collage.domain.model.ProcessingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn

/**
 * Orchestrates the full video processing pipeline.
 */
class ProcessVideoUseCase(
    private val videoFrameExtractor: VideoFrameExtractor,
    private val faceDetector: FrameFaceDetector,
    private val faceEmbedder: FaceEmbedder,
    private val qualityScorer: FaceQualityScorer,
    private val appearanceSegmenter: AppearanceSegmenter,
    private val personClusterer: PersonClusterer,
    private val collageRenderer: CollageRenderer
) {

    /**
     * Executes the pipeline and emits the current [ProcessingState].
     * Uses channelFlow to allow emitting from callbacks.
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

            // 3. Quality Scoring & Embedding Generation
            send(ProcessingState.GeneratingEmbeddings(0f))
            val processedDetections = detections.mapIndexed { index, detection ->
                // Generate embedding first as per sequence suggestion
                val embedding = faceEmbedder.embed(detection.faceCrop)
                
                // Then score quality
                val quality = qualityScorer.score(detection, frameWidth, frameHeight)
                
                val updated = detection.copy(
                    embedding = embedding,
                    qualityScore = quality
                )
                
                trySend(ProcessingState.GeneratingEmbeddings((index + 1).toFloat() / detections.size))
                updated
            }

            // 4. Clustering Identities
            send(ProcessingState.ClusteringIdentities)
            val appearances = appearanceSegmenter.segment(processedDetections)
            val persons = personClusterer.cluster(appearances)

            if (persons.isEmpty()) {
                send(ProcessingState.Error("No distinct persons could be identified."))
                return@channelFlow
            }

            // 5. Building Collage
            send(ProcessingState.BuildingCollage)
            val collage = collageRenderer.render(persons)

            // Sanity check log
            Log.d(TAG, "Pipeline complete. Unique persons identified: ${persons.size}")
            persons.forEachIndexed { index, person ->
                Log.d(TAG, "Person #$index: ${person.appearanceCount} appearances, quality: ${person.representativeQualityScore}")
            }

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
