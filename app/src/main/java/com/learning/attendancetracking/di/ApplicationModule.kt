package com.learning.attendancetracking.di

import android.content.Context
import com.learning.attendancetracking.prefs.AppPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    @Provides
    @Singleton
    fun providesAppPreferences(@ApplicationContext context: Context) = AppPreferences(context)
}