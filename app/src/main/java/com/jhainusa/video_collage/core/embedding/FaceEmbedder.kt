package com.jhainusa.video_collage.core.embedding

import android.graphics.Bitmap

/**
 * Interface for generating a numerical representation (embedding) of a face.
 * This embedding is used to compare faces and identify the same person across different appearances.
 */
interface FaceEmbedder {
    /**
     * Generates a face embedding for the given [faceBitmap].
     *
     * @param faceBitmap A bitmap containing the face to be embedded.
     * @return A [FloatArray] representing the face embedding.
     */
    fun embed(faceBitmap: Bitmap): FloatArray
}

class FaceEmbedderImpl : FaceEmbedder {
    override fun embed(faceBitmap: Bitmap): FloatArray {
        TODO("Not yet implemented")
    }
}
