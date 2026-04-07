package com.sting.openclawchat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sting.openclawchat.ui.screens.chat.ChatScreen
import com.sting.openclawchat.ui.screens.setup.SetupScreen

object Routes {
    const val SETUP = "setup"
    const val CHAT = "chat"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SETUP
    ) {
        composable(Routes.SETUP) {
            SetupScreen(
                onNavigateToChat = {
                    navController.navigate(Routes.CHAT) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.CHAT) {
            ChatScreen(
                onNavigateBack = {
                    navController.navigate(Routes.SETUP) {
                        popUpTo(Routes.CHAT) { inclusive = true }
                    }
                }
            )
        }
    }
}
