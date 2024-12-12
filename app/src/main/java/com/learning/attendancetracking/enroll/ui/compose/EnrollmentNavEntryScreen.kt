package com.learning.attendancetracking.enroll.ui.compose

import androidx.annotation.OptIn
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
import com.learning.attendancetracking.enroll.EnrollmentViewModel
import com.learning.attendancetracking.ui.compose.AppNavRoutes
import com.learning.attendancetracking.ui.compose.setTitleAndTopDestination

@OptIn(ExperimentalImageCaptureOutputFormat::class)
@Composable
fun EnrollmentNavEntryScreen(navController: NavHostController, navEntry: NavBackStackEntry) {
    navEntry.setTitleAndTopDestination(stringResource(R.string.enrollment), false)
    val startEntry =
        remember(navEntry) { navController.getBackStackEntry(AppNavRoutes.StartEnrollment) }
    val cameraViewModel = hiltViewModel<CameraPreviewViewModel>(startEntry)
    val sharedViewModel = hiltViewModel<CapturedImageSharedViewModel>(startEntry)
    val viewModel = hiltViewModel<EnrollmentViewModel>()
    viewModel.faceDetectionHelper = cameraViewModel.faceDetectionHelper
    EnrollmentScreen(
        viewModel = viewModel,
        sharedViewModel = sharedViewModel,
        onEnrolledCallback = { userId ->
            navController.navigate(route = AppNavRoutes.Home(userId)) {
                popUpTo(route = AppNavRoutes.Login) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        },
        cancelEnroll = {
            navController.popBackStack(route = AppNavRoutes.Login, inclusive = false)
        })
}