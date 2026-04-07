package com.sting.openclawchat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
                    navController.navigate(Routes.CHAT)
                }
            )
        }
        composable(Routes.CHAT) {
            ChatScreen(
                onNavigateBack = {
                    navController.navigate(Routes.SETUP) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                }
            )
        }
    }
}
