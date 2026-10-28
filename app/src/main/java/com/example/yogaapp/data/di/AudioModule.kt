package com.example.yogaapp.data.di

import com.example.yogaapp.data.audio.PoseAudioPlayer
import com.example.yogaapp.data.audio.TtsPoseAudioPlayer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindPoseAudioPlayer(impl: TtsPoseAudioPlayer): PoseAudioPlayer
}
