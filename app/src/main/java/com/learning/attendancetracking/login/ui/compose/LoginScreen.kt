package com.learning.attendancetracking.login.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learning.attendancetracking.R
import com.learning.attendancetracking.extensions.showToast
import com.learning.attendancetracking.login.LoginViewModel
import com.learning.attendancetracking.utils.USER_ID_MIN_LENGTH
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (String) -> Unit,
    onEnrollClick: () -> Unit
) {
    LocalLifecycleOwner.current
    val context = LocalContext.current
    val currentLoginSuccess = rememberUpdatedState(onLoginSuccess).value
    val userId by viewModel.idFlow.collectAsStateWithLifecycle()
    val loginButtonEnabled by remember {
        derivedStateOf { userId.trim().length >= USER_ID_MIN_LENGTH }
    }
    LaunchedEffect(true) {
        launch {
            viewModel.loginState.collect { success ->
                if (success) {
                    currentLoginSuccess.invoke(userId)
                } else {
                    context.showToast("User does not exists. Please enroll")
                }
            }
        }
    }
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = userId,
            onValueChange = {
                viewModel.updateUserId(it)
            },
            label = { Text("Employee id") },
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(fraction = 0.75f)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(enabled = loginButtonEnabled, onClick = {
            viewModel.login(userId)
        }) {
            Text(stringResource(R.string.login))
        }
        Spacer(modifier = Modifier.height(30.dp))
        HorizontalDivider(modifier = Modifier.fillMaxWidth(0.8F))
        Spacer(modifier = Modifier.height(30.dp))
        Text("New user?")
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = onEnrollClick) {
            Text(stringResource(R.string.enroll))
        }
    }
}