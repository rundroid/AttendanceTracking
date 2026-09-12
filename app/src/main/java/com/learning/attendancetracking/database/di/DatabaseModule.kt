package com.learning.attendancetracking.database.di

import android.content.Context
import androidx.room.Room
import com.learning.attendancetracking.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "attendance_database"
        ).build()
    }

    @Singleton
    @Provides
    fun providesUserDao(database: AppDatabase) = database.userDao()

    @Singleton
    @Provides
    fun providesAttendanceDao(database: AppDatabase) = database.attendanceDao()
}