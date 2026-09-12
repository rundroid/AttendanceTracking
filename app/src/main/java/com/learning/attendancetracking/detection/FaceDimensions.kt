package com.learning.attendancetracking.detection

import android.graphics.Rect
import com.google.mlkit.vision.face.Face

enum class FaceValidState {
    VALID,
    NO_FACE,
    MULTIPLE_FACES,
    NOT_LIVE
}

fun FaceValidState.message(): String {
    return when (this) {
        FaceValidState.VALID -> "Valid"
        FaceValidState.NO_FACE -> "No face identified"
        FaceValidState.NOT_LIVE -> "Not live face"
        FaceValidState.MULTIPLE_FACES -> "Multiple faces found"
    }
}

data class FaceDimensions(
    val x: Float,
    val y: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

fun Face.faceDimensions(): FaceDimensions {
    val x = boundingBox.centerX().toFloat()
    val y = boundingBox.centerY().toFloat()
    return FaceDimensions(
        x = x,
        y = y,
        left = x - boundingBox.width() / 2.0f,
        top = y - boundingBox.height() / 2.0f,
        right = x + boundingBox.width() / 2.0f,
        bottom = y + boundingBox.height() / 2.0f,
    )
}

fun checkIfTooFar(
    imageRect: Rect,
    faceDimensions: FaceDimensions
): Boolean {
    val screenPercentage = 0.4f
    val width = imageRect.width()
    val height = imageRect.height()
    return (faceDimensions.bottom - faceDimensions.top) <= (height * screenPercentage) ||
            (faceDimensions.right - faceDimensions.left) <= (width * screenPercentage)

}

fun checkIfNotCentered(
    imageRect: Rect,
    faceDimensions: FaceDimensions
): Boolean {
    val width = imageRect.width()
    val height = imageRect.height()

    return faceDimensions.left < 0 || faceDimensions.right > width || faceDimensions.top < 0 || faceDimensions.bottom > height
}