package com.deivid.telegramvideo.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deivid.telegramvideo.data.model.VideoItem
import com.deivid.telegramvideo.data.repository.TelegramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import javax.inject.Inject

/**
 * Estados da UI para o player de vídeo.
 */
sealed class PlayerUiState {
    object Idle : PlayerUiState()
    object Preparing : PlayerUiState()
    data class Ready(val localPath: String) : PlayerUiState()
    data class Downloading(val progress: Int) : PlayerUiState()
    data class Error(val message: String) : PlayerUiState()
}

/**
 * ViewModel responsável por gerenciar o download e reprodução de vídeos.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: TelegramRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Idle)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentVideo: VideoItem? = null
    private var isReadySent = false

    /**
     * Prepara o vídeo para reprodução.
     * Se já estiver baixado, retorna o caminho local.
     * Caso contrário, inicia o download progressivo para streaming.
     */
    fun prepareVideo(video: VideoItem) {
        currentVideo = video
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Preparing

            // Verifica se o arquivo já está disponível localmente
            if (video.isDownloaded && !video.localPath.isNullOrEmpty()) {
                _uiState.value = PlayerUiState.Ready(video.localPath)
                return@launch
            }

            // Obtém informações atualizadas do arquivo
            val fileResult = repository.getFile(video.fileId)
            fileResult.fold(
                onSuccess = { file ->
                    when {
                        file.local.isDownloadingCompleted && file.local.path.isNotEmpty() -> {
                            // Arquivo já baixado
                            _uiState.value = PlayerUiState.Ready(file.local.path)
                        }
                        file.local.isDownloadingActive -> {
                            // Download em andamento - monitora o progresso
                            val progress = if (file.size > 0) {
                                ((file.local.downloadedSize * 100) / file.size).toInt()
                            } else 0
                            _uiState.value = PlayerUiState.Downloading(progress)
                            // Inicia o streaming progressivo
                            startProgressiveDownload(video.fileId)
                        }
                        else -> {
                            // Inicia o download para streaming
                            startProgressiveDownload(video.fileId)
                        }
                    }
                },
                onFailure = { exception ->
                    _uiState.value = PlayerUiState.Error(
                        exception.message ?: "Erro ao preparar vídeo"
                    )
                }
            )
        }
    }

    /**
     * Inicia o download progressivo do arquivo para permitir streaming.
     * O TDLib suporta streaming nativo - o arquivo pode ser reproduzido
     * enquanto ainda está sendo baixado.
     */
    private fun startProgressiveDownload(fileId: Int) {
        isReadySent = false
        viewModelScope.launch {
            try {
                repository.downloadFile(fileId).collect { file ->
                    val progress = if (file.size > 0) {
                        ((file.local.downloadedSize * 100) / file.size).toInt()
                    } else 0

                    when {
                        file.local.isDownloadingCompleted && file.local.path.isNotEmpty() -> {
                            if (!isReadySent) {
                                isReadySent = true
                                _uiState.value = PlayerUiState.Ready(file.local.path)
                            }
                        }
                        file.local.isDownloadingActive -> {
                            // Inicia a reprodução assim que tiver dados iniciais (>= 1%)
                            if (progress >= 1 && file.local.path.isNotEmpty() && !isReadySent) {
                                isReadySent = true
                                _uiState.value = PlayerUiState.Ready(file.local.path)
                            } else if (!isReadySent) {
                                _uiState.value = PlayerUiState.Downloading(progress)
                            }
                        }
                        else -> {
                            if (file.local.path.isNotEmpty() && !isReadySent) {
                                isReadySent = true
                                _uiState.value = PlayerUiState.Ready(file.local.path)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = PlayerUiState.Error(
                    e.message ?: "Erro durante o download do vídeo"
                )
            }
        }
    }

    /**
     * Retenta a preparação do vídeo em caso de erro.
     */
    fun retry() {
        currentVideo?.let { prepareVideo(it) }
    }
}
