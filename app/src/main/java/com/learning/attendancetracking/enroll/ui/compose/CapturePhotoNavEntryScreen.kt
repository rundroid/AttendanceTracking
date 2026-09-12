package com.learning.attendancetracking.enroll.ui.compose

import androidx.camera.core.ExperimentalImageCaptureOutputFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.learning.attendancetracking.R
import com.learning.attendancetracking.camera.CameraPreviewViewModel
import com.learning.attendancetracking.enroll.CapturedImageSharedViewModel
import com.learning.attendancetracking.ui.compose.AppNavRoutes
import com.learning.attendancetracking.ui.compose.setTitleAndTopDestination

@ExperimentalImageCaptureOutputFormat
@Composable
fun CapturePhotoNavEntryScreen(navController: NavHostController, navEntry: NavBackStackEntry) {
    navEntry.setTitleAndTopDestination(stringResource(R.string.take_photo), false)
    val startEntry =
        remember(navEntry) { navController.getBackStackEntry(AppNavRoutes.StartEnrollment) }
    val cameraViewModel = hiltViewModel<CameraPreviewViewModel>(startEntry)
    val sharedViewModel = hiltViewModel<CapturedImageSharedViewModel>(startEntry)
    navEntry.lifecycle.addObserver(cameraViewModel)
    CapturePhotoRootScreen(cameraViewModel, sharedViewModel, captureCallback = {
        navController.navigate(route = AppNavRoutes.Enrollment)
    })
}