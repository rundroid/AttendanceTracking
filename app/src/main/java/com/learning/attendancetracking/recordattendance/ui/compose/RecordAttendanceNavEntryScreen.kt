package com.learning.attendancetracking.recordattendance.ui.compose

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ExperimentalImageCaptureOutputFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.learning.attendancetracking.R
import com.learning.attendancetracking.camera.CameraPreviewViewModel
import com.learning.attendancetracking.home.HomeViewModel
import com.learning.attendancetracking.recordattendance.RecordAttendanceViewModel
import com.learning.attendancetracking.ui.compose.AppNavRoutes
import com.learning.attendancetracking.ui.compose.setTitleAndTopDestination

@ExperimentalGetImage
@OptIn(ExperimentalImageCaptureOutputFormat::class)
@Composable
fun RecordAttendanceNavEntryScreen(navController: NavHostController, navEntry: NavBackStackEntry) {
    val route = navEntry.toRoute<AppNavRoutes.RecordAttendance>()
    navEntry.setTitleAndTopDestination(
        if (route.isLogin) {
            stringResource(R.string.record_login_no_line)
        } else {
            stringResource(R.string.record_logout_no_line)
        }, false
    )
    navController.previousBackStackEntry?.let { parentEntry ->
        val viewModel = hiltViewModel<RecordAttendanceViewModel>()
        val homeViewModel = hiltViewModel<HomeViewModel>(parentEntry)
        val cameraPreviewViewModel = hiltViewModel<CameraPreviewViewModel>()
        viewModel.faceDetectionHelper = cameraPreviewViewModel.faceDetectionHelper
        navEntry.lifecycle.addObserver(cameraPreviewViewModel)
        RecordAttendanceRootScreen(viewModel, cameraPreviewViewModel, homeViewModel) {
            navController.popBackStack()
        }
    }
}