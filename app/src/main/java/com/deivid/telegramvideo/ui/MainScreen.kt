package com.deivid.telegramvideo.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.deivid.telegramvideo.data.model.VideoItem
import com.deivid.telegramvideo.ui.chats.ChatsScreen
import com.deivid.telegramvideo.ui.login.CodeScreen
import com.deivid.telegramvideo.ui.login.LoginScreen
import com.deivid.telegramvideo.ui.login.PasswordScreen
import com.deivid.telegramvideo.ui.player.PlayerScreen
import com.deivid.telegramvideo.ui.setup.SetupScreen
import com.deivid.telegramvideo.ui.videos.VideosScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "setup") {
        composable("setup") {
            SetupScreen(
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("setup") { inclusive = true }
                    }
                }
            )
        }
        composable("login") {
            LoginScreen(
                onNavigateToCode = { navController.navigate("code") },
                onNavigateToChats = {
                    navController.navigate("chats") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("code") {
            CodeScreen(
                onNavigateToPassword = { navController.navigate("password") },
                onNavigateToChats = {
                    navController.navigate("chats") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("password") {
            PasswordScreen(
                onNavigateToChats = {
                    navController.navigate("chats") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("chats") {
            ChatsScreen(
                onChatClick = { chatId, chatTitle ->
                    navController.navigate("videos/$chatId/$chatTitle")
                },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("chats") { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = "videos/{chatId}/{chatTitle}",
            arguments = listOf(
                navArgument("chatId") { type = NavType.LongType },
                navArgument("chatTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getLong("chatId") ?: 0L
            val chatTitle = backStackEntry.arguments?.getString("chatTitle") ?: ""
            VideosScreen(
                chatId = chatId,
                chatTitle = chatTitle,
                onVideoClick = { video ->
                    // For now, we pass video via savedStateHandle or a shared ViewModel
                    // or just navigate and the screen will handle it.
                    // To simplify, let's assume we pass messageId and chatId
                    navController.currentBackStackEntry?.savedStateHandle?.set("video", video)
                    navController.navigate("player/$chatTitle")
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "player/{chatTitle}",
            arguments = listOf(
                navArgument("chatTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatTitle = backStackEntry.arguments?.getString("chatTitle") ?: ""
            val video = navController.previousBackStackEntry?.savedStateHandle?.get<VideoItem>("video")
            if (video != null) {
                PlayerScreen(
                    videoItem = video,
                    chatTitle = chatTitle,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
