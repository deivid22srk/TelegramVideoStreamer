package com.deivid.telegramvideo.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Representa uma série na biblioteca do Modo Vídeo.
 */
@Parcelize
data class SeriesItem(
    val id: String,
    val messageId: Long = 0,
    val title: String,
    val synopsis: String,
    val coverUrl: String?,
    val seasons: Int = 1
) : Parcelable
