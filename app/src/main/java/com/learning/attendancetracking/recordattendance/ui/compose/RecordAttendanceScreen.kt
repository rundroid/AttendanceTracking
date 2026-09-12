package com.learning.attendancetracking.recordattendance.ui.compose

import android.Manifest
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ExperimentalImageCaptureOutputFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.learning.attendancetracking.R
import com.learning.attendancetracking.camera.CameraPreviewViewModel
import com.learning.attendancetracking.camera.ui.compose.CameraPreviewScreen
import com.learning.attendancetracking.extensions.yesOrNo
import com.learning.attendancetracking.home.HomeViewModel
import com.learning.attendancetracking.recordattendance.RecordAttendanceViewModel
import com.learning.attendancetracking.ui.compose.LocationUpdatesComposable
import com.learning.attendancetracking.utils.logImageCaptureCapabilities
import kotlinx.coroutines.flow.filterNotNull

@ExperimentalImageCaptureOutputFormat
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RecordAttendanceRootScreen(
    viewModel: RecordAttendanceViewModel,
    cameraPreviewViewModel: CameraPreviewViewModel,
    homeViewModel: HomeViewModel,
    onSuccess: () -> Unit
) {
    val permissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )
    if (permissionState.allPermissionsGranted) {
        RecordAttendanceScreen(viewModel, cameraPreviewViewModel, homeViewModel, onSuccess)
    } else {
        val requiredPermissions = permissionState.revokedPermissions.map { state ->
            if (state.permission == Manifest.permission.CAMERA) {
                "Camera"
            } else {
                "Location"
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val permissionMessage =
                "${requiredPermissions.joinToString(separator = " and")} permissions are required to record attendance"
            Text(permissionMessage)
            Spacer(modifier = Modifier.height(30.dp))
            Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                Text("Request permission")
            }
        }
    }
}

@ExperimentalImageCaptureOutputFormat
@ExperimentalGetImage
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordAttendanceScreen(
    viewModel: RecordAttendanceViewModel,
    cameraPreviewViewModel: CameraPreviewViewModel,
    homeViewModel: HomeViewModel,
    onSuccess: () -> Unit
) {
    LaunchedEffect(true) {
        cameraPreviewViewModel.initializeFaceComparator()
        viewModel.userImageFlow.filterNotNull().collect { bitmap ->
            cameraPreviewViewModel.setSourceBitmap(bitmap)
        }
    }
    DisposableEffect(true) {
        onDispose {
            cameraPreviewViewModel.clearFaceRectangles()
        }
    }
    LocationUpdatesComposable { location, _ ->
        viewModel.updateCurrentLocation(location)
    }
    viewModel.userEntityFlow.collectAsStateWithLifecycle()
    val liveFaceIdentified by cameraPreviewViewModel.liveFaceIdentifiedFlow.collectAsStateWithLifecycle()
    val faceMatched by cameraPreviewViewModel.faceMatchedFlow.collectAsStateWithLifecycle()
    val inGeofence by viewModel.inGeofenceFlow.collectAsStateWithLifecycle()
    val faceRectangles by cameraPreviewViewModel.faceRectangles.collectAsStateWithLifecycle()
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(color = Color(0x80000000)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Face matched: ${faceMatched.yesOrNo()}", color = Color.White)
            Text(text = "Face live: ${liveFaceIdentified.yesOrNo()}", color = Color.White)
            Text(text = "Office location: ${inGeofence.yesOrNo()}", color = Color.White)
            Spacer(modifier = Modifier.height(5.dp))
            Button(enabled = liveFaceIdentified && faceMatched && inGeofence, onClick = {
                if (viewModel.isLogin == true) {
                    homeViewModel.recordLogin()
                } else {
                    homeViewModel.recordLogout()
                }
                onSuccess.invoke()
            }) {
                Text(text = "Submit")
            }
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