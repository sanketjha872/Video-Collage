package com.jhainusa.video_collage.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jhainusa.video_collage.core.embedding.FaceEmbedder
import com.jhainusa.video_collage.core.facedetection.FrameFaceDetector
import com.jhainusa.video_collage.core.video.VideoFrameExtractor
import com.jhainusa.video_collage.domain.model.ProcessingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProcessingViewModel(
    private val videoFrameExtractor: VideoFrameExtractor,
    private val faceDetector: FrameFaceDetector,
    private val faceEmbedder: FaceEmbedder
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

                // 2. Face Detection
                _processingState.value = ProcessingState.DetectingFaces(0f)
                val allDetections = faceDetector.detectFaces(frames) { progress ->
                    _processingState.value = ProcessingState.DetectingFaces(progress)
                }

                if (allDetections.isEmpty()) {
                    _processingState.value = ProcessingState.Error("No faces detected in the video.")
                    return@launch
                }

                // 3. Embedding Generation
                _processingState.value = ProcessingState.GeneratingEmbeddings(0f)
                val detectionsWithEmbeddings = allDetections.mapIndexed { index, detection ->
                    val embedding = faceEmbedder.embed(detection.sourceFrame)
                    val updated = detection.copy(embedding = embedding)
                    _processingState.value = ProcessingState.GeneratingEmbeddings((index + 1).toFloat() / allDetections.size)
                    updated
                }

                // TODO: Next steps - clustering, etc.
                
            } catch (e: Exception) {
                _processingState.value = ProcessingState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}
