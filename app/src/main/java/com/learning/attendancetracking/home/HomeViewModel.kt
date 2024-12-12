package com.learning.attendancetracking.home

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.learning.attendancetracking.database.entity.AttendanceEntity
import com.learning.attendancetracking.database.entity.LogoutAttendanceEntity
import com.learning.attendancetracking.database.repository.AttendanceRepository
import com.learning.attendancetracking.database.repository.UserRepository
import com.learning.attendancetracking.extensions.showToast
import com.learning.attendancetracking.utils.exportAttendanceReport
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val attendanceRepository: AttendanceRepository,
    @Named("io") private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _userIdFlow = savedStateHandle.getStateFlow<String?>("userId", null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val userEntityFlow = _userIdFlow.filterNotNull().flatMapLatest { id ->
        flow { emit(userRepository.getUser(id)) }
    }.flowOn(ioDispatcher).stateIn(viewModelScope, SharingStarted.Lazily, null)

    val loginRowId = savedStateHandle.getStateFlow<Long?>(KEY_LOGIN_ROW_ID, null)

    private val _isExporting = MutableStateFlow(false)
    val isExporting = _isExporting.asStateFlow()

    fun recordLogin() {
        val userId = _userIdFlow.value ?: return
        viewModelScope.launch(ioDispatcher) {
            runCatching {
                attendanceRepository.recordLogin(
                    AttendanceEntity(
                        userId = userId, loginTime = Date()
                    )
                )
            }.onSuccess { rowId ->
                context.showToast("Attendance saved successfully")
                savedStateHandle[KEY_LOGIN_ROW_ID] = rowId
            }
        }
    }

    fun recordLogout() {
        viewModelScope.launch(ioDispatcher) {
            loginRowId.value?.let { rowId ->
                runCatching {
                    attendanceRepository.recordLogout(
                        LogoutAttendanceEntity(
                            id = rowId, logoutTime = Date()
                        )
                    )
                }.onSuccess {
                    context.showToast("Attendance saved successfully")
                    savedStateHandle[KEY_LOGIN_ROW_ID] = null
                }
            }
        }
    }

    fun persistUri(uri: Uri) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(KEY_EXTERNAL_DOCUMENT_TREE_URI, uri.toString())
        }
    }

    fun getDocumentUri(): Uri? {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
            KEY_EXTERNAL_DOCUMENT_TREE_URI, null
        )?.let { uriString ->
            DocumentFile.fromTreeUri(context, Uri.parse(uriString))
                ?.takeIf { dFile -> dFile.exists() && dFile.canWrite() }?.uri
        }
    }

    fun exportAsCSV(uri: Uri) {
        val documentFile = DocumentFile.fromTreeUri(context, uri) ?: return
        viewModelScope.launch(ioDispatcher) {
            val recordsList = userRepository.getUsersAttendanceHistory()
            if (recordsList.isEmpty()) {
                return@launch
            }
            _isExporting.update { true }
            exportAttendanceReport(context, documentFile, recordsList)
            _isExporting.update { false }
        }
    }

    companion object {
        private const val KEY_LOGIN_ROW_ID = "login_row_id"
        private const val KEY_EXTERNAL_DOCUMENT_TREE_URI = "external_document_tree_uri"
    }
}