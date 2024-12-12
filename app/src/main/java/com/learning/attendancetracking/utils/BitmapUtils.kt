package com.learning.attendancetracking.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect

fun Bitmap.rotate(rotation: Float, setupMatrix: (Matrix.() -> Unit)? = null): Bitmap {
    return Bitmap.createBitmap(
        this,
        0,
        0,
        width,
        height,
        Matrix().apply {
            postRotate(rotation)
            setupMatrix?.invoke(this)
        },
        true
    )
}

fun Bitmap.safeRecycle() {
    if (isRecycled.not()) {
        recycle()
    }
}

fun extractFaceAsBitmap(originalBitmap: Bitmap, faceBoundingBox: Rect, rotation: Int): Bitmap? {
    var image = originalBitmap
    if (rotation != 0) {
        image = image.rotate(rotation.toFloat())
    }
    return if (faceBoundingBox.top >= 0
        && faceBoundingBox.bottom <= image.height
        && faceBoundingBox.top + faceBoundingBox.height() <= image.height
        && faceBoundingBox.left >= 0
        && faceBoundingBox.left + faceBoundingBox.width() <= image.width
    ) {
        Bitmap.createBitmap(
            image,
            faceBoundingBox.left,
            faceBoundingBox.top,
            faceBoundingBox.width(),
            faceBoundingBox.height()
        )
    } else null
}