package com.learning.attendancetracking.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learning.attendancetracking.database.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    @Named("io") val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _idFlow = MutableStateFlow("")
    val idFlow = _idFlow.asStateFlow()
    private val _loginState = MutableSharedFlow<Boolean>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val loginState = _loginState.asSharedFlow()

    fun updateUserId(id: String) {
        _idFlow.update { id }
    }

    fun login(userId: String) {
        viewModelScope.launch(ioDispatcher) {
            val exists = runCatching {
                userRepository.isUserExists(userId)
            }.getOrDefault(false)
            _loginState.emit(exists)
        }
    }
}