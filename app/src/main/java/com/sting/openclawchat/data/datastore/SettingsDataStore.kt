package com.sting.openclawchat.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sting.openclawchat.data.model.ConnectionSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_CONNECTION_NAME = stringPreferencesKey("connection_name")
        private val KEY_SERVER_IP = stringPreferencesKey("server_ip")
        private val KEY_SERVER_PORT = intPreferencesKey("server_port")
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
    }

    val settingsFlow: Flow<ConnectionSettings> = context.dataStore.data.map { preferences ->
        ConnectionSettings(
            connectionName = preferences[KEY_CONNECTION_NAME] ?: "",
            serverIp = preferences[KEY_SERVER_IP] ?: "",
            serverPort = preferences[KEY_SERVER_PORT] ?: 18789,
            authToken = preferences[KEY_AUTH_TOKEN] ?: ""
        )
    }

    suspend fun saveSettings(settings: ConnectionSettings) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CONNECTION_NAME] = settings.connectionName
            preferences[KEY_SERVER_IP] = settings.serverIp
            preferences[KEY_SERVER_PORT] = settings.serverPort
            preferences[KEY_AUTH_TOKEN] = settings.authToken
        }
    }

    suspend fun clearSettings() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
