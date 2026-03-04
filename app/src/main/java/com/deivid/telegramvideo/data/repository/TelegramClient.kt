package com.deivid.telegramvideo.data.repository

import android.content.Context
import android.util.Log
import com.deivid.telegramvideo.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
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
 * Implementa o padrão Singleton via Hilt para garantir uma única instância.
 */
@Singleton
class TelegramClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TelegramClient"
        // Substitua pelos seus valores obtidos em https://my.telegram.org
        private const val API_ID = 2040   // Use seu próprio API_ID
        private const val API_HASH = "b18441a1ff607e10a989891a5462e627" // Use seu próprio API_HASH
    }

    private var client: Client? = null

    private val _authState = MutableStateFlow<AuthState>(AuthState.WaitPhoneNumber)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    init {
        initializeClient()
    }

    /**
     * Inicializa o cliente TDLib com as configurações necessárias.
     */
    private fun initializeClient() {
        try {
            Client.execute(TdApi.SetLogVerbosityLevel(0))

            client = Client.create(
                { update -> handleUpdate(update) },
                { exception -> Log.e(TAG, "Update exception: ${exception.message}") },
                { exception -> Log.e(TAG, "Default exception: ${exception.message}") }
            )

            _isInitialized.value = true
            Log.d(TAG, "TDLib client initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TDLib client: ${e.message}")
            _authState.value = AuthState.Error("Falha ao inicializar cliente Telegram: ${e.message}")
        }
    }

    /**
     * Processa as atualizações recebidas do TDLib.
     */
    private fun handleUpdate(update: TdApi.Object) {
        when (update) {
            is TdApi.UpdateAuthorizationState -> {
                handleAuthorizationState(update.authorizationState)
            }
            else -> {
                Log.v(TAG, "Received update: ${update.javaClass.simpleName}")
            }
        }
    }

    /**
     * Trata as mudanças de estado de autorização.
     */
    private fun handleAuthorizationState(authorizationState: TdApi.AuthorizationState) {
        when (authorizationState) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                sendTdlibParameters()
            }
            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                _authState.value = AuthState.WaitPhoneNumber
            }
            is TdApi.AuthorizationStateWaitCode -> {
                _authState.value = AuthState.WaitCode
            }
            is TdApi.AuthorizationStateWaitPassword -> {
                _authState.value = AuthState.WaitPassword
            }
            is TdApi.AuthorizationStateReady -> {
                _authState.value = AuthState.Authorized
            }
            is TdApi.AuthorizationStateLoggingOut -> {
                _authState.value = AuthState.LoggingOut
            }
            is TdApi.AuthorizationStateClosed -> {
                _authState.value = AuthState.WaitPhoneNumber
                initializeClient()
            }
            else -> {
                Log.d(TAG, "Unknown auth state: ${authorizationState.javaClass.simpleName}")
            }
        }
    }

    /**
     * Envia os parâmetros de configuração do TDLib.
     */
    private fun sendTdlibParameters() {
        val databaseDir = File(context.filesDir, "tdlib").absolutePath
        val filesDir = File(context.filesDir, "tdlib_files").absolutePath

        val parameters = TdApi.SetTdlibParameters().apply {
            this.databaseDirectory = databaseDir
            this.filesDirectory = filesDir
            this.useMessageDatabase = true
            this.useSecretChats = false
            this.apiId = API_ID
            this.apiHash = API_HASH
            this.systemLanguageCode = "pt-BR"
            this.deviceModel = android.os.Build.MODEL
            this.applicationVersion = "1.0.0"
            this.enableStorageOptimizer = true
        }

        client?.send(parameters) { result ->
            if (result is TdApi.Error) {
                Log.e(TAG, "Error setting TDLib parameters: ${result.message}")
                _authState.value = AuthState.Error(result.message)
            }
        }
    }

    /**
     * Envia o número de telefone para iniciar a autenticação.
     */
    suspend fun sendPhoneNumber(phoneNumber: String): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            val request = TdApi.SetAuthenticationPhoneNumber(
                phoneNumber,
                TdApi.PhoneNumberAuthenticationSettings().apply {
                    allowFlashCall = false
                    isCurrentPhoneNumber = false
                    allowSmsRetrieverApi = false
                }
            )

            client?.send(request) { result ->
                when (result) {
                    is TdApi.Ok -> continuation.resume(Result.success(Unit))
                    is TdApi.Error -> continuation.resume(
                        Result.failure(Exception(result.message))
                    )
                    else -> continuation.resume(
                        Result.failure(Exception("Resposta inesperada"))
                    )
                }
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    /**
     * Verifica o código de autenticação recebido via SMS/Telegram.
     */
    suspend fun checkAuthCode(code: String): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            client?.send(TdApi.CheckAuthenticationCode(code)) { result ->
                when (result) {
                    is TdApi.Ok -> continuation.resume(Result.success(Unit))
                    is TdApi.Error -> continuation.resume(
                        Result.failure(Exception(result.message))
                    )
                    else -> continuation.resume(
                        Result.failure(Exception("Resposta inesperada"))
                    )
                }
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    /**
     * Verifica a senha de dois fatores (2FA).
     */
    suspend fun checkAuthPassword(password: String): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            client?.send(TdApi.CheckAuthenticationPassword(password)) { result ->
                when (result) {
                    is TdApi.Ok -> continuation.resume(Result.success(Unit))
                    is TdApi.Error -> continuation.resume(
                        Result.failure(Exception(result.message))
                    )
                    else -> continuation.resume(
                        Result.failure(Exception("Resposta inesperada"))
                    )
                }
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    /**
     * Carrega a lista de chats do usuário.
     */
    suspend fun loadChats(limit: Int = 50): Result<List<TdApi.Chat>> =
        suspendCancellableCoroutine { continuation ->
            client?.send(TdApi.LoadChats(TdApi.ChatListMain(), limit)) { result ->
                when (result) {
                    is TdApi.Ok -> {
                        // Após LoadChats, busca os chats carregados
                        client?.send(TdApi.GetChats(TdApi.ChatListMain(), limit)) { chatsResult ->
                            when (chatsResult) {
                                is TdApi.Chats -> {
                                    val chatIds = chatsResult.chatIds
                                    val chats = mutableListOf<TdApi.Chat>()
                                    var remaining = chatIds.size

                                    if (remaining == 0) {
                                        continuation.resume(Result.success(emptyList()))
                                        return@send
                                    }

                                    chatIds.forEach { chatId ->
                                        client?.send(TdApi.GetChat(chatId)) { chatResult ->
                                            synchronized(chats) {
                                                if (chatResult is TdApi.Chat) {
                                                    chats.add(chatResult)
                                                }
                                                remaining--
                                                if (remaining == 0) {
                                                    continuation.resume(
                                                        Result.success(chats.sortedByDescending {
                                                            it.lastMessage?.date ?: 0
                                                        })
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                is TdApi.Error -> continuation.resume(
                                    Result.failure(Exception(chatsResult.message))
                                )
                                else -> continuation.resume(Result.success(emptyList()))
                            }
                        }
                    }
                    is TdApi.Error -> {
                        // Tenta buscar os chats mesmo se LoadChats falhar (já podem estar em cache)
                        client?.send(TdApi.GetChats(TdApi.ChatListMain(), limit)) { chatsResult ->
                            when (chatsResult) {
                                is TdApi.Chats -> {
                                    val chatIds = chatsResult.chatIds
                                    val chats = mutableListOf<TdApi.Chat>()
                                    var remaining = chatIds.size

                                    if (remaining == 0) {
                                        continuation.resume(Result.success(emptyList()))
                                        return@send
                                    }

                                    chatIds.forEach { chatId ->
                                        client?.send(TdApi.GetChat(chatId)) { chatResult ->
                                            synchronized(chats) {
                                                if (chatResult is TdApi.Chat) {
                                                    chats.add(chatResult)
                                                }
                                                remaining--
                                                if (remaining == 0) {
                                                    continuation.resume(
                                                        Result.success(chats.sortedByDescending {
                                                            it.lastMessage?.date ?: 0
                                                        })
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> continuation.resume(Result.success(emptyList()))
                            }
                        }
                    }
                    else -> continuation.resume(Result.success(emptyList()))
                }
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    /**
     * Busca mensagens de vídeo de um chat específico.
     */
    suspend fun getVideoMessages(
        chatId: Long,
        fromMessageId: Long = 0,
        limit: Int = 50
    ): Result<List<TdApi.Message>> =
        suspendCancellableCoroutine { continuation ->
            val filter = TdApi.SearchMessagesFilterVideo()
            client?.send(
                TdApi.SearchChatMessages(chatId, "", null, fromMessageId, 0, limit, filter, 0, 0)
            ) { result ->
                when (result) {
                    is TdApi.Messages -> {
                        val videoMessages = result.messages.filter { message ->
                            message.content is TdApi.MessageVideo
                        }
                        continuation.resume(Result.success(videoMessages))
                    }
                    is TdApi.Error -> continuation.resume(
                        Result.failure(Exception(result.message))
                    )
                    else -> continuation.resume(Result.success(emptyList()))
                }
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    /**
     * Faz o download de um arquivo do Telegram para streaming.
     * Retorna o caminho local do arquivo após o download.
     */
    suspend fun downloadFile(fileId: Int, priority: Int = 1): Flow<TdApi.File> = callbackFlow {
        client?.send(TdApi.DownloadFile(fileId, priority, 0, 0, true)) { result ->
            when (result) {
                is TdApi.File -> trySend(result)
                is TdApi.Error -> close(Exception(result.message))
                else -> close(Exception("Erro desconhecido no download"))
            }
        }

        awaitClose {
            // Cancela o download se o flow for cancelado
            client?.send(TdApi.CancelDownloadFile(fileId, false)) {}
        }
    }

    /**
     * Obtém informações de um arquivo pelo ID.
     */
    suspend fun getFile(fileId: Int): Result<TdApi.File> =
        suspendCancellableCoroutine { continuation ->
            client?.send(TdApi.GetFile(fileId)) { result ->
                when (result) {
                    is TdApi.File -> continuation.resume(Result.success(result))
                    is TdApi.Error -> continuation.resume(
                        Result.failure(Exception(result.message))
                    )
                    else -> continuation.resume(
                        Result.failure(Exception("Resposta inesperada"))
                    )
                }
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    /**
     * Obtém a foto de perfil de um chat.
     */
    suspend fun getChatPhoto(chatId: Long): Result<TdApi.File?> =
        suspendCancellableCoroutine { continuation ->
            client?.send(TdApi.GetChat(chatId)) { result ->
                when (result) {
                    is TdApi.Chat -> {
                        val photoFileId = result.photo?.small?.id
                        if (photoFileId != null) {
                            client?.send(TdApi.DownloadFile(photoFileId, 1, 0, 0, true)) { fileResult ->
                                when (fileResult) {
                                    is TdApi.File -> continuation.resume(Result.success(fileResult))
                                    else -> continuation.resume(Result.success(null))
                                }
                            }
                        } else {
                            continuation.resume(Result.success(null))
                        }
                    }
                    is TdApi.Error -> continuation.resume(
                        Result.failure(Exception(result.message))
                    )
                    else -> continuation.resume(Result.success(null))
                }
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    /**
     * Faz logout do usuário.
     */
    suspend fun logout(): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            client?.send(TdApi.LogOut()) { result ->
                when (result) {
                    is TdApi.Ok -> continuation.resume(Result.success(Unit))
                    is TdApi.Error -> continuation.resume(
                        Result.failure(Exception(result.message))
                    )
                    else -> continuation.resume(Result.success(Unit))
                }
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    /**
     * Verifica se o usuário está autenticado.
     */
    fun isAuthorized(): Boolean = _authState.value is AuthState.Authorized

    /**
     * Fecha o cliente TDLib ao destruir a instância.
     */
    fun close() {
        client?.close()
        client = null
    }
}
