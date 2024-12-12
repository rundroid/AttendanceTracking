package com.learning.attendancetracking.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalImageCaptureOutputFormat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.learning.attendancetracking.ui.compose.AppNavigation
import com.learning.attendancetracking.ui.compose.AppTopBar
import com.learning.attendancetracking.ui.theme.AttendanceTrackingTheme
import dagger.hilt.android.AndroidEntryPoint

@ExperimentalMaterial3Api
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalImageCaptureOutputFormat::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AttendanceTrackingTheme {
                val navController = rememberNavController()
                val actionMenuList = remember { mutableStateListOf<ActionMenu>() }
                val menuHandler = remember {
                    object : ActionMenuHandler {
                        override fun addActionMenu(actionMenu: ActionMenu) {
                            actionMenuList.add(actionMenu)
                        }

                        override fun removeActionMenu(actionMenu: ActionMenu) {
                            actionMenuList.remove(actionMenu)
                        }
                    }
                }
                Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
                    AppTopBar(navController, actionMenuList)
                }) { innerPadding ->
                    CompositionLocalProvider(LocalActionMenuHandler provides menuHandler) {
                        AppNavigation(
                            navController = navController, modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}