package com.deivid.telegramvideo.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Representa um filme ou episódio de série no modo Vídeo.
 */
@Parcelize
data class MovieItem(
    val id: String,
    val messageId: Long = 0,
    val title: String,
    val synopsis: String,
    val coverUrl: String?,
    val isSeries: Boolean,
    val seriesId: String? = null,
    val seriesTitle: String?,
    val season: Int?,
    val episode: Int?,
    val remoteFileId: String,
    val fileName: String,
    val duration: Int,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val mimeType: String,
    val caption: String,
    val date: Int
) : Parcelable {

    fun toVideoItem(): VideoItem {
        return VideoItem(
            messageId = 0,
            chatId = 0,
            fileId = 0,
            fileName = fileName,
            duration = duration,
            width = width,
            height = height,
            fileSize = fileSize,
            mimeType = mimeType,
            caption = caption,
            date = date,
            thumbnailPath = null,
            localPath = null,
            isDownloaded = false,
            downloadedSize = 0,
            remoteFileId = remoteFileId
        )
    }
}
