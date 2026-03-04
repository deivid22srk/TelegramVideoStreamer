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
 * Implementa o padrão Singleton via Hilt para garantir uma única instância.
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
        Log.d(TAG, "Credentials loaded: API_ID=$API_ID")
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
        clientScope.launch {
            _updates.emit(update)
        }
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

        // Recarrega credenciais caso tenham sido alteradas
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
                        client?.send(TdApi.GetChats(TdApi.ChatListMain(), limit)) { chatsResult ->
                            if (chatsResult is TdApi.Chats) {
                                // Tenta retornar o que já tem
                                continuation.resume(Result.success(emptyList()))
                            } else {
                                continuation.resume(Result.failure(Exception(result.message)))
                            }
                        }
                    }
                    else -> continuation.resume(Result.failure(Exception("Resposta inesperada")))
                }
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    /**
     * Busca mensagens de vídeo em um chat específico.
     */
    suspend fun getVideos(chatId: Long, fromMessageId: Long = 0, limit: Int = 20): Result<List<TdApi.Message>> =
        suspendCancellableCoroutine { continuation ->
            val filter = TdApi.SearchMessagesFilterVideo()
            val request = TdApi.SearchChatMessages(chatId, null, "", null, fromMessageId, 0, limit, filter)
            client?.send(request) { result ->
                when (result) {
                    is TdApi.FoundChatMessages -> continuation.resume(Result.success(result.messages.toList()))
                    is TdApi.Error -> continuation.resume(Result.failure(Exception(result.message)))
                    else -> continuation.resume(Result.failure(Exception("Resposta inesperada: ${result.javaClass.simpleName}")))
                }
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    /**
     * Obtém informações de um arquivo.
     */
    suspend fun getFile(fileId: Int): Result<TdApi.File> =
        suspendCancellableCoroutine { continuation ->
            client?.send(TdApi.GetFile(fileId)) { result ->
                when (result) {
                    is TdApi.File -> continuation.resume(Result.success(result))
                    is TdApi.Error -> continuation.resume(Result.failure(Exception(result.message)))
                    else -> continuation.resume(Result.failure(Exception("Resposta inesperada")))
                }
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    /**
     * Solicita o download de um arquivo.
     */
    fun downloadFile(fileId: Int, priority: Int = 1): Flow<TdApi.File> =
        updates.filterIsInstance<TdApi.UpdateFile>()
            .filter { it.file.id == fileId }
            .map { it.file }
            .onStart {
                // Emite o estado inicial do arquivo
                getFile(fileId).getOrNull()?.let { emit(it) }

                // Inicia o download
                client?.send(TdApi.DownloadFile(fileId, priority, 0, 0, false)) { result ->
                    if (result is TdApi.Error) {
                        Log.e(TAG, "Error downloading file $fileId: ${result.message}")
                    }
                }
            }

    /**
     * Envia uma mensagem de texto para um chat.
     * Usado para salvar dados do Modo Vídeo em um grupo.
     */
    suspend fun sendTextMessage(chatId: Long, text: String): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            val formattedText = TdApi.FormattedText(text, emptyArray())
            val content = TdApi.InputMessageText(formattedText, null, false)
            val request = TdApi.SendMessage(chatId, 0, null, null, null, content)
            client?.send(request) { result ->
                when (result) {
                    is TdApi.Message -> continuation.resume(Result.success(Unit))
                    is TdApi.Error -> continuation.resume(Result.failure(Exception(result.message)))
                    else -> continuation.resume(Result.failure(Exception("Resposta inesperada")))
                }
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    /**
     * Busca a mensagem de texto mais recente contendo uma determinada string em um chat.
     * Usado para restaurar dados do Modo Vídeo.
     */
    suspend fun searchTextMessage(chatId: Long, query: String): Result<String> =
        suspendCancellableCoroutine { continuation ->
            val filter = TdApi.SearchMessagesFilterEmpty()
            val request = TdApi.SearchChatMessages(chatId, query, "", null, 0, 0, 1, filter)
            client?.send(request) { result ->
                when (result) {
                    is TdApi.FoundChatMessages -> {
                        val message = result.messages.firstOrNull()
                        if (message != null) {
                            val text = (message.content as? TdApi.MessageText)?.text?.text
                            if (text != null) {
                                continuation.resume(Result.success(text))
                            } else {
                                continuation.resume(Result.failure(Exception("Mensagem não contém texto")))
                            }
                        } else {
                            continuation.resume(Result.failure(Exception("Nenhuma mensagem encontrada")))
                        }
                    }
                    is TdApi.Error -> continuation.resume(Result.failure(Exception(result.message)))
                    else -> continuation.resume(Result.failure(Exception("Resposta inesperada")))
                }
            } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
        }

    /**
     * Encerra a sessão do usuário.
     */
    suspend fun logout(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        client?.send(TdApi.LogOut()) { result ->
            when (result) {
                is TdApi.Ok -> continuation.resume(Result.success(Unit))
                is TdApi.Error -> continuation.resume(Result.failure(Exception(result.message)))
                else -> continuation.resume(Result.failure(Exception("Resposta inesperada")))
            }
        } ?: continuation.resumeWithException(Exception("Cliente não inicializado"))
    }
}
