package com.learning.attendancetracking.login.ui.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.learning.attendancetracking.R
import com.learning.attendancetracking.login.LoginViewModel
import com.learning.attendancetracking.ui.ActionMenu
import com.learning.attendancetracking.ui.LocalActionMenuHandler
import com.learning.attendancetracking.ui.compose.AppNavRoutes
import com.learning.attendancetracking.ui.compose.setTitleAndTopDestination

@Composable
fun LoginNavEntryScreen(navController: NavHostController, navEntry: NavBackStackEntry) {
    navEntry.setTitleAndTopDestination("", true)
    val currentRoute = navEntry.toRoute<AppNavRoutes.Login>()
    LoginMenuHandler(navController)
    LoginScreen(
        viewModel = hiltViewModel<LoginViewModel>(),
        onLoginSuccess = { userId ->
            navController.navigate(route = AppNavRoutes.Home(userId)) {
                popUpTo(route = currentRoute) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        },
        onEnrollClick = {
            navController.navigate(route = AppNavRoutes.StartEnrollment)
        })
}

@Composable
fun LoginMenuHandler(navController: NavHostController) {
    val settingsLabel = stringResource(R.string.settings)
    val lifecycleOwner = LocalLifecycleOwner.current
    LocalActionMenuHandler.current?.let { menuHandler ->
        DisposableEffect(lifecycleOwner, menuHandler) {
            val actionMenu = ActionMenu(Icons.Default.Settings, settingsLabel) {
                navController.navigate(route = AppNavRoutes.AppSettings)
            }
            val observer = object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    super.onResume(owner)
                    menuHandler.addActionMenu(actionMenu)
                }

                override fun onStop(owner: LifecycleOwner) {
                    super.onPause(owner)
                    menuHandler.removeActionMenu(actionMenu)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                menuHandler.removeActionMenu(actionMenu)
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }
}