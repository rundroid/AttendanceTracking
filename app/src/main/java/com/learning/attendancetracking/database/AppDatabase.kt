package com.learning.attendancetracking.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.learning.attendancetracking.database.converters.DateConverter
import com.learning.attendancetracking.database.dao.AttendanceDao
import com.learning.attendancetracking.database.dao.UserDao
import com.learning.attendancetracking.database.entity.AttendanceEntity
import com.learning.attendancetracking.database.entity.UserEntity

@Database(entities = [UserEntity::class, AttendanceEntity::class], version = 1)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun attendanceDao(): AttendanceDao
}