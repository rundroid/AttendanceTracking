package com.learning.attendancetracking.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import com.learning.attendancetracking.R
import com.learning.attendancetracking.ui.ActionMenu
import com.learning.attendancetracking.utils.orElse


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(navController: NavHostController, actionMenuList: SnapshotStateList<ActionMenu>) {
    val titleText by navigationTitleState(navController)
    val isTopLevelDestination by isTopLevelDestinationState(navController)
    CenterAlignedTopAppBar(title = {
        AnimatedContent(titleText.takeIf { it.isNotBlank() }
            .orElse { stringResource(R.string.app_name) },
            label = "top_app_bar_title_content"
        ) { mTitleText ->
            Text(
                text = mTitleText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        }
    }, navigationIcon = {
        AnimatedVisibility(visible = isTopLevelDestination.not()) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
            }
        }
    }, actions = {
        for (actionMenu in actionMenuList) {
            IconButton(onClick = actionMenu.onClick) {
                Icon(
                    imageVector = actionMenu.imageVector,
                    contentDescription = actionMenu.contentDescription,
                )
            }
        }
    })
}