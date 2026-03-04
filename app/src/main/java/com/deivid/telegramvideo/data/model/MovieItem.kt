package com.deivid.telegramvideo.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Tipo de conteúdo no Modo Vídeo.
 */
enum class VideoModeType {
    MOVIE,
    SERIES
}

/**
 * Representa um episódio de uma série.
 */
@Parcelize
data class EpisodeItem(
    val episodeNumber: Int,
    val title: String,
    val messageId: Long,
    val chatId: Long,
    val fileId: Int,
    val duration: Int,
    val fileSize: Long,
    val mimeType: String,
    val thumbnailPath: String? = null,
    val localPath: String? = null
) : Parcelable

/**
 * Representa uma temporada de uma série.
 */
@Parcelize
data class SeasonItem(
    val seasonNumber: Int,
    val title: String,
    val episodes: List<EpisodeItem> = emptyList()
) : Parcelable

/**
 * Representa um filme ou série no Modo Vídeo.
 */
@Parcelize
data class MovieItem(
    val id: String,                      // UUID gerado localmente
    val type: VideoModeType,
    val title: String,
    val synopsis: String,
    val coverImagePath: String?,         // caminho local da capa (thumbnail do vídeo ou customizada)
    val coverMessageId: Long?,           // messageId da mensagem com a capa no Telegram
    val coverChatId: Long?,
    val year: String,
    val genre: String,
    // Para filmes: referência direta ao vídeo
    val messageId: Long?,
    val chatId: Long?,
    val fileId: Int?,
    val duration: Int,
    val fileSize: Long,
    val mimeType: String,
    // Para séries: lista de temporadas
    val seasons: List<SeasonItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable
