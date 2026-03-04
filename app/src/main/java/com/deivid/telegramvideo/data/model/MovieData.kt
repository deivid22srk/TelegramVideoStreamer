package com.deivid.telegramvideo.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Container para uma lista de MovieItems, usado para serialização JSON.
 */
@Parcelize
data class MovieData(
    val movies: List<MovieItem> = emptyList()
) : Parcelable
