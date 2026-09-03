package com.jhainusa.video_collage.core.clustering

import com.jhainusa.video_collage.domain.model.Appearance
import com.jhainusa.video_collage.domain.model.FaceDetection
import com.jhainusa.video_collage.domain.model.Person

/**
 * Interface for segmenting a list of raw face detections into continuous appearances.
 * An [Appearance] represents one continuous visible segment of a person (e.g., from frame 100 to 250).
 */
interface AppearanceSegmenter {
    /**
     * Groups [detections] into continuous [Appearance] segments.
     * Uses tracking IDs from the detector to maintain continuity within a segment.
     *
     * @param detections All face detections extracted from the video.
     * @return A list of segmented [Appearance] objects.
     */
    fun segment(detections: List<FaceDetection>): List<Appearance>
}

/**
 * Interface for clustering separate appearance segments into unique [Person] identities.
 * This identifies when the same person leaves the frame and returns later (multiple appearances).
 */
interface PersonClusterer {
    /**
     * Clusters the given [appearances] into unique identities.
     * Comparison is typically done using the [Appearance.representativeEmbedding].
     *
     * @param appearances All appearance segments detected in the video.
     * @return A list of [Person] objects, each aggregating one or more appearances.
     */
    fun cluster(appearances: List<Appearance>): List<Person>
}

class AppearanceSegmenterImpl : AppearanceSegmenter {
    override fun segment(detections: List<FaceDetection>): List<Appearance> {
        throw NotImplementedError("AppearanceSegmenterImpl.segment() not yet implemented")
    }
}

class PersonClustererImpl : PersonClusterer {
    override fun cluster(appearances: List<Appearance>): List<Person> {
        throw NotImplementedError("PersonClustererImpl.cluster() not yet implemented")
    }
}
