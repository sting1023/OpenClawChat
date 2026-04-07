package com.sting.openclawchat.data.repository

import com.sting.openclawchat.data.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val message: String, val errorType: ErrorType = ErrorType.UNKNOWN) : ConnectionState()
}

enum class ErrorType {
    TIMEOUT,
    AUTH_FAILED,
    CONNECTION_REFUSED,
    PROTOCOL_ERROR,
    UNKNOWN
}

@Singleton
class WebSocketClient @Inject constructor() {
    private var webSocket: WebSocket? = null
    private var pendingRequestId: String? = null
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var currentToken: String = ""
    private var currentServerIp: String = ""
    private var currentServerPort: Int = 0

    fun connect(serverIp: String, serverPort: Int, token: String) {
        if (_connectionState.value == ConnectionState.Connecting ||
            _connectionState.value == ConnectionState.Connected) {
            return
        }

        _connectionState.value = ConnectionState.Connecting
        currentServerIp = serverIp
        currentServerPort = serverPort
        currentToken = token

        val url = "ws://$serverIp:$serverPort/gateway/ws?token=$token"
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Connection established, waiting for challenge from server
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val errorType = when {
                    t.message?.contains("timeout", ignoreCase = true) == true -> ErrorType.TIMEOUT
                    t.message?.contains("refused", ignoreCase = true) == true -> ErrorType.CONNECTION_REFUSED
                    t.message?.contains("401", ignoreCase = true) == true -> ErrorType.AUTH_FAILED
                    t.message?.contains("403", ignoreCase = true) == true -> ErrorType.AUTH_FAILED
                    t.message?.contains("canceled", ignoreCase = true) == true -> ErrorType.TIMEOUT
                    t.message?.contains("ConnectException", ignoreCase = true) == true -> ErrorType.CONNECTION_REFUSED
                    else -> ErrorType.UNKNOWN
                }
                
                val errorMessage = when (errorType) {
                    ErrorType.TIMEOUT -> "连接超时，请检查服务器地址是否正确"
                    ErrorType.AUTH_FAILED -> "认证失败，Token 无效"
                    ErrorType.CONNECTION_REFUSED -> "无法连接到服务器，请检查 IP 和端口"
                    ErrorType.PROTOCOL_ERROR -> "协议不匹配"
                    ErrorType.UNKNOWN -> t.message ?: "连接失败"
                }
                
                _connectionState.value = ConnectionState.Error(errorMessage, errorType)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (_connectionState.value !is ConnectionState.Error) {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, "Closing")
            }
        })
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type", "")

            when (type) {
                "event" -> {
                    val event = json.optString("event", "")
                    if (event == "connect.challenge") {
                        // Server sent challenge, respond with connect request
                        respondToChallenge(json.optJSONObject("payload"))
                    }
                }
                "res" -> {
                    // Response to our connect request
                    val id = json.optString("id", "")
                    val ok = json.optBoolean("ok", false)
                    
                    if (id == pendingRequestId || pendingRequestId == "connect") {
                        pendingRequestId = null
                        if (ok) {
                            _connectionState.value = ConnectionState.Connected
                        } else {
                            val payload = json.optJSONObject("payload")
                            val errorMsg = payload?.optString("error", "认证失败") ?: "认证失败"
                            _connectionState.value = ConnectionState.Error(errorMsg, ErrorType.AUTH_FAILED)
                        }
                    }
                    
                    // Handle message responses
                    val payload = json.optJSONObject("payload")
                    if (payload?.optString("type") == "message-ok") {
                        // Message sent successfully
                    }
                }
                "message" -> {
                    // Incoming chat message
                    val content = json.optString("content", "")
                    if (content.isNotBlank()) {
                        val message = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            content = content,
                            isUser = false
                        )
                        _messages.value = _messages.value + message
                    }
                }
            }
        } catch (e: Exception) {
            // Handle parsing error - might be a simple text message
            if (text.isNotBlank() && _connectionState.value == ConnectionState.Connected) {
                val message = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = text,
                    isUser = false
                )
                _messages.value = _messages.value + message
            }
        }
    }

    private fun respondToChallenge(payload: JSONObject?) {
        pendingRequestId = UUID.randomUUID().toString()
        
        val response = JSONObject().apply {
            put("type", "req")
            put("id", pendingRequestId)
            put("method", "connect")
            put("payload", JSONObject().apply {
                put("type", "connect-request")
                put("token", currentToken)
            })
        }
        
        webSocket?.send(response.toString())
    }

    fun sendMessage(content: String) {
        if (_connectionState.value !is ConnectionState.Connected) {
            return
        }

        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = content,
            isUser = true
        )
        _messages.value = _messages.value + message

        val requestId = UUID.randomUUID().toString()
        val json = JSONObject().apply {
            put("type", "req")
            put("id", requestId)
            put("method", "message")
            put("payload", JSONObject().apply {
                put("type", "message-send")
                put("content", content)
                put("timestamp", System.currentTimeMillis())
            })
        }
        
        val sent = webSocket?.send(json.toString()) ?: false
        if (!sent) {
            // Remove the optimistically added message if send failed
            _messages.value = _messages.value.filter { it.id != message.id }
        }
    }

    fun isConnected(): Boolean = _connectionState.value == ConnectionState.Connected

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
        _messages.value = emptyList()
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    fun reconnect() {
        if (currentServerIp.isNotBlank() && currentToken.isNotBlank()) {
            disconnect()
            connect(currentServerIp, currentServerPort, currentToken)
        }
    }
}
