package com.learning.attendancetracking.enroll.ui.compose

import android.Manifest
import androidx.camera.core.ExperimentalImageCaptureOutputFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.learning.attendancetracking.R
import com.learning.attendancetracking.camera.CameraPreviewViewModel
import com.learning.attendancetracking.camera.ui.compose.CameraPreviewScreen
import com.learning.attendancetracking.enroll.CapturedImageSharedViewModel
import com.learning.attendancetracking.utils.logImageCaptureCapabilities

@androidx.annotation.OptIn(ExperimentalImageCaptureOutputFormat::class)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CapturePhotoRootScreen(
    cameraPreviewViewModel: CameraPreviewViewModel,
    sharedViewModel: CapturedImageSharedViewModel,
    captureCallback: () -> Unit
) {
    val permissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    AnimatedContent(permissionState.status, label = "capture_photo") { status ->
        when (status) {
            is PermissionStatus.Granted -> {
                CapturePhotoScreen(cameraPreviewViewModel, sharedViewModel, captureCallback)
            }

            is PermissionStatus.Denied -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val permissionMessage = if (status.shouldShowRationale) {
                        "The camera is important for this app. Please grant the permission."
                    } else {
                        "Camera permission required for this feature to be available. Please grant the permission"
                    }
                    Text(permissionMessage)
                    Spacer(modifier = Modifier.height(30.dp))
                    Button(onClick = { permissionState.launchPermissionRequest() }) {
                        Text("Request permission")
                    }
                }
            }
        }
    }

}

@ExperimentalImageCaptureOutputFormat
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapturePhotoScreen(
    cameraPreviewViewModel: CameraPreviewViewModel,
    sharedViewModel: CapturedImageSharedViewModel,
    captureCallback: () -> Unit
) {
    val currentCaptureCallback = rememberUpdatedState(captureCallback).value
    val faceRectangles by cameraPreviewViewModel.faceRectangles.collectAsStateWithLifecycle()
    var captureClicked by remember { mutableStateOf(false) }
    val liveFaceIdentified by cameraPreviewViewModel.liveFaceIdentifiedFlow.collectAsStateWithLifecycle()
    DisposableEffect(true) {
        with(cameraPreviewViewModel) {
            captureImageWhenLiveRef.set(false)
            capturedImageListener = { bitmapState ->
                captureClicked = false
                bitmapState.success()?.let { bitmap ->
                    sharedViewModel.updateImage(bitmap)
                    currentCaptureCallback()
                }
            }
            onDispose {
                clearFaceRectangles()
                capturedImageListener = null
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreviewScreen(
            modifier = Modifier.fillMaxSize(),
            cameraPreviewViewModel,
            false
        ) { capabilities ->
            logImageCaptureCapabilities(capabilities)
        }
        Canvas(modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                cameraPreviewViewModel.canvasSize = it
            }) {
            arrayListOf<Rect>().apply {
                addAll(faceRectangles)
            }.forEach { rect ->
                drawRoundRect(
                    color = Color.Green,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
        Button(
            onClick = {
                captureClicked = true
                cameraPreviewViewModel.captureImageWhenLiveRef.set(true)
            },
            enabled = captureClicked.not() && liveFaceIdentified,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        ) {
            Text(stringResource(R.string.take_photo))
        }
        IconButton(
            onClick = {
                cameraPreviewViewModel.switchCamera()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 10.dp, end = 10.dp)
                .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_switch_camera_24),
                tint = null,
                contentDescription = "Switch camera"
            )
        }
    }
}