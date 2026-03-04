package com.deivid.telegramvideo.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Modelo de domínio representando uma mensagem de vídeo do Telegram.
 * Implementa Parcelable para ser passado entre Activities/Fragments.
 */
@Parcelize
data class VideoItem(
    val messageId: Long,
    val chatId: Long,
    val fileId: Int,
    val fileName: String,
    val duration: Int,           // em segundos
    val width: Int,
    val height: Int,
    val fileSize: Long,          // em bytes
    val mimeType: String,
    val caption: String,
    val date: Int,               // timestamp Unix
    val thumbnailPath: String?,
    val localPath: String?,      // caminho local se já baixado
    val isDownloaded: Boolean,
    val downloadedSize: Long
) : Parcelable {

    /**
     * Retorna a duração formatada no padrão MM:SS ou HH:MM:SS.
     */
    fun formattedDuration(): String {
        val hours = duration / 3600
        val minutes = (duration % 3600) / 60
        val seconds = duration % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Retorna o tamanho do arquivo formatado (ex: "45,3 MB").
     */
    fun formattedFileSize(): String {
        return when {
            fileSize >= 1_073_741_824 -> String.format("%.1f GB", fileSize / 1_073_741_824.0)
            fileSize >= 1_048_576 -> String.format("%.1f MB", fileSize / 1_048_576.0)
            fileSize >= 1_024 -> String.format("%.1f KB", fileSize / 1_024.0)
            else -> "$fileSize B"
        }
    }

    /**
     * Retorna a resolução do vídeo formatada (ex: "1920×1080").
     */
    fun formattedResolution(): String = "${width}×${height}"

    /**
     * Percentual de download concluído (0-100).
     */
    fun downloadProgress(): Int {
        return if (fileSize > 0) ((downloadedSize * 100) / fileSize).toInt() else 0
    }
}
