package com.learning.attendancetracking.enroll

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.learning.attendancetracking.utils.safeRecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class CapturedImageSharedViewModel @Inject constructor() : ViewModel() {
    private val _imageCaptured = MutableStateFlow<Bitmap?>(null)
    val imageCaptured = _imageCaptured.asStateFlow()

    fun updateImage(bitmap: Bitmap) {
        _imageCaptured.update { bitmap }
    }

    fun clearCapturedImage(recycle: Boolean) {
        _imageCaptured.update {
            if (recycle) {
                it?.safeRecycle()
            }
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        clearCapturedImage(true)
    }
}