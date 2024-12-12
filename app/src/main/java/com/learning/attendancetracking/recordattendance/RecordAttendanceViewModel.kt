package com.learning.attendancetracking.recordattendance

import android.graphics.BitmapFactory
import android.location.Location
import androidx.camera.core.ExperimentalGetImage
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learning.attendancetracking.database.repository.UserRepository
import com.learning.attendancetracking.detection.FaceDetectionHelper
import com.learning.attendancetracking.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@ExperimentalGetImage
@HiltViewModel
class RecordAttendanceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    @Named("io") private val ioDispatcher: CoroutineDispatcher,
    appPreferences: AppPreferences
) : ViewModel() {

    lateinit var faceDetectionHelper: FaceDetectionHelper

    private val geofenceRadius = appPreferences.geofenceRadius
    val isLogin = savedStateHandle.get<Boolean>("isLogin")
    private val _userIdFlow = savedStateHandle.getStateFlow<String?>("userId", null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val userEntityFlow = _userIdFlow.filterNotNull().flatMapLatest { id ->
        flow { emit(userRepository.getUser(id)) }
    }.flowOn(ioDispatcher).stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _userCurrentLocation = MutableStateFlow<Location?>(null)

    private val _inGeofenceFlow = combine(
        userEntityFlow.filterNotNull(),
        _userCurrentLocation.filterNotNull()
    ) { userEntity, currentLocation ->
        val distance = Location(currentLocation).apply {
            latitude = userEntity.latitude
            longitude = userEntity.longitude
        }.distanceTo(currentLocation)
        Timber.d("Current location distance: $distance")
        distance <= geofenceRadius
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    val inGeofenceFlow = _inGeofenceFlow.distinctUntilChanged { old, new -> old == new }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val userImageFlow =
        userEntityFlow.mapNotNull { it?.imagePath?.takeIf { p -> p.isNotBlank() } }
            .flatMapLatest { imagePath ->
                flow { emit(BitmapFactory.decodeFile(imagePath)) }
            }.flowOn(ioDispatcher).stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun updateCurrentLocation(location: Location) {
        _userCurrentLocation.update { location }
    }
}