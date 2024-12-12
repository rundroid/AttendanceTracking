package com.learning.attendancetracking.enroll.ui.compose

import android.Manifest
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.learning.attendancetracking.R
import com.learning.attendancetracking.enroll.CapturedImageSharedViewModel
import com.learning.attendancetracking.enroll.EnrollmentViewModel
import com.learning.attendancetracking.extensions.showToast
import com.learning.attendancetracking.ui.compose.LocationUpdatesComposable
import com.learning.attendancetracking.utils.DataState
import com.learning.attendancetracking.utils.USER_ID_MIN_LENGTH
import com.learning.attendancetracking.utils.orElse
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun EnrollmentScreen(
    viewModel: EnrollmentViewModel,
    sharedViewModel: CapturedImageSharedViewModel,
    onEnrolledCallback: (String) -> Unit,
    cancelEnroll: () -> Unit
) {
    val context = LocalContext.current
    val permissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    if (permissionState.allPermissionsGranted) {
        val userId by viewModel.idFlow.collectAsStateWithLifecycle()
        val userName by viewModel.nameFlow.collectAsStateWithLifecycle()
        val department by viewModel.departmentFlow.collectAsStateWithLifecycle()
        val termsAndConditionsAccepted by viewModel.conditionsAcceptedFlow.collectAsStateWithLifecycle()
        val addressString by viewModel.addressFlow.collectAsStateWithLifecycle()
        viewModel.similarFaceIdentifiedFlow.collectAsStateWithLifecycle()
        val bitmapState by viewModel.capturedImageFlow.collectAsStateWithLifecycle()
        CollectData(context, viewModel, sharedViewModel, onEnrolledCallback)
        LocationUpdatesComposable { location, address ->
            viewModel.updateAddress(location, address.getAddressLine(0).orEmpty())
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            bitmapState?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "captured photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(160.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = userId,
                onValueChange = {
                    viewModel.updateUserId(it)
                },
                label = { Text("Employee id") },
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(fraction = 0.8f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = userName,
                onValueChange = {
                    viewModel.updateName(it)
                },
                label = { Text("Employee Name") },
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(fraction = 0.8f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = department,
                onValueChange = {
                    viewModel.updateDepartment(it)
                },
                label = { Text("Department") },
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(fraction = 0.8f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            addressString?.let {
                Text("Current location\n${addressString}")
            } ?: run {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Identifying current location")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = termsAndConditionsAccepted, onCheckedChange = {
                    viewModel.updateConditionsState(it)
                })
                Text("Agree to T&C")
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Button(onClick = {
                    viewModel.cancelEnroll()
                    cancelEnroll()
                }) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(enabled = userId.trim().length >= USER_ID_MIN_LENGTH
                        && userName.isNotBlank()
                        && department.isNotBlank()
                        && addressString.isNullOrBlank().not()
                        && termsAndConditionsAccepted,
                    onClick = {
                        viewModel.enroll()
                    }) {
                    Text(text = stringResource(R.string.enroll))
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val permissionMessage = if (permissionState.shouldShowRationale) {
                "Location is important for this app. Please grant the permission."
            } else {
                "Location permission required for this feature to be available. Please grant the permission"
            }
            Text(permissionMessage)
            Spacer(modifier = Modifier.height(30.dp))
            Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                Text(stringResource(R.string.request_permission))
            }
        }
    }
}

@Composable
private fun CollectData(
    context: Context,
    viewModel: EnrollmentViewModel,
    sharedViewModel: CapturedImageSharedViewModel,
    onEnrolledCallback: (String) -> Unit,
) {
    val currentEnrolledCallback = rememberUpdatedState(onEnrolledCallback).value
    LaunchedEffect(true) {
        launch {
            viewModel.enrollmentStatusFlow.collect { state ->
                when (state) {
                    is DataState.Success -> {
                        context.showToast("Enrolled successfully")
                        sharedViewModel.clearCapturedImage(true)
                        currentEnrolledCallback.invoke(viewModel.idFlow.value)
                    }

                    is DataState.Failure -> {
                        context.showToast(state.throwable.message.orElse { "Unable to save user details" })
                    }

                    else -> {

                    }
                }
            }
        }
        launch {
            sharedViewModel.imageCaptured.filterNotNull().collect { bitmap ->
                viewModel.setSourceBitmap(bitmap)
                sharedViewModel.clearCapturedImage(false)
            }
        }
    }
}