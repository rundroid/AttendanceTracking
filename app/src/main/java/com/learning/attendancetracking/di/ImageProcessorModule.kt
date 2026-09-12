package com.learning.attendancetracking.di

import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.learning.attendancetracking.detection.FaceDetectionHelper
import com.learning.attendancetracking.utils.MIN_FACE_SIZE
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
class ImageProcessorModule {

    @Provides
    @ViewModelScoped
    fun providesFaceDetector(): FaceDetector {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .setMinFaceSize(MIN_FACE_SIZE)
            .build()
        return FaceDetection.getClient(options)
    }

    @Provides
    @ViewModelScoped
    fun providesFaceDetectionHelper(
        faceDetector: FaceDetector
    ): FaceDetectionHelper {
        return FaceDetectionHelper(faceDetector)
    }

    /*private fun getImageProcessor(cropSize: Int, imageRotationDegrees: Int): ImageProcessor {
        return ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(
                ResizeOp(
                    FACE_NET_INPUT_IMAGE_SIZE,
                    FACE_NET_INPUT_IMAGE_SIZE,
                    ResizeOp.ResizeMethod.NEAREST_NEIGHBOR
                )
            )
            .add(Rot90Op(-imageRotationDegrees / 90))
            .add(NormalizeOp(0f, 255f))
            .build()
    }*/
}