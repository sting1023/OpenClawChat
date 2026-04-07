package com.sting.openclawchat.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sting.openclawchat.data.model.ChatMessage
import com.sting.openclawchat.data.repository.ConnectionState
import com.sting.openclawchat.data.repository.ErrorType
import com.sting.openclawchat.ui.theme.AIBubbleColor
import com.sting.openclawchat.ui.theme.AITextColor
import com.sting.openclawchat.ui.theme.UserBubbleColor
import com.sting.openclawchat.ui.theme.UserTextColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    // Show snackbar when trying to send while disconnected
    LaunchedEffect(inputText) {
        // This will be triggered when user tries to send
    }

    val connectionStatusText = when (connectionState) {
        is ConnectionState.Connected -> "已连接"
        is ConnectionState.Connecting -> "连接中..."
        is ConnectionState.Error -> {
            val error = connectionState as ConnectionState.Error
            when (error.errorType) {
                ErrorType.TIMEOUT -> "连接超时"
                ErrorType.AUTH_FAILED -> "认证失败"
                ErrorType.CONNECTION_REFUSED -> "连接被拒绝"
                ErrorType.PROTOCOL_ERROR -> "协议错误"
                ErrorType.UNKNOWN -> "连接失败"
            }
        }
        is ConnectionState.Disconnected -> "未连接"
    }

    val connectionStatusColor = when (connectionState) {
        is ConnectionState.Connected -> MaterialTheme.colorScheme.primary
        is ConnectionState.Connecting -> MaterialTheme.colorScheme.tertiary
        is ConnectionState.Error -> MaterialTheme.colorScheme.error
        is ConnectionState.Disconnected -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("OpenClaw Chat")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            when (connectionState) {
                                is ConnectionState.Connected -> {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                is ConnectionState.Connecting -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(10.dp),
                                        strokeWidth = 1.5.dp
                                    )
                                }
                                is ConnectionState.Error -> {
                                    Icon(
                                        imageVector = Icons.Filled.Error,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                is ConnectionState.Disconnected -> {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = connectionStatusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = connectionStatusColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    if (connectionState is ConnectionState.Error || connectionState is ConnectionState.Disconnected) {
                        IconButton(onClick = { viewModel.reconnect() }) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "重连"
                            )
                        }
                    }
                    IconButton(onClick = {
                        viewModel.disconnect()
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "断开连接"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
        ) {
            // Disconnection Banner
            AnimatedVisibility(
                visible = connectionState is ConnectionState.Error || connectionState is ConnectionState.Disconnected,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                val backgroundColor = if (connectionState is ConnectionState.Error) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val error = connectionState as? ConnectionState.Error
                    val message = if (error != null) {
                        when (error.errorType) {
                            ErrorType.TIMEOUT -> "连接超时，请检查服务器地址"
                            ErrorType.AUTH_FAILED -> "认证失败，Token 无效"
                            ErrorType.CONNECTION_REFUSED -> "无法连接到服务器"
                            ErrorType.PROTOCOL_ERROR -> "协议不匹配"
                            ErrorType.UNKNOWN -> error.message
                        }
                    } else {
                        "已断开连接"
                    }
                    
                    Text(
                        text = message,
                        color = if (connectionState is ConnectionState.Error) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatBubble(message = message)
                }
            }

            // Input area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息...") },
                    maxLines = 4,
                    enabled = connectionState is ConnectionState.Connected
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (connectionState !is ConnectionState.Connected) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "未连接，请先连接 Gateway",
                                    duration = SnackbarDuration.Short,
                                    withDismissAction = true
                                )
                            }
                            return@IconButton
                        }
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = true // Always enabled, we show snackbar on click if not connected
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送",
                        tint = if (connectionState is ConnectionState.Connected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val bubbleColor = if (message.isUser) UserBubbleColor else AIBubbleColor
    val textColor = if (message.isUser) UserTextColor else AITextColor
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (message.isUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = textColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
