package com.sting.openclawchat.di

import android.content.Context
import com.sting.openclawchat.data.datastore.SettingsDataStore
import com.sting.openclawchat.data.repository.WebSocketClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context
    ): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideWebSocketClient(): WebSocketClient {
        return WebSocketClient()
    }
}
