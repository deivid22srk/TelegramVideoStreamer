package com.deivid.telegramvideo.ui.videomode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deivid.telegramvideo.data.model.EpisodeItem
import com.deivid.telegramvideo.data.model.MovieItem
import com.deivid.telegramvideo.data.model.VideoItem
import com.deivid.telegramvideo.data.model.VideoModeType
import com.deivid.telegramvideo.data.repository.VideoModeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estados da UI para a tela do Modo Vídeo.
 */
sealed class VideoModeUiState {
    object Loading : VideoModeUiState()
    data class Success(
        val movies: List<MovieItem>,
        val series: List<MovieItem>,
        val storageChatTitle: String
    ) : VideoModeUiState()
    data class Error(val message: String) : VideoModeUiState()
    object Empty : VideoModeUiState()
}

/**
 * Estados para operações de salvar/restaurar.
 */
sealed class VideoModeSyncState {
    object Idle : VideoModeSyncState()
    object Loading : VideoModeSyncState()
    data class Success(val message: String) : VideoModeSyncState()
    data class Error(val message: String) : VideoModeSyncState()
}

/**
 * ViewModel responsável pelo Modo Vídeo (filmes e séries).
 */
@HiltViewModel
class VideoModeViewModel @Inject constructor(
    private val videoModeRepository: VideoModeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<VideoModeUiState>(VideoModeUiState.Loading)
    val uiState: StateFlow<VideoModeUiState> = _uiState.asStateFlow()

    private val _syncState = MutableStateFlow<VideoModeSyncState>(VideoModeSyncState.Idle)
    val syncState: StateFlow<VideoModeSyncState> = _syncState.asStateFlow()

    init {
        observeMovies()
    }

    private fun observeMovies() {
        viewModelScope.launch {
            combine(
                videoModeRepository.movies,
                videoModeRepository.storageChatTitle
            ) { movies, chatTitle ->
                Pair(movies, chatTitle)
            }.collect { (movies, chatTitle) ->
                if (movies.isEmpty()) {
                    _uiState.value = VideoModeUiState.Empty
                } else {
                    _uiState.value = VideoModeUiState.Success(
                        movies = movies.filter { it.type == VideoModeType.MOVIE },
                        series = movies.filter { it.type == VideoModeType.SERIES },
                        storageChatTitle = chatTitle
                    )
                }
            }
        }
    }

    /**
     * Adiciona um vídeo como filme ao Modo Vídeo.
     */
    fun addAsMovie(
        videoItem: VideoItem,
        title: String,
        synopsis: String,
        year: String,
        genre: String
    ) {
        val movie = MovieItem(
            id = videoModeRepository.generateId(),
            type = VideoModeType.MOVIE,
            title = title,
            synopsis = synopsis,
            coverImagePath = videoItem.thumbnailPath,
            coverMessageId = videoItem.messageId,
            coverChatId = videoItem.chatId,
            year = year,
            genre = genre,
            messageId = videoItem.messageId,
            chatId = videoItem.chatId,
            fileId = videoItem.fileId,
            duration = videoItem.duration,
            fileSize = videoItem.fileSize,
            mimeType = videoItem.mimeType
        )
        videoModeRepository.addMovie(movie)
    }

    /**
     * Adiciona um vídeo como episódio de uma série existente ou cria uma nova série.
     */
    fun addAsEpisode(
        videoItem: VideoItem,
        seriesId: String?,
        seriesTitle: String,
        seriesSynopsis: String,
        seriesYear: String,
        seriesGenre: String,
        seasonNumber: Int,
        seasonTitle: String,
        episodeNumber: Int,
        episodeTitle: String
    ) {
        val episode = EpisodeItem(
            episodeNumber = episodeNumber,
            title = episodeTitle,
            messageId = videoItem.messageId,
            chatId = videoItem.chatId,
            fileId = videoItem.fileId,
            duration = videoItem.duration,
            fileSize = videoItem.fileSize,
            mimeType = videoItem.mimeType,
            thumbnailPath = videoItem.thumbnailPath,
            localPath = videoItem.localPath
        )

        if (seriesId != null) {
            // Adiciona a uma série existente
            videoModeRepository.addEpisodeToSeries(seriesId, seasonNumber, seasonTitle, episode)
        } else {
            // Cria uma nova série
            val series = MovieItem(
                id = videoModeRepository.generateId(),
                type = VideoModeType.SERIES,
                title = seriesTitle,
                synopsis = seriesSynopsis,
                coverImagePath = videoItem.thumbnailPath,
                coverMessageId = videoItem.messageId,
                coverChatId = videoItem.chatId,
                year = seriesYear,
                genre = seriesGenre,
                messageId = null,
                chatId = null,
                fileId = null,
                duration = 0,
                fileSize = 0,
                mimeType = ""
            )
            videoModeRepository.addMovie(series)
            videoModeRepository.addEpisodeToSeries(series.id, seasonNumber, seasonTitle, episode)
        }
    }

    /**
     * Remove um item do Modo Vídeo.
     */
    fun removeItem(movieId: String) {
        videoModeRepository.removeMovie(movieId)
    }

    /**
     * Salva os dados do Modo Vídeo no grupo do Telegram selecionado.
     */
    fun saveToTelegram() {
        viewModelScope.launch {
            _syncState.value = VideoModeSyncState.Loading
            val result = videoModeRepository.saveToTelegram()
            _syncState.value = result.fold(
                onSuccess = { VideoModeSyncState.Success("Dados salvos com sucesso no Telegram!") },
                onFailure = { VideoModeSyncState.Error(it.message ?: "Erro ao salvar") }
            )
        }
    }

    /**
     * Restaura os dados do Modo Vídeo a partir de um grupo do Telegram.
     */
    fun restoreFromTelegram(chatId: Long, chatTitle: String) {
        viewModelScope.launch {
            _syncState.value = VideoModeSyncState.Loading
            val result = videoModeRepository.restoreFromTelegram(chatId, chatTitle)
            _syncState.value = result.fold(
                onSuccess = { count -> VideoModeSyncState.Success("$count item(s) restaurado(s) com sucesso!") },
                onFailure = { VideoModeSyncState.Error(it.message ?: "Erro ao restaurar") }
            )
        }
    }

    /**
     * Define o grupo de armazenamento.
     */
    fun setStorageChat(chatId: Long, chatTitle: String) {
        videoModeRepository.setStorageChat(chatId, chatTitle)
    }

    /**
     * Retorna a lista de séries para seleção ao adicionar episódio.
     */
    fun getSeriesList(): List<MovieItem> = videoModeRepository.getSeries()

    /**
     * Reseta o estado de sincronização.
     */
    fun resetSyncState() {
        _syncState.value = VideoModeSyncState.Idle
    }

    val storageChatId: StateFlow<Long?> = videoModeRepository.storageChatId
    val storageChatTitle: StateFlow<String> = videoModeRepository.storageChatTitle
    val movies: StateFlow<List<MovieItem>> = videoModeRepository.movies
}
