package com.learning.attendancetracking.home.ui.compose

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learning.attendancetracking.R
import com.learning.attendancetracking.home.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    loginUpdateRequest: (String, Boolean) -> Unit,
    logout: () -> Unit
) {
    val context = LocalContext.current
    val userEntity by viewModel.userEntityFlow.collectAsStateWithLifecycle()
    val rowId by viewModel.loginRowId.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { result ->
            result?.let {
                context.contentResolver.takePersistableUriPermission(
                    result, Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                viewModel.persistUri(result)
                viewModel.exportAsCSV(result)
            }
        }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hello ${userEntity?.name.orEmpty()}",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(30.dp))
        Button(enabled = rowId == null && userEntity != null, onClick = {
            userEntity?.userId?.let { id ->
                loginUpdateRequest(id, true)
            }
        }) {
            Text(stringResource(R.string.record_login))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(enabled = rowId != null && userEntity != null, onClick = {
            userEntity?.userId?.let { id ->
                loginUpdateRequest(id, false)
            }
        }) {
            Text(stringResource(R.string.record_logout))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(enabled = isExporting.not(), onClick = {
            viewModel.getDocumentUri()?.let {
                viewModel.exportAsCSV(it)
            } ?: run {
                launcher.launch(null)
            }
        }) {
            if (isExporting) {
                CircularProgressIndicator()
            } else {
                Text(stringResource(R.string.report))
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = logout) {
            Text(stringResource(R.string.logout))
        }
    }
}