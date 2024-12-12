package com.learning.attendancetracking.database.repository

import com.learning.attendancetracking.database.dao.AttendanceDao
import com.learning.attendancetracking.database.entity.AttendanceEntity
import com.learning.attendancetracking.database.entity.LogoutAttendanceEntity
import javax.inject.Inject

class AttendanceRepository @Inject constructor(private val attendanceDao: AttendanceDao) {

    suspend fun recordLogin(entity: AttendanceEntity): Long = attendanceDao.recordLogin(entity)

    suspend fun recordLogout(entity: LogoutAttendanceEntity) = attendanceDao.recordLogout(entity)
}