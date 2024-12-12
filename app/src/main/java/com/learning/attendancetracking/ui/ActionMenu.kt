package com.learning.attendancetracking.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class ActionMenu(
    val imageVector: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit
) {

    @OptIn(ExperimentalUuidApi::class)
    private val id = Uuid.random().toHexString()

    override fun equals(other: Any?): Boolean {
        other ?: return false
        return other is ActionMenu && other.id == id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

interface ActionMenuHandler {

    fun addActionMenu(actionMenu: ActionMenu)

    fun removeActionMenu(actionMenu: ActionMenu)
}

val LocalActionMenuHandler = compositionLocalOf<ActionMenuHandler?> { null }
