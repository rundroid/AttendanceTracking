package com.learning.attendancetracking.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.learning.attendancetracking.database.entity.UserAttendanceRelation
import com.learning.attendancetracking.database.entity.UserEntity

@Dao
interface UserDao {

    @Query("SELECT COUNT() FROM ${UserEntity.TABLE_NAME} WHERE ${UserEntity.COLUMN_USER_ID} = :id")
    suspend fun isUserExists(id: String): Boolean

    @Query("SELECT * FROM ${UserEntity.TABLE_NAME}")
    suspend fun getAllUsers(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(userEntity: UserEntity): Long

    @Query("SELECT * FROM ${UserEntity.TABLE_NAME} WHERE ${UserEntity.COLUMN_USER_ID} = :id")
    suspend fun getUser(id: String): UserEntity

    @Transaction
    @Query("SELECT * FROM ${UserEntity.TABLE_NAME}")
    suspend fun getUsersAttendanceHistory(): List<UserAttendanceRelation>
}