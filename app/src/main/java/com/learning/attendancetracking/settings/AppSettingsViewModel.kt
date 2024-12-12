package com.learning.attendancetracking.settings

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.learning.attendancetracking.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(private val appPreferences: AppPreferences) :
    ViewModel(), DefaultLifecycleObserver {

    private val _radiusFlow = MutableStateFlow(appPreferences.geofenceRadius.toString())
    val radiusFlow = _radiusFlow.asStateFlow()

    var useFaceNet: Boolean
        set(value) {
            appPreferences.useFaceNet = value
        }
        get() = appPreferences.useFaceNet

    override fun onCreate(owner: LifecycleOwner) {
        super.onStart(owner)
        _radiusFlow.update { appPreferences.geofenceRadius.toString() }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        _radiusFlow.value.toIntOrNull()?.let {
            appPreferences.geofenceRadius = it
        }
    }

    fun updateRadius(radius: String) {
        _radiusFlow.update { radius }
    }
}