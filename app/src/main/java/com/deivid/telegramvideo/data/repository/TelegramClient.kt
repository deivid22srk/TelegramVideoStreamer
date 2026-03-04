package com.deivid.telegramvideo.data.repository

import android.content.Context
import android.util.Log
import com.deivid.telegramvideo.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Estados possíveis de autenticação no Telegram.
 */
sealed class AuthState {
    object WaitPhoneNumber : AuthState()
    object WaitCode : AuthState()
    object WaitPassword : AuthState()
    object Authorized : AuthState()
    object LoggingOut : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * Cliente TDLib que gerencia toda a comunicação com a API do Telegram.
 */
@Singleton
class TelegramClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TelegramClient"

        init {
            try {
                System.loadLibrary("tdjni")
                Log.d(TAG, "TDLib native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load TDLib native library: ${e.message}")
            }
        }
    }

    private var API_ID = 0
    private var API_HASH = ""

    private var client: Client? = null

    private val _authState = MutableStateFlow<AuthState>(AuthState.WaitPhoneNumber)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _updates = MutableSharedFlow<TdApi.Object>(extraBufferCapacity = 100)
    val updates = _updates.asSharedFlow()

    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        loadCredentials()
        initializeClient()
    }

    private fun loadCredentials() {
        val prefs = context.getSharedPreferences("telegram_prefs", Context.MODE_PRIVATE)
        API_ID = prefs.getInt("api_id", 0)
        API_HASH = prefs.getString("api_hash", "") ?: ""
    }

    private fun initializeClient() {
        try {
            Client.execute(TdApi.SetLogVerbosityLevel(0))

            client = Client.create(
                { update -> handleUpdate(update) },
                { exception -> Log.e(TAG, "Update exception: ${exception.message}") },
                { exception -> Log.e(TAG, "Default exception: ${exception.message}") }
            )

            _isInitialized.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TDLib client: ${e.message}")
            _authState.value = AuthState.Error("Falha ao inicializar: ${e.message}")
        }
    }

    private fun handleUpdate(update: TdApi.Object) {
        clientScope.launch {
            _updates.emit(update)
        }
        when (update) {
            is TdApi.UpdateAuthorizationState -> handleAuthorizationState(update.authorizationState)
        }
    }

    private fun handleAuthorizationState(authorizationState: TdApi.AuthorizationState) {
        when (authorizationState) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> sendTdlibParameters()
            is TdApi.AuthorizationStateWaitPhoneNumber -> _authState.value = AuthState.WaitPhoneNumber
            is TdApi.AuthorizationStateWaitCode -> _authState.value = AuthState.WaitCode
            is TdApi.AuthorizationStateWaitPassword -> _authState.value = AuthState.WaitPassword
            is TdApi.AuthorizationStateReady -> _authState.value = AuthState.Authorized
            is TdApi.AuthorizationStateLoggingOut -> _authState.value = AuthState.LoggingOut
            is TdApi.AuthorizationStateClosed -> {
                _authState.value = AuthState.WaitPhoneNumber
                initializeClient()
            }
        }
    }

    private fun sendTdlibParameters() {
        val databaseDir = File(context.filesDir, "tdlib").absolutePath
        val filesDir = File(context.filesDir, "tdlib_files").absolutePath
        loadCredentials()

        val parameters = TdApi.SetTdlibParameters().apply {
            this.databaseDirectory = databaseDir
            this.filesDirectory = filesDir
            this.useFileDatabase = true
            this.useChatInfoDatabase = true
            this.useMessageDatabase = true
            this.useSecretChats = false
            this.apiId = API_ID
            this.apiHash = API_HASH
            this.systemLanguageCode = "pt-BR"
            this.deviceModel = android.os.Build.MODEL
            this.systemVersion = android.os.Build.VERSION.RELEASE
            this.applicationVersion = "1.0.0"
        }

        client?.send(parameters) { result ->
            if (result is TdApi.Error) _authState.value = AuthState.Error(result.message)
        }
    }

    suspend fun sendPhoneNumber(phoneNumber: String): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            val settings = TdApi.PhoneNumberAuthenticationSettings(false, false, false, false, false, null, null)
            val request = TdApi.SetAuthenticationPhoneNumber(phoneNumber, settings)
            client?.send(request) { result ->
                if (result is TdApi.Ok) continuation.resume(Result.success(Unit))
                else continuation.resume(Result.failure(Exception(if (result is TdApi.Error) result.message else "Erro desconhecido")))
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    suspend fun checkAuthCode(code: String): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            client?.send(TdApi.CheckAuthenticationCode(code)) { result ->
                if (result is TdApi.Ok) continuation.resume(Result.success(Unit))
                else continuation.resume(Result.failure(Exception(if (result is TdApi.Error) result.message else "Erro desconhecido")))
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    suspend fun checkAuthPassword(password: String): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            client?.send(TdApi.CheckAuthenticationPassword(password)) { result ->
                if (result is TdApi.Ok) continuation.resume(Result.success(Unit))
                else continuation.resume(Result.failure(Exception(if (result is TdApi.Error) result.message else "Erro desconhecido")))
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    suspend fun loadChats(limit: Int = 50): Result<List<TdApi.Chat>> =
        suspendCancellableCoroutine { continuation ->
            client?.send(TdApi.LoadChats(TdApi.ChatListMain(), limit)) { result ->
                if (result is TdApi.Ok || result is TdApi.Error) {
                    client?.send(TdApi.GetChats(TdApi.ChatListMain(), limit)) { chatsResult ->
                        if (chatsResult is TdApi.Chats) {
                            val chats = mutableListOf<TdApi.Chat>()
                            var remaining = chatsResult.chatIds.size
                            if (remaining == 0) { continuation.resume(Result.success(emptyList())); return@send }
                            chatsResult.chatIds.forEach { id ->
                                client?.send(TdApi.GetChat(id)) { chat ->
                                    if (chat is TdApi.Chat) synchronized(chats) { chats.add(chat) }
                                    if (java.util.concurrent.atomic.AtomicInteger(remaining).decrementAndGet() == 0) {
                                        continuation.resume(Result.success(chats.sortedByDescending { it.lastMessage?.date ?: 0 }))
                                    }
                                }
                            }
                        } else continuation.resume(Result.success(emptyList()))
                    }
                } else continuation.resume(Result.failure(Exception("Resposta inesperada")))
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    suspend fun deleteMessages(chatId: Long, messageIds: LongArray, revoke: Boolean = true): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            client?.send(TdApi.DeleteMessages(chatId, messageIds, revoke)) { result ->
                if (result is TdApi.Ok) continuation.resume(Result.success(Unit))
                else continuation.resume(Result.failure(Exception(if (result is TdApi.Error) result.message else "Erro ao deletar")))
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    suspend fun getVideos(chatId: Long, fromMessageId: Long = 0, limit: Int = 20): Result<List<TdApi.Message>> =
        suspendCancellableCoroutine { continuation ->
            val filter = TdApi.SearchMessagesFilterVideo()
            val request = TdApi.SearchChatMessages(chatId, null, "", null, fromMessageId, 0, limit, filter)
            client?.send(request) { result ->
                if (result is TdApi.FoundChatMessages) continuation.resume(Result.success(result.messages.toList()))
                else continuation.resume(Result.failure(Exception(if (result is TdApi.Error) result.message else "Erro ao buscar")))
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    suspend fun getFile(fileId: Int): Result<TdApi.File> =
        suspendCancellableCoroutine { continuation ->
            client?.send(TdApi.GetFile(fileId)) { result ->
                if (result is TdApi.File) continuation.resume(Result.success(result))
                else continuation.resume(Result.failure(Exception(if (result is TdApi.Error) result.message else "Erro ao obter arquivo")))
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    suspend fun getRemoteFile(remoteFileId: String): Result<TdApi.File> =
        suspendCancellableCoroutine { continuation ->
            client?.send(TdApi.GetRemoteFile(remoteFileId, TdApi.FileTypeVideo())) { result ->
                if (result is TdApi.File) continuation.resume(Result.success(result))
                else continuation.resume(Result.failure(Exception(if (result is TdApi.Error) result.message else "Erro ao obter arquivo remoto")))
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    fun downloadFile(fileId: Int, priority: Int = 1): Flow<TdApi.File> =
        updates.filterIsInstance<TdApi.UpdateFile>()
            .filter { it.file.id == fileId }
            .map { it.file }
            .onStart {
                getFile(fileId).getOrNull()?.let { emit(it) }
                client?.send(TdApi.DownloadFile(fileId, priority, 0, 0, false)) {}
            }

    suspend fun getChatHistory(chatId: Long, fromMessageId: Long = 0, offset: Int = 0, limit: Int = 20, onlyLocal: Boolean = false): Result<List<TdApi.Message>> =
        suspendCancellableCoroutine { continuation ->
            client?.send(TdApi.GetChatHistory(chatId, fromMessageId, offset, limit, onlyLocal)) { result ->
                if (result is TdApi.Messages) continuation.resume(Result.success(result.messages.toList()))
                else continuation.resume(Result.failure(Exception(if (result is TdApi.Error) result.message else "Erro ao obter histórico")))
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    suspend fun sendMessage(chatId: Long, text: String): Result<TdApi.Message> =
        suspendCancellableCoroutine { continuation ->
            val content = TdApi.InputMessageText(TdApi.FormattedText(text, null), null, false)
            client?.send(TdApi.SendMessage(chatId, null, null, null, null, content)) { result ->
                if (result is TdApi.Message) continuation.resume(Result.success(result))
                else continuation.resume(Result.failure(Exception(if (result is TdApi.Error) result.message else "Erro ao enviar")))
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    suspend fun logout(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        client?.send(TdApi.LogOut()) { result ->
            if (result is TdApi.Ok) continuation.resume(Result.success(Unit))
            else continuation.resume(Result.failure(Exception(if (result is TdApi.Error) result.message else "Erro no logout")))
        } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
    }
}
