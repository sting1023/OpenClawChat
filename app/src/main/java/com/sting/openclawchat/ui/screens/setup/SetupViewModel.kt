package com.sting.openclawchat.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sting.openclawchat.data.datastore.SettingsDataStore
import com.sting.openclawchat.data.model.ConnectionSettings
import com.sting.openclawchat.data.repository.ConnectionState
import com.sting.openclawchat.data.repository.WebSocketClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val webSocketClient: WebSocketClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = webSocketClient.connectionState

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = settingsDataStore.settingsFlow.first()
            _uiState.value = _uiState.value.copy(
                connectionName = settings.connectionName,
                serverIp = settings.serverIp,
                serverPort = settings.serverPort.toString(),
                authToken = settings.authToken
            )
        }
    }

    fun updateConnectionName(name: String) {
        _uiState.value = _uiState.value.copy(connectionName = name)
    }

    fun updateServerIp(ip: String) {
        _uiState.value = _uiState.value.copy(serverIp = ip)
    }

    fun updateServerPort(port: String) {
        _uiState.value = _uiState.value.copy(serverPort = port)
    }

    fun updateAuthToken(token: String) {
        _uiState.value = _uiState.value.copy(authToken = token)
    }

    fun saveSettings() {
        viewModelScope.launch {
            val state = _uiState.value
            val settings = ConnectionSettings(
                connectionName = state.connectionName,
                serverIp = state.serverIp,
                serverPort = state.serverPort.toIntOrNull() ?: 18789,
                authToken = state.authToken
            )
            settingsDataStore.saveSettings(settings)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    fun testConnection() {
        val state = _uiState.value
        if (state.serverIp.isBlank() || state.authToken.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "IP and Token are required")
            return
        }

        saveSettings()
        val port = state.serverPort.toIntOrNull() ?: 18789
        webSocketClient.connect(state.serverIp, port, state.authToken)
    }

    fun connect() {
        val state = _uiState.value
        if (state.serverIp.isBlank() || state.authToken.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "IP and Token are required")
            return
        }

        saveSettings()
        val port = state.serverPort.toIntOrNull() ?: 18789
        webSocketClient.connect(state.serverIp, port, state.authToken)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class SetupUiState(
    val connectionName: String = "",
    val serverIp: String = "",
    val serverPort: String = "18789",
    val authToken: String = "",
    val isSaved: Boolean = false,
    val error: String? = null
)
