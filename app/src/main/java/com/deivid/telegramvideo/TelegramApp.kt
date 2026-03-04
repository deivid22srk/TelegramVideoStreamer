package com.deivid.telegramvideo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Classe Application principal do app.
 * Anotada com @HiltAndroidApp para habilitar injeção de dependências via Hilt.
 */
@HiltAndroidApp
class TelegramApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Inicializações globais podem ser feitas aqui
    }
}
