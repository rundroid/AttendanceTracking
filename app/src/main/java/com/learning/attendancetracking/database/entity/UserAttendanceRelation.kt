package com.learning.attendancetracking.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class UserAttendanceRelation(
    @Embedded val userEntity: UserEntity,
    @Relation(
        parentColumn = UserEntity.COLUMN_USER_ID,
        entityColumn = AttendanceEntity.COLUMN_USER_ID
    )
    val attendanceHistory: List<AttendanceEntity>
)
