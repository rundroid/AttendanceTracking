package com.learning.attendancetracking.camera.ui.compose

import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalCameraInfo
import androidx.camera.core.ExperimentalImageCaptureOutputFormat
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureCapabilities
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learning.attendancetracking.camera.CameraPreviewViewModel
import com.learning.attendancetracking.extensions.getCameraProvider

@OptIn(ExperimentalImageCaptureOutputFormat::class, ExperimentalCameraInfo::class)
@Composable
fun CameraPreviewScreen(
    modifier: Modifier = Modifier,
    cameraPreviewViewModel: CameraPreviewViewModel,
    capturePicture: Boolean,
    captureCapabilitiesCallback: (ImageCaptureCapabilities) -> Unit
) {
    val currentCaptureCapabilitiesCallback = rememberUpdatedState(captureCapabilitiesCallback).value
    val context = LocalContext.current
    val previewView = remember {
        PreviewView(context)
    }
    val isBackCamera by cameraPreviewViewModel.isBackCamera.collectAsStateWithLifecycle()
    val lensFacing = if (isBackCamera) {
        CameraSelector.LENS_FACING_BACK
    } else {
        CameraSelector.LENS_FACING_FRONT
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lensFacing, capturePicture) {
        val cameraxSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        val useCases = with(cameraPreviewViewModel) {
            arrayListOf(previewUseCase, imageAnalysisUseCase).apply {
                if (capturePicture) {
                    add(getImageCaptureUseCase())
                }
            }
        }
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraxSelector,
            *useCases.toTypedArray()
        )
        cameraPreviewViewModel.previewUseCase.surfaceProvider = previewView.surfaceProvider
        val capabilities =
            ImageCapture.getImageCaptureCapabilities(cameraProvider.getCameraInfo(cameraxSelector))
        currentCaptureCapabilitiesCallback(capabilities)
    }
    AndroidView(factory = { previewView }, modifier = modifier.onSizeChanged { newSize ->
        cameraPreviewViewModel.updateCanvasSize(newSize)
    })
}