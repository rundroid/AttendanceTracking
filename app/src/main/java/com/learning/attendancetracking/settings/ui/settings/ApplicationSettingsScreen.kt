package com.learning.attendancetracking.settings.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.learning.attendancetracking.R
import com.learning.attendancetracking.settings.AppSettingsViewModel

@Composable
fun ApplicationSettingsScreen(viewModel: AppSettingsViewModel) {
    val radiusString by viewModel.radiusFlow.collectAsStateWithLifecycle()
    var useFaceNet by remember { mutableStateOf(viewModel.useFaceNet) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.use_large_model))
            Switch(checked = useFaceNet, onCheckedChange = { checked ->
                useFaceNet = checked
                viewModel.useFaceNet = checked
            })
        }
        HorizontalDivider()
        OutlinedTextField(
            value = radiusString,
            onValueChange = { value ->
                viewModel.updateRadius(value)
            },
            label = { Text("Geofence radius in meters") },
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(fraction = 0.75f),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
        )
    }
}