package com.deivid.telegramvideo.ui.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deivid.telegramvideo.data.model.MovieItem
import com.deivid.telegramvideo.data.repository.VideoModeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Representa um grupo de filmes/episódios na biblioteca.
 */
sealed class VideoLibraryItem {
    data class Header(val title: String) : VideoLibraryItem()
    data class Movie(val movie: MovieItem) : VideoLibraryItem()
}

@HiltViewModel
class VideoModeViewModel @Inject constructor(
    private val videoModeRepository: VideoModeRepository
) : ViewModel() {

    private val _libraryItems = MutableStateFlow<List<VideoLibraryItem>>(emptyList())
    val libraryItems: StateFlow<List<VideoLibraryItem>> = _libraryItems.asStateFlow()

    init {
        viewModelScope.launch {
            videoModeRepository.movies.collect { movies ->
                _libraryItems.value = groupMovies(movies)
            }
        }
    }

    private fun groupMovies(movies: List<MovieItem>): List<VideoLibraryItem> {
        val result = mutableListOf<VideoLibraryItem>()

        // Separa filmes de séries
        val simpleMovies = movies.filter { !it.isSeries }.sortedBy { it.title }
        val series = movies.filter { it.isSeries }
            .groupBy { it.seriesTitle ?: "Série Sem Título" }
            .toSortedMap()

        if (simpleMovies.isNotEmpty()) {
            result.add(VideoLibraryItem.Header("Filmes"))
            result.addAll(simpleMovies.map { VideoLibraryItem.Movie(it) })
        }

        series.forEach { (title, episodes) ->
            result.add(VideoLibraryItem.Header(title))
            val seasons = episodes.groupBy { it.season ?: 1 }.toSortedMap()
            seasons.forEach { (season, seasonEpisodes) ->
                result.add(VideoLibraryItem.Header("  Temporada $season"))
                result.addAll(seasonEpisodes.sortedBy { it.episode ?: 0 }.map { VideoLibraryItem.Movie(it) })
            }
        }

        return result
    }

    fun deleteMovie(movie: MovieItem) {
        viewModelScope.launch {
            videoModeRepository.deleteMovie(movie)
        }
    }

    fun updateMovie(movie: MovieItem) {
        viewModelScope.launch {
            videoModeRepository.updateMovie(movie)
        }
    }
}
