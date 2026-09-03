package com.jhainusa.video_collage.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jhainusa.video_collage.domain.model.ProcessingState
import com.jhainusa.video_collage.domain.usecase.ProcessVideoUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for orchestrating the video processing UI state.
 * Launches the processing pipeline in a background thread and updates the UI state.
 */
class ProcessingViewModel(
    private val processVideoUseCase: ProcessVideoUseCase
) : ViewModel() {
    
    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    /**
     * Observable state of the video processing pipeline.
     */
    val processingState: StateFlow<ProcessingState> = _processingState

    /**
     * Starts the video processing pipeline for the given [uri].
     * The pipeline is executed on [Dispatchers.Default].
     */
    fun processVideo(uri: Uri) {
        viewModelScope.launch(Dispatchers.Default) {
            processVideoUseCase(uri).collect { state ->
                _processingState.value = state
            }
        }
    }
}
