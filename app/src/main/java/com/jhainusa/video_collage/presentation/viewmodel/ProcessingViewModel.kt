package com.jhainusa.video_collage.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jhainusa.video_collage.domain.model.ProcessingState
import com.jhainusa.video_collage.domain.usecase.ProcessVideoUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Job
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

    private var processingJob: Job? = null

    /**
     * Resets the processing state to Idle and cancels any ongoing processing.
     */
    fun reset() {
        processingJob?.cancel()
        _processingState.value = ProcessingState.Idle
    }

    /**
     * Starts the video processing pipeline for the given [uri].
     * If the pipeline is already running, it will be cancelled and restarted.
     * The pipeline is executed on [Dispatchers.Default].
     */
    fun processVideo(uri: Uri) {
        processingJob?.cancel()
        processingJob = viewModelScope.launch(Dispatchers.Default) {
            processVideoUseCase(uri).collect { state ->
                _processingState.value = state
            }
        }
    }

    override fun onCleared() {
        processingJob?.cancel()
    }
}
