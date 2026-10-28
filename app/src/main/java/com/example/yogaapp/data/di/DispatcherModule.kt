package com.example.yogaapp.data.di

import com.example.yogaapp.domain.util.AppDispatchers
import com.example.yogaapp.domain.util.DefaultAppDispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the production [AppDispatchers]. Tests override this binding by constructing
 * the VM with a custom dispatcher — no Hilt test replacement needed for pure VM unit tests.
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Provides
    @Singleton
    fun provideAppDispatchers(): AppDispatchers = DefaultAppDispatchers()
}
