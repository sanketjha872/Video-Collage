package com.jhainusa.video_collage.di

import android.content.Context
import com.jhainusa.video_collage.core.embedding.FaceEmbedder
import com.jhainusa.video_collage.core.embedding.MobileFaceNetEmbedder
import com.jhainusa.video_collage.core.facedetection.FrameFaceDetector
import com.jhainusa.video_collage.core.facedetection.MlKitFrameFaceDetector
import com.jhainusa.video_collage.core.quality.FaceQualityScorer
import com.jhainusa.video_collage.core.video.MediaMetadataFrameExtractor
import com.jhainusa.video_collage.core.video.VideoFrameExtractor

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

    // TODO: Add other pipeline components as they are implemented
    // val faceDetector: FrameFaceDetector by lazy { ... }
    // val faceEmbedder: FaceEmbedder by lazy { ... }
    // val appearanceSegmenter: AppearanceSegmenter by lazy { ... }
    // val personClusterer: PersonClusterer by lazy { ... }
}
