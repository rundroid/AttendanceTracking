package com.learning.attendancetracking.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = UserEntity.TABLE_NAME)
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = COLUMN_USER_ID)
    val userId: String,
    @ColumnInfo(name = COLUMN_NAME)
    val name: String,
    @ColumnInfo(name = COLUMN_DEPARTMENT)
    val department: String,
    @ColumnInfo(name = COLUMN_LATITUDE)
    val latitude: Double,
    @ColumnInfo(name = COLUMN_LONGITUDE)
    val longitude: Double,
    @ColumnInfo(name = COLUMN_IMAGE_PATH)
    val imagePath: String
) {
    companion object {
        const val TABLE_NAME = "users"
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_NAME = "name"
        const val COLUMN_DEPARTMENT = "department"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_IMAGE_PATH = "image_path"
    }
}