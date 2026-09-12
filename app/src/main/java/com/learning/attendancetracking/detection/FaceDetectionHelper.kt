package com.learning.attendancetracking.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.compose.ui.unit.IntSize
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetector
import com.learning.attendancetracking.utils.extractFaceAsBitmap
import com.learning.attendancetracking.utils.orElse
import com.learning.attendancetracking.utils.safeRecycle
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.tensorflow.lite.support.image.TensorImage
import timber.log.Timber
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.sqrt
import androidx.compose.ui.geometry.Rect as ComposeRect

class FaceDetectionHelper(
    private val faceDetector: FaceDetector
) {

    private val faceDetectionExecutor = Executors.newFixedThreadPool(3)
    val faceNetInterpreter = FaceNetInterpreter()
    val initializationState = faceNetInterpreter.initializationState

    fun initializeInterpreter(context: Context, assetFileName: String) {
        faceDetectionExecutor.submit {
            runBlocking {
                faceNetInterpreter.initializeInterpreter(context, assetFileName)
            }
        }
    }

    suspend fun readFacesData(
        bitmap: Bitmap,
        recycle: Boolean = true
    ): Map<Face, FloatArray> {
        return suspendCancellableCoroutine { continuation ->
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            faceDetector.process(inputImage).addOnCompleteListener(faceDetectionExecutor) { task ->
                if (task.isSuccessful && task.result.isNotEmpty()) {
                    val resultMap = mutableMapOf<Face, FloatArray>()
                    task.result.forEach { face ->
                        readFaceDataFromBitmap(
                            bitmap,
                            face.boundingBox,
                            inputImage.rotationDegrees,
                            false
                        )?.let {
                            resultMap[face] = it
                        }
                    }
                    continuation.resume(resultMap)
                } else {
                    continuation.resumeWithException(task.exception.orElse { Exception("No faces detected") })
                }
                if (recycle) {
                    bitmap.safeRecycle()
                }
            }
        }
    }

    @JvmOverloads
    fun readFaceDataFromBitmap(
        sourceBitmap: Bitmap,
        boundingBox: Rect,
        rotationDegrees: Int,
        recycle: Boolean = true
    ): FloatArray? {
        var floatArray: FloatArray? = null
        val faceBitmap = extractFaceAsBitmap(
            sourceBitmap,
            boundingBox,
            rotationDegrees
        ) ?: return null
        val tensorImage = TensorImage.fromBitmap(faceBitmap)
        val byteBuffer = faceNetInterpreter.imageProcessor.process(tensorImage).buffer
        val faceOutputArray = Array(1) { FloatArray(faceNetInterpreter.outputSize) }
        val interpreter = faceNetInterpreter.interpreter ?: return null
        runCatching {
            interpreter.run(byteBuffer, faceOutputArray)
        }.onSuccess {
            floatArray = faceOutputArray[0]
        }
        if (faceBitmap != sourceBitmap) {
            faceBitmap.safeRecycle()
        }
        if (recycle) {
            sourceBitmap.safeRecycle()
        }
        return floatArray
    }

    fun compareFace(
        source: FloatArray,
        imageProxy: ImageProxy,
        faceBoundingBox: Rect,
        recycle: Boolean
    ): Float? {
        val bitmap = imageProxy.toBitmap()
        return readFaceDataFromBitmap(
            bitmap,
            faceBoundingBox,
            imageProxy.imageInfo.rotationDegrees,
            recycle
        )?.let { faceData ->
            findDistance(source, faceData)
        }
    }

    fun findDistance(knownEmbedding: FloatArray, embedding: FloatArray): Float {
        var distance = 0f
        for (i in embedding.indices) {
            val diff = embedding[i] - knownEmbedding[i]
            distance += diff * diff
        }
        return sqrt(distance.toDouble()).toFloat()
    }

    suspend fun findFacesInBitmap(bitmap: Bitmap, rotationDegrees: Int): List<Face> {
        return suspendCancellableCoroutine { continuation ->
            faceDetector.process(bitmap, rotationDegrees)
                .addOnCompleteListener(faceDetectionExecutor) { task ->
                    if (task.isSuccessful) {
                        continuation.resume(task.result)
                    } else {
                        continuation.resume(emptyList())
                    }
                }
        }
    }

    @OptIn(ExperimentalGetImage::class)
    suspend fun findFacesInImageProxy(
        context: Context,
        imageProxy: ImageProxy,
        canvasSize: IntSize,
        isFrontCamera: Boolean
    ): Pair<List<Face>, List<ComposeRect>> {
        return suspendCancellableCoroutine { continuation ->
            val image = imageProxy.image ?: run {
                continuation.resumeWithException(Exception("No image found"))
                return@suspendCancellableCoroutine
            }
            faceDetector.process(
                InputImage.fromMediaImage(
                    image, imageProxy.imageInfo.rotationDegrees
                )
            ).addOnCompleteListener(faceDetectionExecutor) { task ->
                if (task.isSuccessful.not()) {
                    val exception = task.exception.orElse { Exception("No image found") }
                    Timber.d(exception, "Error finding faces")
                    continuation.resumeWithException(exception)
                    return@addOnCompleteListener
                }
                val faces = task.result
                val faceRectangles = faces.map { face ->
                    calculateRect(
                        context,
                        imageProxy.cropRect,
                        face.boundingBox,
                        canvasSize,
                        isFrontCamera
                    ).let { fRect ->
                        ComposeRect(
                            left = fRect.left,
                            top = fRect.top,
                            right = fRect.right,
                            bottom = fRect.bottom
                        )
                    }
                }
                continuation.resume(faces to faceRectangles)
            }
        }
    }

    fun releaseInterpreter() {
        faceNetInterpreter.releaseInterpreter()
    }

    fun dispose() {
        releaseInterpreter()
        faceDetector.close()
        faceDetectionExecutor.shutdown()
    }
}