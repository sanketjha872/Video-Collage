package com.jhainusa.video_collage.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jhainusa.video_collage.core.embedding.FaceEmbedder
import com.jhainusa.video_collage.core.facedetection.FrameFaceDetector
import com.jhainusa.video_collage.core.quality.FaceQualityScorer
import com.jhainusa.video_collage.core.video.VideoFrameExtractor
import com.jhainusa.video_collage.domain.model.ProcessingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProcessingViewModel(
    private val videoFrameExtractor: VideoFrameExtractor,
    private val faceDetector: FrameFaceDetector,
    private val faceEmbedder: FaceEmbedder,
    private val qualityScorer: FaceQualityScorer
) : ViewModel() {
    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState

    fun processVideo(uri: Uri) {
        viewModelScope.launch {
            try {
                // 1. Frame Extraction
                _processingState.value = ProcessingState.ExtractingFrames(0f)
                val frames = videoFrameExtractor.extractFrames(uri) { progress ->
                    _processingState.value = ProcessingState.ExtractingFrames(progress)
                }

                if (frames.isEmpty()) {
                    _processingState.value = ProcessingState.Error("No frames could be extracted from the video.")
                    return@launch
                }

                val frameWidth = frames[0].second.width
                val frameHeight = frames[0].second.height

                // 2. Face Detection
                _processingState.value = ProcessingState.DetectingFaces(0f)
                val detections = faceDetector.detectFaces(frames) { progress ->
                    _processingState.value = ProcessingState.DetectingFaces(progress)
                }

                if (detections.isEmpty()) {
                    _processingState.value = ProcessingState.Error("No faces detected in the video.")
                    return@launch
                }

                // 3. Quality Scoring & Embedding Generation
                _processingState.value = ProcessingState.GeneratingEmbeddings(0f)
                val processedDetections = detections.mapIndexed { index, detection ->
                    // Score quality
                    val quality = qualityScorer.score(detection, frameWidth, frameHeight)
                    
                    // Generate embedding
                    val embedding = faceEmbedder.embed(detection.sourceFrame)
                    
                    val updated = detection.copy(
                        qualityScore = quality,
                        embedding = embedding
                    )
                    
                    _processingState.value = ProcessingState.GeneratingEmbeddings((index + 1).toFloat() / detections.size)
                    updated
                }

                // TODO: 4. Clustering Identities
                _processingState.value = ProcessingState.ClusteringIdentities
                
                // TODO: 5. Building Collage
                _processingState.value = ProcessingState.BuildingCollage

            } catch (e: Exception) {
                _processingState.value = ProcessingState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}
