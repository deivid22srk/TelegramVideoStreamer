package com.deivid.telegramvideo.di

import android.content.Context
import com.deivid.telegramvideo.data.repository.TelegramClient
import com.deivid.telegramvideo.data.repository.TelegramRepository
import com.deivid.telegramvideo.data.repository.VideoModeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo Hilt que provê as dependências principais do app.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTelegramClient(
        @ApplicationContext context: Context
    ): TelegramClient = TelegramClient(context)

    @Provides
    @Singleton
    fun provideTelegramRepository(
        telegramClient: TelegramClient
    ): TelegramRepository = TelegramRepository(telegramClient)

    @Provides
    @Singleton
    fun provideVideoModeRepository(
        @ApplicationContext context: Context,
        telegramClient: TelegramClient
    ): VideoModeRepository = VideoModeRepository(context, telegramClient)
}
