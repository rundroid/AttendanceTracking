package com.learning.attendancetracking.ui.compose

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

sealed interface AppNavRoutes {

    @Keep
    @Serializable
    data object Login : AppNavRoutes

    @Keep
    @Serializable
    data object StartEnrollment : AppNavRoutes

    @Keep
    @Serializable
    data object CapturePhoto : AppNavRoutes

    @Keep
    @Serializable
    data object Enrollment : AppNavRoutes

    @Keep
    @Serializable
    data class Home(private val userId: String) : AppNavRoutes

    @Keep
    @Serializable
    data class RecordAttendance(private val userId: String, val isLogin: Boolean) : AppNavRoutes

    @Keep
    @Serializable
    data object AppSettings : AppNavRoutes
}