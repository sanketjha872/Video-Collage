package com.jhainusa.video_collage.core.collage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.jhainusa.video_collage.domain.model.Person
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Renders a stylized, shareable collage (Instagram Story aspect ratio) 
 * showing unique persons identified in the video.
 */
class CollageRenderer {

    companion object {
        private const val CANVAS_WIDTH = 1080
        private const val CANVAS_HEIGHT = 1920
        private const val CORNER_RADIUS = 48f
        private const val TILE_MARGIN = 24f
        private const val PADDING_EXTERNAL = 60f
    }

    fun render(persons: List<Person>): Bitmap {
        if (persons.isEmpty()) {
            return createEmptyBitmap()
        }

        val bitmap = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBackground(canvas)
        drawTitle(canvas)

        val (cols, rows) = getGridDimensions(persons.size)
        
        // Calculate drawing area (excluding title space and external padding)
        val topOffset = 250f
        val bottomOffset = 100f
        val drawWidth = CANVAS_WIDTH - (PADDING_EXTERNAL * 2)
        val drawHeight = CANVAS_HEIGHT - topOffset - bottomOffset
        
        val tileWidth = (drawWidth - (TILE_MARGIN * (cols - 1))) / cols
        val tileHeight = (drawHeight - (TILE_MARGIN * (rows - 1))) / rows

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        persons.forEachIndexed { index, person ->
            val col = index % cols
            val row = index / cols
            
            val left = PADDING_EXTERNAL + col * (tileWidth + TILE_MARGIN)
            val top = topOffset + row * (tileHeight + TILE_MARGIN)
            val tileRect = RectF(left, top, left + tileWidth, top + tileHeight)

            drawPersonTile(canvas, person, tileRect, paint)
        }

        return bitmap
    }

    private fun getGridDimensions(count: Int): Pair<Int, Int> {
        return when {
            count <= 1 -> 1 to 1
            count <= 2 -> 1 to 2
            count <= 4 -> 2 to 2
            count <= 6 -> 2 to 3
            count <= 9 -> 3 to 3
            count <= 12 -> 3 to 4
            else -> 4 to ceil(count / 4f).toInt()
        }
    }

    private fun drawBackground(canvas: Canvas) {
        val paint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, CANVAS_HEIGHT.toFloat(),
                Color.parseColor("#1A1A2E"), // Deep navy
                Color.parseColor("#16213E"), // Slightly lighter navy
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(), paint)
        
        // Subtle accent circle
        paint.shader = null
        paint.color = Color.parseColor("#0F3460")
        canvas.drawCircle(CANVAS_WIDTH.toFloat(), 0f, 600f, paint)
    }

    private fun drawTitle(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Video Face Collage", CANVAS_WIDTH / 2f, 120f, paint)
        
        paint.textSize = 36f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#A0A0A0")
        canvas.drawText("Identified Personalities", CANVAS_WIDTH / 2f, 180f, paint)
    }

    private fun drawPersonTile(canvas: Canvas, person: Person, rect: RectF, paint: Paint) {
        canvas.save()
        
        // 1. Clip Rounded Corners
        val path = Path().apply {
            addRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, Path.Direction.CW)
        }
        canvas.clipPath(path)

        // 2. Draw Image (Center Crop)
        val source = person.representativeShot
        val srcRect = calculateCenterCrop(source.width, source.height, rect.width(), rect.height())
        canvas.drawBitmap(source, srcRect, rect, paint)

        // 3. Draw Scrim for text legibility
        val scrimHeight = rect.height() * 0.25f
        val scrimPaint = Paint().apply {
            shader = LinearGradient(
                0f, rect.bottom - scrimHeight, 0f, rect.bottom,
                Color.TRANSPARENT, Color.parseColor("#CC000000"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(rect.left, rect.bottom - scrimHeight, rect.right, rect.bottom, scrimPaint)

        // 4. Draw Badge/Label
        val label = "${person.appearanceCount} appearances"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = max(24f, rect.width() * 0.08f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        val textWidth = textPaint.measureText(label)
        val textHeight = textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent
        
        // Pill background
        val pillPaddingH = 20f
        val pillPaddingV = 10f
        val pillRect = RectF(
            rect.left + 20f,
            rect.bottom - textHeight - pillPaddingV * 2 - 20f,
            rect.left + 20f + textWidth + pillPaddingH * 2,
            rect.bottom - 20f
        )
        
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E94560") // Accent pink/red
        }
        canvas.drawRoundRect(pillRect, 20f, 20f, pillPaint)
        
        canvas.drawText(
            label,
            pillRect.left + pillPaddingH,
            pillRect.bottom - pillPaddingV - textPaint.fontMetrics.descent,
            textPaint
        )

        canvas.restore()

        // 5. Subtle Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#33FFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, borderPaint)
    }

    private fun calculateCenterCrop(srcW: Int, srcH: Int, dstW: Float, dstH: Float): Rect {
        val srcRatio = srcW.toFloat() / srcH
        val dstRatio = dstW / dstH
        
        return if (srcRatio > dstRatio) {
            val width = (srcH * dstRatio).toInt()
            val offset = (srcW - width) / 2
            Rect(offset, 0, offset + width, srcH)
        } else {
            val height = (srcW / dstRatio).toInt()
            val offset = (srcH - height) / 2
            Rect(0, offset, srcW, offset + height)
        }
    }

    private fun createEmptyBitmap(): Bitmap {
        val b = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        Canvas(b).drawColor(Color.DKGRAY)
        return b
    }
}
