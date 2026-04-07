package com.sting.openclawchat.ui.screens.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sting.openclawchat.data.repository.ConnectionState
import com.sting.openclawchat.data.repository.ErrorType
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onNavigateToChat: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    var testConnectionResult by remember { mutableStateOf<TestResult?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    LaunchedEffect(connectionState) {
        if (connectionState is ConnectionState.Connected) {
            onNavigateToChat()
        }
    }

    // Update test result when connection state changes during testing
    LaunchedEffect(connectionState, isTesting) {
        if (isTesting) {
            delay(500) // Small delay to let connection start
            when (connectionState) {
                is ConnectionState.Connected -> {
                    testConnectionResult = TestResult.Success("连接成功")
                    isTesting = false
                }
                is ConnectionState.Error -> {
                    val error = connectionState as ConnectionState.Error
                    testConnectionResult = TestResult.Error(getErrorDisplayMessage(error))
                    isTesting = false
                }
                else -> {}
            }
        }
    }

    // Auto-clear test result after 3 seconds
    LaunchedEffect(testConnectionResult) {
        if (testConnectionResult != null) {
            delay(3000)
            testConnectionResult = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OpenClaw 连接设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.connectionName,
                onValueChange = viewModel::updateConnectionName,
                label = { Text("连接名称") },
                placeholder = { Text("如：我的OpenClaw") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = uiState.serverIp,
                onValueChange = {
                    viewModel.updateServerIp(it)
                    testConnectionResult = null
                },
                label = { Text("IP 地址") },
                placeholder = { Text("如：192.168.5.x") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { /* focus next */ })
            )

            OutlinedTextField(
                value = uiState.serverPort,
                onValueChange = {
                    viewModel.updateServerPort(it)
                    testConnectionResult = null
                },
                label = { Text("端口号") },
                placeholder = { Text("默认：18789") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = uiState.authToken,
                onValueChange = {
                    viewModel.updateAuthToken(it)
                    testConnectionResult = null
                },
                label = { Text("Token 密钥") },
                placeholder = { Text("输入你的认证 Token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (uiState.serverIp.isNotBlank() && uiState.authToken.isNotBlank()) {
                        viewModel.testConnection()
                        isTesting = true
                        testConnectionResult = null
                    }
                })
            )

            // Test Connection Button with Result
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.testConnection()
                        isTesting = true
                        testConnectionResult = null
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isTesting && uiState.serverIp.isNotBlank() && uiState.authToken.isNotBlank()
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("测试中...")
                    } else {
                        Text("测试连接")
                    }
                }

                AnimatedVisibility(
                    visible = testConnectionResult != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    testConnectionResult?.let { result ->
                        when (result) {
                            is TestResult.Success -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "成功",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = result.message,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            is TestResult.Error -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "失败",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = result.message,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Connection Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (connectionState) {
                        is ConnectionState.Connected -> MaterialTheme.colorScheme.primaryContainer
                        is ConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
                        is ConnectionState.Connecting -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "连接状态：",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (connectionState is ConnectionState.Connecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = getConnectionStatusText(connectionState),
                            style = MaterialTheme.typography.bodyLarge,
                            color = when (connectionState) {
                                is ConnectionState.Connected -> MaterialTheme.colorScheme.primary
                                is ConnectionState.Error -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // Error message display
            if (connectionState is ConnectionState.Error) {
                val error = connectionState as ConnectionState.Error
                Text(
                    text = getErrorDisplayMessage(error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.connect() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = connectionState !is ConnectionState.Connecting &&
                        uiState.serverIp.isNotBlank() &&
                        uiState.authToken.isNotBlank()
            ) {
                Text(
                    text = if (connectionState is ConnectionState.Connecting) "连接中..." else "连接",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

sealed class TestResult {
    data class Success(val message: String) : TestResult()
    data class Error(val message: String) : TestResult()
}

private fun getConnectionStatusText(state: ConnectionState): String {
    return when (state) {
        is ConnectionState.Connected -> "已连接"
        is ConnectionState.Error -> "连接失败"
        is ConnectionState.Connecting -> "连接中..."
        is ConnectionState.Disconnected -> "未连接"
    }
}

private fun getErrorDisplayMessage(error: ConnectionState.Error): String {
    return when (error.errorType) {
        ErrorType.TIMEOUT -> "连接超时，请检查服务器地址是否正确"
        ErrorType.AUTH_FAILED -> "认证失败，Token 无效"
        ErrorType.CONNECTION_REFUSED -> "无法连接到服务器，请检查 IP 和端口"
        ErrorType.PROTOCOL_ERROR -> "协议不匹配"
        ErrorType.UNKNOWN -> error.message
    }
}

