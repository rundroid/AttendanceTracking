package com.learning.attendancetracking.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.learning.attendancetracking.database.entity.AttendanceEntity.Companion.COLUMN_ID
import java.util.Date

@Entity(tableName = AttendanceEntity.TABLE_NAME)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = COLUMN_ID)
    val id: Long? = null,
    @ColumnInfo(name = COLUMN_USER_ID)
    val userId: String,
    @ColumnInfo(name = COLUMN_LOGIN_TIME)
    val loginTime: Date,
    @ColumnInfo(name = COLUMN_LOG_OUT_TIME)
    val logoutTime: Date? = null
) {
    companion object {
        const val TABLE_NAME = "attendance"
        const val COLUMN_ID = "id"
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_LOGIN_TIME = "login_time"
        const val COLUMN_LOG_OUT_TIME = "logout_time"
    }
}

@Entity
data class LogoutAttendanceEntity(
    @ColumnInfo(name = COLUMN_ID)
    val id: Long,
    @ColumnInfo(name = AttendanceEntity.COLUMN_LOG_OUT_TIME)
    val logoutTime: Date,
)

