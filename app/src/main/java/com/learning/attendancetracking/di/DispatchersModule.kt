package com.learning.attendancetracking.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @Named("default")
    fun providesDefaultDispatcher() = Dispatchers.Default

    @Provides
    @Named("io")
    fun providesIODispatcher() = Dispatchers.IO
}