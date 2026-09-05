package com.jhainusa.video_collage.di

import android.content.Context
import com.jhainusa.video_collage.core.collage.CollageRenderer
import com.jhainusa.video_collage.core.facedetection.FrameFaceDetector
import com.jhainusa.video_collage.core.facedetection.MlKitFrameFaceDetector
import com.jhainusa.video_collage.core.quality.FaceQualityScorer
import com.jhainusa.video_collage.core.video.MediaMetadataFrameExtractor
import com.jhainusa.video_collage.core.video.VideoFrameExtractor
import com.jhainusa.video_collage.domain.usecase.ProcessVideoUseCase

/**
 * Dependency provider for the application.
 * Manages the lifecycle of core components.
 */
class AppContainer(private val context: Context) {
    val videoFrameExtractor: VideoFrameExtractor by lazy {
        MediaMetadataFrameExtractor(context)
    }

    val faceDetector: FrameFaceDetector by lazy {
        MlKitFrameFaceDetector()
    }

    val faceQualityScorer: FaceQualityScorer by lazy {
        FaceQualityScorer()
    }

    val collageRenderer: CollageRenderer by lazy {
        CollageRenderer()
    }

    val processVideoUseCase: ProcessVideoUseCase by lazy {
        ProcessVideoUseCase(
            videoFrameExtractor,
            faceDetector,
            faceQualityScorer,
            collageRenderer
        )
    }
}
