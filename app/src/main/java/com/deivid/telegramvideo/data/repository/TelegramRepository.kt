package com.deivid.telegramvideo.data.repository

import com.deivid.telegramvideo.data.model.ChatItem
import com.deivid.telegramvideo.data.model.VideoItem
import com.deivid.telegramvideo.util.FileSizeFormatter
import com.deivid.telegramvideo.util.DurationFormatter
import kotlinx.coroutines.flow.StateFlow
import org.drinkless.tdlib.TdApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório principal que abstrai o acesso aos dados do Telegram.
 * Converte os objetos TDLib em modelos de domínio do app.
 */
@Singleton
class TelegramRepository @Inject constructor(
    private val telegramClient: TelegramClient
) {
    val authState: StateFlow<AuthState> = telegramClient.authState
    val isInitialized: StateFlow<Boolean> = telegramClient.isInitialized

    /**
     * Envia o número de telefone para autenticação.
     */
    suspend fun sendPhoneNumber(phoneNumber: String): Result<Unit> =
        telegramClient.sendPhoneNumber(phoneNumber)

    /**
     * Verifica o código de autenticação.
     */
    suspend fun checkAuthCode(code: String): Result<Unit> =
        telegramClient.checkAuthCode(code)

    /**
     * Verifica a senha de dois fatores.
     */
    suspend fun checkAuthPassword(password: String): Result<Unit> =
        telegramClient.checkAuthPassword(password)

    /**
     * Carrega e retorna a lista de chats do usuário formatada.
     */
    suspend fun getChats(): Result<List<ChatItem>> {
        return telegramClient.loadChats().map { chats ->
            chats.map { chat -> chat.toChatItem() }
        }
    }

    /**
     * Carrega os vídeos de um chat específico.
     */
    suspend fun getVideoMessages(
        chatId: Long,
        fromMessageId: Long = 0
    ): Result<List<VideoItem>> {
        return telegramClient.getVideos(chatId, fromMessageId).map { messages ->
            messages.mapNotNull { message ->
                (message.content as? TdApi.MessageVideo)?.let { videoContent ->
                    message.toVideoItem(videoContent)
                }
            }
        }
    }

    /**
     * Obtém informações de um arquivo.
     */
    suspend fun getFile(fileId: Int): Result<TdApi.File> =
        telegramClient.getFile(fileId)

    /**
     * Inicia o download de um arquivo para streaming.
     */
    suspend fun downloadFile(fileId: Int) =
        telegramClient.downloadFile(fileId)

    /**
     * Faz logout do usuário.
     */
    suspend fun logout(): Result<Unit> = telegramClient.logout()

    /**
     * Verifica se o usuário está autenticado.
     */
    fun isAuthorized(): Boolean = telegramClient.authState.value == AuthState.Authorized

    // ---- Funções de conversão (extensões privadas) ----

    private fun TdApi.Chat.toChatItem(): ChatItem {
        val subtitle = when (val type = this.type) {
            is TdApi.ChatTypePrivate -> "Conversa privada"
            is TdApi.ChatTypeBasicGroup -> "${type.basicGroupId} membros"
            is TdApi.ChatTypeSupergroup -> if (type.isChannel) "Canal" else "Grupo"
            is TdApi.ChatTypeSecret -> "Chat secreto"
            else -> ""
        }

        val lastMessageText = when (val content = this.lastMessage?.content) {
            is TdApi.MessageText -> content.text.text
            is TdApi.MessageVideo -> "🎥 Vídeo"
            is TdApi.MessagePhoto -> "📷 Foto"
            is TdApi.MessageDocument -> "📄 Documento"
            is TdApi.MessageAudio -> "🎵 Áudio"
            is TdApi.MessageVoiceNote -> "🎤 Mensagem de voz"
            is TdApi.MessageSticker -> "Sticker"
            else -> ""
        }

        return ChatItem(
            id = this.id,
            title = this.title,
            subtitle = subtitle,
            lastMessage = lastMessageText,
            lastMessageDate = this.lastMessage?.date ?: 0,
            unreadCount = this.unreadCount,
            photoPath = this.photo?.small?.local?.path
        )
    }

    private fun TdApi.Message.toVideoItem(videoContent: TdApi.MessageVideo): VideoItem {
        val video = videoContent.video
        return VideoItem(
            messageId = this.id,
            chatId = this.chatId,
            fileId = video.video.id,
            fileName = video.fileName.ifEmpty { "video_${this.id}.mp4" },
            duration = video.duration,
            width = video.width,
            height = video.height,
            fileSize = video.video.size,
            mimeType = video.mimeType,
            caption = videoContent.caption.text,
            date = this.date,
            thumbnailPath = video.thumbnail?.file?.local?.path,
            localPath = video.video.local?.path?.takeIf { it.isNotEmpty() },
            isDownloaded = video.video.local?.isDownloadingCompleted ?: false,
            downloadedSize = video.video.local?.downloadedSize ?: 0
        )
    }
}
