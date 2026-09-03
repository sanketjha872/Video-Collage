package com.jhainusa.video_collage.di

import android.content.Context
import com.jhainusa.video_collage.core.clustering.AppearanceSegmenter
import com.jhainusa.video_collage.core.clustering.CosineHacPersonClusterer
import com.jhainusa.video_collage.core.clustering.PersonClusterer
import com.jhainusa.video_collage.core.clustering.TrackingBasedAppearanceSegmenter
import com.jhainusa.video_collage.core.collage.CollageRenderer
import com.jhainusa.video_collage.core.embedding.FaceEmbedder
import com.jhainusa.video_collage.core.embedding.MobileFaceNetEmbedder
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

    val faceEmbedder: FaceEmbedder by lazy {
        MobileFaceNetEmbedder(context)
    }

    val faceQualityScorer: FaceQualityScorer by lazy {
        FaceQualityScorer()
    }

    val appearanceSegmenter: AppearanceSegmenter by lazy {
        TrackingBasedAppearanceSegmenter()
    }

    val personClusterer: PersonClusterer by lazy {
        CosineHacPersonClusterer()
    }

    val collageRenderer: CollageRenderer by lazy {
        CollageRenderer()
    }

    val processVideoUseCase: ProcessVideoUseCase by lazy {
        ProcessVideoUseCase(
            videoFrameExtractor,
            faceDetector,
            faceEmbedder,
            faceQualityScorer,
            appearanceSegmenter,
            personClusterer,
            collageRenderer
        )
    }
}
