package com.deivid.telegramvideo.ui.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deivid.telegramvideo.data.model.MovieItem
import com.deivid.telegramvideo.data.model.VideoItem
import com.deivid.telegramvideo.data.repository.TelegramRepository
import com.deivid.telegramvideo.data.repository.VideoModeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
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
    private val repository: TelegramRepository,
    private val videoModeRepository: VideoModeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<VideosUiState>(VideosUiState.Loading)
    val uiState: StateFlow<VideosUiState> = _uiState.asStateFlow()

    private var currentChatId: Long = 0
    private var allVideos: MutableList<VideoItem> = mutableListOf()
    private var lastMessageId: Long = 0
    private var isLoadingMore = false
    private var currentQuery = ""

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
        // Don't load more if we are filtering, to avoid confusion
        if (currentQuery.isNotEmpty()) return

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
                    updateUiWithFilter()
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

    /**
     * Filtra os vídeos por texto.
     */
    fun filterVideos(query: String) {
        currentQuery = query
        updateUiWithFilter()
    }

    private fun updateUiWithFilter() {
        if (currentQuery.isBlank()) {
            if (allVideos.isEmpty() && _uiState.value is VideosUiState.Success) {
                _uiState.value = VideosUiState.Empty
            } else if (allVideos.isNotEmpty()) {
                _uiState.value = VideosUiState.Success(allVideos.toList())
            }
            return
        }

        val filtered = allVideos.filter { video ->
            video.fileName.contains(currentQuery, ignoreCase = true) ||
            video.caption.contains(currentQuery, ignoreCase = true)
        }

        if (filtered.isEmpty()) {
            _uiState.value = VideosUiState.Empty
        } else {
            _uiState.value = VideosUiState.Success(filtered)
        }
    }

    /**
     * Adiciona um vídeo ao Modo Vídeo.
     */
    fun addToVideoMode(movie: MovieItem) {
        viewModelScope.launch {
            videoModeRepository.addMovie(movie)
        }
    }

    /**
     * Obtém o remoteFileId para um vídeo (necessário para o MovieItem).
     */
    suspend fun getRemoteFileId(video: VideoItem): String? {
        return repository.getFile(video.fileId).getOrNull()?.remote?.id
    }
}
