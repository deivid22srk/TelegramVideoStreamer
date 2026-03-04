package com.deivid.telegramvideo.data.repository

import android.content.Context
import com.deivid.telegramvideo.data.model.MovieData
import com.deivid.telegramvideo.data.model.MovieItem
import com.deivid.telegramvideo.util.BackupManager
import com.google.gson.Gson
import java.io.File
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.TdApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório para gerenciar o "Modo Vídeo" (filmes e séries).
 * Salva e restaura cada item como uma mensagem individual no Telegram.
 */
@Singleton
class VideoModeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telegramClient: TelegramClient,
    private val gson: Gson
) {
    private val backupManager = BackupManager(context, gson)

    suspend fun createAndUploadBackup(): Result<Unit> {
        val chatId = _storageChatId.value
        if (chatId == 0L) return Result.failure(Exception("Nenhum chat de armazenamento definido"))

        val moviesList = _movies.value
        val zipFile = backupManager.createBackupZip(moviesList) ?: return Result.failure(Exception("Falha ao criar ZIP"))

        return telegramClient.sendDocument(chatId, zipFile.absolutePath, "VIDEO_MODE_BACKUP_ZIP")
            .map { Unit }
    }

    suspend fun restoreFromBackupZip(fileId: Int): Result<Unit> {
        val chatId = _storageChatId.value
        if (chatId == 0L) return Result.failure(Exception("Nenhum chat de armazenamento definido"))

        try {
            telegramClient.downloadFile(fileId).collect { file ->
                if (file.local.isDownloadingCompleted && file.local.path.isNotEmpty()) {
                    val zipFile = File(file.local.path)
                    val extractedJson = backupManager.extractJsonFromZip(zipFile)
                    if (extractedJson != null) {
                        val movies = gson.fromJson(extractedJson, Array<MovieItem>::class.java).toList()
                        _movies.value = movies
                    }
                }
            }
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    private val prefs = context.getSharedPreferences("video_mode_prefs", Context.MODE_PRIVATE)

    private val _storageChatId = MutableStateFlow(prefs.getLong("storage_chat_id", 0L))
    val storageChatId: StateFlow<Long> = _storageChatId.asStateFlow()

    private val _movies = MutableStateFlow<List<MovieItem>>(emptyList())
    val movies: StateFlow<List<MovieItem>> = _movies.asStateFlow()

    fun setStorageChat(chatId: Long) {
        _storageChatId.value = chatId
        prefs.edit().putLong("storage_chat_id", chatId).apply()
    }

    suspend fun addMovie(movie: MovieItem): Result<Unit> {
        val chatId = _storageChatId.value
        if (chatId == 0L) return Result.failure(Exception("Nenhum chat de armazenamento definido"))

        val json = gson.toJson(movie)
        val encodedText = "VIDEO_ITEM_METADATA:$json"

        return telegramClient.sendMessage(chatId, encodedText).map {
            restoreMovies() // Recarrega para obter o messageId correto
            Unit
        }
    }

    suspend fun editMovie(movie: MovieItem): Result<Unit> {
        val chatId = _storageChatId.value
        if (chatId == 0L) return Result.failure(Exception("Nenhum chat de armazenamento definido"))
        if (movie.messageId == 0L) return Result.failure(Exception("ID de mensagem inválido"))

        val json = gson.toJson(movie)
        val encodedText = "VIDEO_ITEM_METADATA:$json"

        return telegramClient.editMessageText(chatId, movie.messageId, encodedText).map {
            restoreMovies()
            Unit
        }
    }

    suspend fun deleteMovie(movie: MovieItem): Result<Unit> {
        val chatId = _storageChatId.value
        if (chatId == 0L) return Result.failure(Exception("Nenhum chat de armazenamento definido"))
        if (movie.messageId == 0L) return Result.failure(Exception("ID de mensagem inválido"))

        return telegramClient.deleteMessages(chatId, longArrayOf(movie.messageId), true).map {
            restoreMovies()
            Unit
        }
    }

    suspend fun restoreMovies(): Result<List<MovieItem>> {
        val chatId = _storageChatId.value
        if (chatId == 0L) return Result.failure(Exception("Nenhum chat de armazenamento definido"))

        // Primeiro, tentamos restaurar a partir do ZIP mais recente
        val backupResult = restoreFromLatestBackup()
        if (backupResult.isSuccess) {
            return Result.success(_movies.value)
        }

        // Se não houver backup ZIP ou falhar, caímos para a restauração por mensagens individuais
        return telegramClient.getChatHistory(chatId, limit = 100).map { messages ->
            val items = messages.mapNotNull { message ->
                val content = message.content
                if (content is TdApi.MessageText && content.text.text.startsWith("VIDEO_ITEM_METADATA:")) {
                    val json = content.text.text.removePrefix("VIDEO_ITEM_METADATA:")
                    try {
                        gson.fromJson(json, MovieItem::class.java).copy(messageId = message.id)
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            }
            _movies.value = items
            items
        }
    }

    private suspend fun restoreFromLatestBackup(): Result<Unit> {
        val chatId = _storageChatId.value
        if (chatId == 0L) return Result.failure(Exception("Nenhum chat de armazenamento definido"))

        return telegramClient.getChatHistory(chatId, limit = 50).map { messages ->
            val latestBackup = messages.find { message ->
                val content = message.content
                content is TdApi.MessageDocument && content.caption.text == "VIDEO_MODE_BACKUP_ZIP"
            }

            if (latestBackup != null) {
                val doc = (latestBackup.content as TdApi.MessageDocument).document
                restoreFromBackupZip(doc.document.id).getOrThrow()
            } else {
                throw Exception("Nenhum backup ZIP encontrado")
            }
        }
    }
}
