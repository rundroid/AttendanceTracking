package com.learning.attendancetracking.database.repository

import com.learning.attendancetracking.database.dao.UserDao
import com.learning.attendancetracking.database.entity.UserAttendanceRelation
import com.learning.attendancetracking.database.entity.UserEntity
import javax.inject.Inject

class UserRepository @Inject constructor(private val userDao: UserDao) {

    suspend fun isUserExists(id: String): Boolean = userDao.isUserExists(id)

    suspend fun getAllUsers(): List<UserEntity> = userDao.getAllUsers()

    suspend fun getUser(id: String): UserEntity = userDao.getUser(id)

    suspend fun insertUser(userEntity: UserEntity): Long = userDao.insertUser(userEntity)

    suspend fun getUsersAttendanceHistory(): List<UserAttendanceRelation> =
        userDao.getUsersAttendanceHistory()
}