package com.sting.openclawchat.data.model

data class ConnectionSettings(
    val connectionName: String = "",
    val serverIp: String = "",
    val serverPort: Int = 18789,
    val authToken: String = ""
)
