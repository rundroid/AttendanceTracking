package com.learning.attendancetracking.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import com.learning.attendancetracking.database.entity.AttendanceEntity
import com.learning.attendancetracking.database.entity.LogoutAttendanceEntity

@Dao
interface AttendanceDao {

    @Insert
    suspend fun recordLogin(entity: AttendanceEntity): Long

    @Update(entity = AttendanceEntity::class)
    suspend fun recordLogout(entity: LogoutAttendanceEntity)
}