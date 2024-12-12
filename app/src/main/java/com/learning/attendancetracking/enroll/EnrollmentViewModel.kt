package com.learning.attendancetracking.enroll

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import androidx.lifecycle.AtomicReference
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learning.attendancetracking.database.entity.UserEntity
import com.learning.attendancetracking.database.repository.UserRepository
import com.learning.attendancetracking.detection.FaceDetectionHelper
import com.learning.attendancetracking.detection.FaceValidState
import com.learning.attendancetracking.detection.hasMinOpenProbability
import com.learning.attendancetracking.detection.message
import com.learning.attendancetracking.prefs.AppPreferences
import com.learning.attendancetracking.utils.DataState
import com.learning.attendancetracking.utils.FACE_MATCH_COMPARISON_CONFIDENCE
import com.learning.attendancetracking.utils.LIVE_FACE_MIN_PROBABILITY
import com.learning.attendancetracking.utils.getModelFileName
import com.learning.attendancetracking.utils.orElse
import com.learning.attendancetracking.utils.safeRecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class EnrollmentViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    @Named("io") private val ioDispatcher: CoroutineDispatcher,
    @Named("default") private val defaultDispatcher: CoroutineDispatcher,
    private val appPreferences: AppPreferences
) : ViewModel() {

    lateinit var faceDetectionHelper: FaceDetectionHelper

    private val _idFlow = MutableStateFlow("")
    val idFlow = _idFlow.asStateFlow()

    private val _nameFlow = MutableStateFlow("")
    val nameFlow = _nameFlow.asStateFlow()

    private val _departmentFlow = MutableStateFlow("")
    val departmentFlow = _departmentFlow.asStateFlow()

    private val _conditionsAcceptedFlow = MutableStateFlow(false)
    val conditionsAcceptedFlow = _conditionsAcceptedFlow.asStateFlow()

    private val _addressFlow = MutableStateFlow<String?>(null)
    val addressFlow = _addressFlow.filterNotNull().distinctUntilChanged { old, new -> old == new }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _enrollmentStatusFlow = MutableSharedFlow<DataState<Long>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val enrollmentStatusFlow = _enrollmentStatusFlow.asSharedFlow()

    private val locationRef = AtomicReference<Location>()

    private val _capturedImageFlow = MutableStateFlow<Bitmap?>(null)
    val capturedImageFlow = _capturedImageFlow.asStateFlow()

    private val _similarFaceIdentifiedFlow = MutableStateFlow<Boolean?>(null)
    val similarFaceIdentifiedFlow: StateFlow<Boolean?> = _similarFaceIdentifiedFlow.asStateFlow()
    private val _faceValidityState = MutableStateFlow<FaceValidState?>(null)

    init {
        viewModelScope.launch(defaultDispatcher) {
            updateFaceValidity()
            if (_faceValidityState.value == FaceValidState.VALID) {
                _similarFaceIdentifiedFlow.update {
                    hasSimilarFace()
                }
                faceDetectionHelper.releaseInterpreter()
            }
        }
    }

    private suspend fun updateFaceValidity() {
        val bitmap = _capturedImageFlow.filterNotNull().first()
        val faces = faceDetectionHelper.findFacesInBitmap(bitmap, 0)
        if (faces.isEmpty()) {
            _faceValidityState.update { FaceValidState.NO_FACE }
        } else if (faces.size > 1) {
            _faceValidityState.update { FaceValidState.MULTIPLE_FACES }
        } else if (faces[0].hasMinOpenProbability(LIVE_FACE_MIN_PROBABILITY).not()) {
            _faceValidityState.update { FaceValidState.NOT_LIVE }
        } else {
            _faceValidityState.update { FaceValidState.VALID }
        }
    }

    private suspend fun hasSimilarFace(): Boolean {
        val enrolledFacesPath = kotlin.runCatching {
            userRepository.getAllUsers().map { userEntity: UserEntity ->
                userEntity.imagePath
            }
        }.getOrDefault(emptyList())
        if (enrolledFacesPath.isEmpty()) {
            return false
        }
        initializeInterpreter()
        val enrolledFaces = enrolledFacesPath.mapNotNull { imagePath ->
            readFaceData(imagePath)
        }
        if (enrolledFaces.isEmpty()) {
            return false
        }
        val capturedBitmap = _capturedImageFlow.filterNotNull().first()
        val sourceData =
            runCatching {
                faceDetectionHelper.readFacesData(
                    capturedBitmap,
                    false
                ).values.firstOrNull()
            }.getOrNull()
                ?: return false
        return enrolledFaces.minOfOrNull { enrolledFace ->
            runCatching {
                faceDetectionHelper.findDistance(sourceData, enrolledFace)
            }.getOrDefault(Float.MAX_VALUE)
        }?.let { it <= FACE_MATCH_COMPARISON_CONFIDENCE }.orElse(false)
    }

    private suspend fun initializeInterpreter() {
        faceDetectionHelper.initializeInterpreter(
            context,
            getModelFileName(appPreferences.useFaceNet)
        )
        faceDetectionHelper.initializationState.filter { it }.first()
    }

    private suspend fun readFaceData(imagePath: String): FloatArray? {
        return runCatching {
            val bitmap = withContext(ioDispatcher) {
                BitmapFactory.decodeFile(imagePath)
            }
            withContext(defaultDispatcher) {
                faceDetectionHelper.readFacesData(bitmap, true).values.firstOrNull()
            }
        }.getOrNull()
    }

    fun setSourceBitmap(bitmap: Bitmap) {
        _capturedImageFlow.update { bitmap }
    }

    fun updateUserId(id: String) {
        _idFlow.update { id }
    }

    fun updateName(name: String) {
        _nameFlow.update { name }
    }

    fun updateDepartment(dept: String) {
        _departmentFlow.update { dept }
    }

    fun updateConditionsState(accepted: Boolean) {
        _conditionsAcceptedFlow.update { accepted }
    }

    fun updateAddress(location: Location, address: String) {
        locationRef.set(location)
        _addressFlow.update { address }
    }

    fun cancelEnroll() {
        recycleCapturedImage()
    }

    private fun recycleCapturedImage() {
        val bitmap = _capturedImageFlow.value
        _capturedImageFlow.update { null }
        bitmap?.safeRecycle()
    }

    fun enroll() {
        viewModelScope.launch(ioDispatcher) {
            val faceValidityState = _faceValidityState.filterNotNull().first()
            if (faceValidityState != FaceValidState.VALID) {
                _enrollmentStatusFlow.tryEmit(DataState.Failure(Exception("Error: ${faceValidityState.message()}")))
                return@launch
            }
            val similarFaceIdentified = _similarFaceIdentifiedFlow.filterNotNull().first()
            if (similarFaceIdentified) {
                _enrollmentStatusFlow.tryEmit(DataState.Failure(Exception("Similar face identified. May be you registered already?")))
                return@launch
            }
            runCatching {
                val userId = _idFlow.value.trim()
                if (userRepository.isUserExists(userId)) {
                    throw Exception("Employee ID already exists")
                }
                val imagePath = _capturedImageFlow.value?.let { bitmap ->
                    saveBitmap(bitmap, userId)
                }
                if (imagePath == null) {
                    throw Exception("Unable to save photo")
                }
                val location = locationRef.get()
                runCatching {
                    userRepository.insertUser(
                        UserEntity(
                            userId = userId,
                            name = _nameFlow.value.trim(),
                            department = _departmentFlow.value.trim(),
                            latitude = location.latitude,
                            longitude = location.longitude,
                            imagePath = imagePath
                        )
                    )
                }.getOrNull() ?: throw Exception("Unable to save user details")
            }.onSuccess { rowId ->
                _enrollmentStatusFlow.tryEmit(DataState.Success(rowId))
            }.onFailure { tr ->
                _enrollmentStatusFlow.tryEmit(DataState.Failure(tr))
            }
        }
    }

    private fun saveBitmap(bitmap: Bitmap, userId: String): String? {
        val imagesDir = File(context.filesDir, "user_images")
        if (imagesDir.exists().not()) {
            if (imagesDir.mkdirs().not()) {
                return null
            }
        }
        val imageFile = File(imagesDir, "${userId}.jpg")
        if (imageFile.exists().not()) {
            if (imageFile.createNewFile().not()) {
                return null
            }
        }
        imageFile.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            it.flush()
        }
        return imageFile.absolutePath
    }

    override fun onCleared() {
        super.onCleared()
        recycleCapturedImage()
    }
}