package com.jhainusa.video_collage.core.clustering

import com.jhainusa.video_collage.domain.model.Appearance
import com.jhainusa.video_collage.domain.model.Person
import java.util.UUID

/**
 * Interface for clustering separate appearances into unique person identities.
 */
interface PersonClusterer {
    fun cluster(appearances: List<Appearance>): List<Person>
}

/**
 * Clusters appearances using Hierarchical Agglomerative Clustering (HAC) with average-linkage
 * based on cosine similarity of face embeddings.
 */
class CosineHacPersonClusterer : PersonClusterer {

    override fun cluster(appearances: List<Appearance>): List<Person> {
        if (appearances.isEmpty()) return emptyList()

        // Start with each appearance in its own cluster
        val clusters = appearances.map { mutableListOf(it) }.toMutableList()

        while (clusters.size > 1) {
            var bestSimilarity = -1f
            var bestPair: Pair<Int, Int>? = null

            // Find the pair of clusters with the highest average similarity
            for (i in 0 until clusters.size) {
                for (j in i + 1 until clusters.size) {
                    val similarity = calculateAverageSimilarity(clusters[i], clusters[j])
                    if (similarity > bestSimilarity && similarity >= SIMILARITY_THRESHOLD) {
                        bestSimilarity = similarity
                        bestPair = Pair(i, j)
                    }
                }
            }

            if (bestPair != null) {
                // Merge clusters[bestPair.second] into clusters[bestPair.first]
                val clusterToMerge = clusters.removeAt(bestPair.second)
                clusters[bestPair.first].addAll(clusterToMerge)
            } else {
                // No more pairs exceed the threshold
                break
            }
        }

        return clusters.map { clusterAppearances ->
            val bestDetectionAcrossAll = clusterAppearances
                .flatMap { it.detections }
                .maxBy { it.qualityScore ?: 0f }

            Person(
                id = UUID.randomUUID().toString(),
                appearances = clusterAppearances.sortedBy { it.startMs },
                appearanceCount = clusterAppearances.size,
                representativeShot = bestDetectionAcrossAll.sourceFrame,
                representativeQualityScore = bestDetectionAcrossAll.qualityScore ?: 0f
            )
        }
    }

    /**
     * Calculates the average pairwise cosine similarity between all embeddings in two clusters.
     */
    private fun calculateAverageSimilarity(cluster1: List<Appearance>, cluster2: List<Appearance>): Float {
        var totalSimilarity = 0f
        var count = 0
        
        for (a1 in cluster1) {
            for (a2 in cluster2) {
                totalSimilarity += cosineSimilarity(a1.representativeEmbedding, a2.representativeEmbedding)
                count++
            }
        }
        
        return if (count > 0) totalSimilarity / count else 0f
    }

    /**
     * Calculates the cosine similarity between two embeddings.
     * Assumes embeddings are already L2-normalized (dot product == cosine similarity).
     */
    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        if (vec1.size != vec2.size || vec1.isEmpty()) return 0f
        var dotProduct = 0f
        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
        }
        return dotProduct
    }

    companion object {
        /**
         * The minimum average cosine similarity to consider two clusters as the same person.
         * NOTE: This threshold value should be documented in the project README along with 
         * the specific embedding model used (MobileFaceNet).
         */
        const val SIMILARITY_THRESHOLD = 0.6f
    }
}
