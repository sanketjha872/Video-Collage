package com.jhainusa.video_collage.core.embedding

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import kotlin.math.sqrt

/**
 * Implementation of [FaceEmbedder] using a MobileFaceNet TFLite model.
 * Assumes a 112x112 RGB input and produces a 128-192 dimensional embedding.
 */
class MobileFaceNetEmbedder(context: Context) : FaceEmbedder {

    private val interpreter: Interpreter
    private val outputBuffer: Array<FloatArray>
    private val embeddingSize: Int

    init {
        val model = FileUtil.loadMappedFile(context, MODEL_PATH)
        val options = Interpreter.Options().apply {
            setNumThreads(NUM_THREADS)
        }
        interpreter = Interpreter(model, options)

        // Inspect output shape to determine embedding size
        val outputShape = interpreter.getOutputTensor(0).shape() // e.g., [1, 128]
        embeddingSize = outputShape[1]
        outputBuffer = Array(1) { FloatArray(embeddingSize) }
    }

    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
        .add(NormalizeOp(IMAGE_MEAN, IMAGE_STD))
        .build()

    override fun embed(faceBitmap: Bitmap): FloatArray {
        var tensorImage = TensorImage(interpreter.getInputTensor(0).dataType())
        tensorImage.load(faceBitmap)
        tensorImage = imageProcessor.process(tensorImage)

        interpreter.run(tensorImage.buffer, outputBuffer)

        return l2Normalize(outputBuffer[0].copyOf())
    }

    /**
     * Normalizes the vector to unit length (L2 norm = 1).
     */
    private fun l2Normalize(embedding: FloatArray): FloatArray {
        var sum = 0f
        for (v in embedding) {
            sum += v * v
        }
        val norm = sqrt(sum)
        
        if (norm > 1e-6) {
            for (i in embedding.indices) {
                embedding[i] /= norm
            }
        }
        return embedding
    }

    fun close() {
        interpreter.close()
    }

    companion object {
        private const val MODEL_PATH = "face_embedder.tflite"
        private const val NUM_THREADS = 4
        
        const val INPUT_SIZE = 112
        
        // Normalization: (pixel - 127.5) / 127.5 => scaled to [-1, 1]
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 127.5f
    }
}
