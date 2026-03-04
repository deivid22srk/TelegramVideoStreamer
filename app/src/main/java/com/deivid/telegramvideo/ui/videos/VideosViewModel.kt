package com.deivid.telegramvideo.ui.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deivid.telegramvideo.data.model.VideoItem
import com.deivid.telegramvideo.data.repository.TelegramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estados da UI para a tela de vídeos.
 */
sealed class VideosUiState {
    object Loading : VideosUiState()
    data class Success(val videos: List<VideoItem>) : VideosUiState()
    data class Error(val message: String) : VideosUiState()
    object Empty : VideosUiState()
}

/**
 * ViewModel responsável por carregar os vídeos de um chat específico.
 */
@HiltViewModel
class VideosViewModel @Inject constructor(
    private val repository: TelegramRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<VideosUiState>(VideosUiState.Loading)
    val uiState: StateFlow<VideosUiState> = _uiState.asStateFlow()

    private var currentChatId: Long = 0
    private var allVideos: MutableList<VideoItem> = mutableListOf()
    private var lastMessageId: Long = 0
    private var isLoadingMore = false

    /**
     * Carrega os vídeos de um chat específico.
     */
    fun loadVideos(chatId: Long) {
        currentChatId = chatId
        allVideos.clear()
        lastMessageId = 0
        viewModelScope.launch {
            _uiState.value = VideosUiState.Loading
            fetchVideos()
        }
    }

    /**
     * Carrega mais vídeos (paginação).
     */
    fun loadMoreVideos() {
        if (isLoadingMore || lastMessageId == 0L) return
        viewModelScope.launch {
            isLoadingMore = true
            fetchVideos()
            isLoadingMore = false
        }
    }

    private suspend fun fetchVideos() {
        val result = repository.getVideoMessages(currentChatId, lastMessageId)
        result.fold(
            onSuccess = { videos ->
                if (videos.isNotEmpty()) {
                    lastMessageId = videos.last().messageId
                    allVideos.addAll(videos)
                    _uiState.value = VideosUiState.Success(allVideos.toList())
                } else if (allVideos.isEmpty()) {
                    _uiState.value = VideosUiState.Empty
                }
            },
            onFailure = { exception ->
                if (allVideos.isEmpty()) {
                    _uiState.value = VideosUiState.Error(
                        exception.message ?: "Erro ao carregar vídeos"
                    )
                }
            }
        )
    }

    /**
     * Recarrega os vídeos do chat atual.
     */
    fun refresh() {
        loadVideos(currentChatId)
    }
}
