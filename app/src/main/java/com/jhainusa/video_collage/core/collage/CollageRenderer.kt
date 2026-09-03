package com.jhainusa.video_collage.core.collage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.jhainusa.video_collage.domain.model.Person
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Renders a collage of the identified persons with their appearance counts.
 */
class CollageRenderer {

    /**
     * Renders a grid-based collage from the list of [Person] identities.
     * Each cell shows the representative shot and the number of appearances.
     */
    fun render(persons: List<Person>): Bitmap {
        if (persons.isEmpty()) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

        val numPersons = persons.size
        val columns = ceil(sqrt(numPersons.toDouble())).toInt()
        val rows = ceil(numPersons.toDouble() / columns).toInt()

        val cellSize = 300 // Target size for each face crop in the collage
        val padding = 20
        
        val width = columns * cellSize + (columns + 1) * padding
        val height = rows * cellSize + (rows + 1) * padding

        val collage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(collage)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 40f
            textAlign = Paint.Align.RIGHT
            setShadowLayer(5f, 0f, 0f, Color.BLACK)
        }

        val bgTextPaint = Paint().apply {
            color = Color.parseColor("#80000000") // Semi-transparent black
        }

        for (i in persons.indices) {
            val person = persons[i]
            val row = i / columns
            val col = i % columns

            val left = col * cellSize + (col + 1) * padding
            val top = row * cellSize + (row + 1) * padding
            val rect = Rect(left, top, left + cellSize, top + cellSize)

            // Draw the face (it's already a generous crop from sourceFrame, but we might want to center-crop here)
            drawCenterCrop(canvas, person.representativeShot, rect, paint)

            // Draw appearance count badge
            val label = "x${person.appearanceCount}"
            val textWidth = textPaint.measureText(label)
            val textHeight = textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent
            
            canvas.drawRect(
                rect.right - textWidth - 10f,
                rect.bottom - textHeight - 10f,
                rect.right.toFloat(),
                rect.bottom.toFloat(),
                bgTextPaint
            )
            
            canvas.drawText(
                label,
                rect.right - 5f,
                rect.bottom - 10f,
                textPaint
            )
        }

        return collage
    }

    private fun drawCenterCrop(canvas: Canvas, source: Bitmap, dest: Rect, paint: Paint) {
        val srcWidth = source.width
        val srcHeight = source.height
        
        val srcRect: Rect
        if (srcWidth > srcHeight) {
            val offset = (srcWidth - srcHeight) / 2
            srcRect = Rect(offset, 0, offset + srcHeight, srcHeight)
        } else {
            val offset = (srcHeight - srcWidth) / 2
            srcRect = Rect(0, offset, srcWidth, offset + srcWidth)
        }
        
        canvas.drawBitmap(source, srcRect, dest, paint)
    }
}
