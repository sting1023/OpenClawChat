package com.sting.openclawchat.ui.screens.chat

import androidx.lifecycle.ViewModel
import com.sting.openclawchat.data.model.ChatMessage
import com.sting.openclawchat.data.repository.ConnectionState
import com.sting.openclawchat.data.repository.WebSocketClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val webSocketClient: WebSocketClient
) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = webSocketClient.messages
    val connectionState: StateFlow<ConnectionState> = webSocketClient.connectionState

    fun sendMessage(content: String) {
        if (content.isNotBlank()) {
            webSocketClient.sendMessage(content)
        }
    }

    fun reconnect() {
        webSocketClient.reconnect()
    }

    fun disconnect() {
        webSocketClient.disconnect()
    }
}
