package com.deivid.telegramvideo.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.deivid.telegramvideo.ui.theme.TelegramVideoStreamerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity principal que hospeda o MainScreen com Jetpack Compose.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TelegramVideoStreamerTheme {
                MainScreen()
            }
        }
    }
}
