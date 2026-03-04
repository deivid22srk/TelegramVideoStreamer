package com.deivid.telegramvideo.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deivid.telegramvideo.data.model.ChatItem
import com.deivid.telegramvideo.data.repository.TelegramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estados da UI para a tela de chats.
 */
sealed class ChatsUiState {
    object Loading : ChatsUiState()
    data class Success(val chats: List<ChatItem>) : ChatsUiState()
    data class Error(val message: String) : ChatsUiState()
    object LoggedOut : ChatsUiState()
}

/**
 * ViewModel responsável por carregar e filtrar a lista de chats.
 */
@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val repository: TelegramRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatsUiState>(ChatsUiState.Loading)
    val uiState: StateFlow<ChatsUiState> = _uiState.asStateFlow()

    private var allChats: List<ChatItem> = emptyList()

    init {
        loadChats()
    }

    /**
     * Carrega a lista de chats do usuário.
     */
    fun loadChats() {
        viewModelScope.launch {
            _uiState.value = ChatsUiState.Loading
            val result = repository.getChats()
            result.fold(
                onSuccess = { chats ->
                    allChats = chats
                    _uiState.value = ChatsUiState.Success(chats)
                },
                onFailure = { exception ->
                    _uiState.value = ChatsUiState.Error(
                        exception.message ?: "Erro ao carregar conversas"
                    )
                }
            )
        }
    }

    /**
     * Filtra os chats pelo texto de pesquisa.
     */
    fun searchChats(query: String) {
        if (query.isBlank()) {
            _uiState.value = ChatsUiState.Success(allChats)
            return
        }

        val filtered = allChats.filter { chat ->
            chat.title.contains(query, ignoreCase = true) ||
            chat.lastMessage.contains(query, ignoreCase = true)
        }
        _uiState.value = ChatsUiState.Success(filtered)
    }

    /**
     * Faz logout do usuário.
     */
    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = ChatsUiState.LoggedOut
        }
    }
}
