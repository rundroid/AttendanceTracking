package com.learning.attendancetracking.detection

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.ui.unit.IntSize
import com.google.mlkit.vision.face.Face
import kotlin.math.ceil

fun Face.hasMaxOpenProbability(maxProbability: Float): Boolean {
    val left = leftEyeOpenProbability ?: return false
    val right = rightEyeOpenProbability ?: return false
    return left >= 0F && right >= 0F && left <= maxProbability && right <= maxProbability
}

fun Face.hasMinOpenProbability(minProbability: Float): Boolean {
    val left = leftEyeOpenProbability ?: return false
    val right = rightEyeOpenProbability ?: return false
    return left >= 0F && right >= 0F && left >= minProbability && right >= minProbability
}

fun calculateRect(
    context: Context,
    imageRect: Rect,
    boundingBox: Rect,
    overlay: IntSize,
    isFrontCamera: Boolean
): RectF {

    val width: Float = imageRect.width().toFloat()
    val height: Float = imageRect.height().toFloat()

    // for land scape
    fun isLandScapeMode(): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    fun whenLandScapeModeWidth(): Float {
        return when (isLandScapeMode()) {
            true -> width
            false -> height
        }
    }

    fun whenLandScapeModeHeight(): Float {
        return when (isLandScapeMode()) {
            true -> height
            false -> width
        }
    }

    val scaleX = overlay.width.toFloat() / whenLandScapeModeWidth()
    val scaleY = overlay.height.toFloat() / whenLandScapeModeHeight()
    val scale = scaleX.coerceAtLeast(scaleY)

    // Calculate offset (we need to center the overlay on the target)
    val offsetX = (overlay.width.toFloat() - ceil(whenLandScapeModeWidth() * scale)) / 2.0f
    val offsetY = (overlay.height.toFloat() - ceil(whenLandScapeModeHeight() * scale)) / 2.0f

    val mappedBox = RectF().apply {
        left = boundingBox.right * scale + offsetX
        top = boundingBox.top * scale + offsetY
        right = boundingBox.left * scale + offsetX
        bottom = boundingBox.bottom * scale + offsetY
    }

    // for front mode
    if (isFrontCamera) {
        val centerX = overlay.width.toFloat() / 2
        mappedBox.apply {
            left = centerX + (centerX - left)
            right = centerX - (right - centerX)
        }
    }
    return mappedBox
}