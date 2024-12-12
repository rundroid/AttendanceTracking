package com.learning.attendancetracking.ui.compose

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ExperimentalImageCaptureOutputFormat
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.learning.attendancetracking.R
import com.learning.attendancetracking.enroll.ui.compose.CapturePhotoNavEntryScreen
import com.learning.attendancetracking.enroll.ui.compose.EnrollmentNavEntryScreen
import com.learning.attendancetracking.home.ui.compose.HomeNavEntryScreen
import com.learning.attendancetracking.login.ui.compose.LoginNavEntryScreen
import com.learning.attendancetracking.recordattendance.ui.compose.RecordAttendanceNavEntryScreen
import com.learning.attendancetracking.settings.AppSettingsViewModel
import com.learning.attendancetracking.settings.ui.settings.ApplicationSettingsScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

const val KEY_CURRENT_SCREEN_TITLE = "current_title"
const val KEY_TOP_LEVEL_DESTINATION = "top_level_destination"

@Suppress("ProduceStateDoesNotAssignValue")
@Composable
fun navigationTitleState(navController: NavHostController): State<String> {
    return produceState(key1 = true, initialValue = "") {
        navController.currentBackStackEntryFlow.collectLatest { currentEntry ->
            currentEntry.savedStateHandle.getStateFlow<String?>(
                KEY_CURRENT_SCREEN_TITLE,
                initialValue = null
            ).filterNotNull().collectLatest {
                value = it
            }
        }
    }
}

@Suppress("ProduceStateDoesNotAssignValue")
@Composable
fun isTopLevelDestinationState(navController: NavHostController): State<Boolean> {
    return produceState(key1 = true, initialValue = false) {
        navController.currentBackStackEntryFlow.collectLatest { currentEntry ->
            currentEntry.savedStateHandle.getStateFlow<Boolean?>(
                KEY_TOP_LEVEL_DESTINATION,
                initialValue = null
            ).filterNotNull().collectLatest {
                value = it
            }
        }
    }
}

fun NavBackStackEntry.setTitleAndTopDestination(title: String, topDestination: Boolean) {
    savedStateHandle.apply {
        set(KEY_CURRENT_SCREEN_TITLE, title)
        set(KEY_TOP_LEVEL_DESTINATION, topDestination)
    }
}

@ExperimentalImageCaptureOutputFormat
@OptIn(ExperimentalGetImage::class)
@Composable
fun AppNavigation(modifier: Modifier = Modifier, navController: NavHostController) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = AppNavRoutes.Login
    ) {
        composable<AppNavRoutes.Login>(
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(250)
                )
            },
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(250)
                )
            }) { navEntry ->
            LoginNavEntryScreen(navController, navEntry)
        }
        navigation<AppNavRoutes.StartEnrollment>(startDestination = AppNavRoutes.CapturePhoto) {
            composable<AppNavRoutes.CapturePhoto>(enterTransition = {
                scaleIn(
                    animationSpec = tween(
                        250
                    )
                )
            }, exitTransition = {
                scaleOut(animationSpec = tween(250))
            }) { navEntry ->
                CapturePhotoNavEntryScreen(navController, navEntry)
            }
            composable<AppNavRoutes.Enrollment>(enterTransition = {
                scaleIn(
                    animationSpec = tween(
                        250
                    )
                )
            }, exitTransition = {
                scaleOut(animationSpec = tween(250))
            }) { navEntry ->
                EnrollmentNavEntryScreen(navController, navEntry)
            }
        }
        composable<AppNavRoutes.Home>(
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(250)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    tween(250)
                )
            }) { navEntry ->
            HomeNavEntryScreen(navController, navEntry)
        }
        composable<AppNavRoutes.RecordAttendance> { navEntry ->
            RecordAttendanceNavEntryScreen(navController, navEntry)
        }
        composable<AppNavRoutes.AppSettings>(enterTransition = {
            scaleIn(animationSpec = tween(250))
        }, exitTransition = {
            scaleOut(animationSpec = tween(250))
        }) { navEntry ->
            navEntry.setTitleAndTopDestination(stringResource(R.string.settings), false)
            val viewModel = hiltViewModel<AppSettingsViewModel>()
            navEntry.lifecycle.addObserver(viewModel)
            ApplicationSettingsScreen(viewModel)
        }
    }
}