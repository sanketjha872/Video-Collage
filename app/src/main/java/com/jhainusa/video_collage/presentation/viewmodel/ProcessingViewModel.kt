package com.jhainusa.video_collage.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jhainusa.video_collage.core.video.VideoFrameExtractor
import com.jhainusa.video_collage.domain.model.ProcessingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProcessingViewModel(
    private val videoFrameExtractor: VideoFrameExtractor
) : ViewModel() {
    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val processingState: StateFlow<ProcessingState> = _processingState

    fun processVideo(uri: Uri) {
        viewModelScope.launch {
            try {
                _processingState.value = ProcessingState.ExtractingFrames(0f)
                
                val frames = videoFrameExtractor.extractFrames(uri) { progress ->
                    _processingState.value = ProcessingState.ExtractingFrames(progress)
                }

                if (frames.isEmpty()) {
                    _processingState.value = ProcessingState.Error("No frames could be extracted from the video.")
                    return@launch
                }

                // TODO: Next steps - Detect faces, generate embeddings, etc.
                // For now, we stop here as per the current implementation phase.
                
            } catch (e: Exception) {
                _processingState.value = ProcessingState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}
