package com.learning.attendancetracking.home.ui.compose

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.learning.attendancetracking.home.HomeViewModel
import com.learning.attendancetracking.ui.compose.AppNavRoutes
import com.learning.attendancetracking.ui.compose.setTitleAndTopDestination

@Composable
fun HomeNavEntryScreen(navController: NavHostController, navEntry: NavBackStackEntry) {
    navEntry.setTitleAndTopDestination("", true)
    val route = navEntry.toRoute<AppNavRoutes.Home>()
    val viewModel = hiltViewModel<HomeViewModel>()
    HomeScreen(viewModel, loginUpdateRequest = { id, isLogin ->
        navController.navigate(
            route = AppNavRoutes.RecordAttendance(
                userId = id,
                isLogin = isLogin
            )
        )
    }, logout = {
        navController.navigate(route = AppNavRoutes.Login) {
            popUpTo(route = route) {
                inclusive = true
            }
            launchSingleTop = true
        }
    })
}