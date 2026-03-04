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

    val allMovies = videoModeRepository.movies

    init {
        viewModelScope.launch {
            videoModeRepository.movies.collect { movies ->
                _libraryItems.value = groupMovies(movies)
            }
        }
    }

    private fun groupMovies(movies: List<MovieItem>): List<VideoLibraryItem> {
        val result = mutableListOf<VideoLibraryItem>()

        // Recentemente Adicionados
        val recentlyAdded = movies.sortedByDescending { it.date }.take(10)
        if (recentlyAdded.isNotEmpty()) {
            result.add(VideoLibraryItem.Header("Recentemente Adicionados"))
            result.addAll(recentlyAdded.map { VideoLibraryItem.Movie(it) })
        }

        // Filmes
        val simpleMovies = movies.filter { !it.isSeries }.sortedBy { it.title }
        if (simpleMovies.isNotEmpty()) {
            result.add(VideoLibraryItem.Header("Filmes"))
            result.addAll(simpleMovies.map { VideoLibraryItem.Movie(it) })
        }

        // Séries
        val series = movies.filter { it.isSeries }
            .groupBy { it.seriesTitle ?: "Série Sem Título" }
            .toSortedMap()

        if (series.isNotEmpty()) {
            result.add(VideoLibraryItem.Header("Séries"))
            series.forEach { (title, episodes) ->
                // Aqui podemos escolher mostrar apenas o primeiro episódio de cada série como um card de série
                episodes.firstOrNull()?.let { firstEpisode ->
                    result.add(VideoLibraryItem.Movie(firstEpisode.copy(title = title)))
                }
            }
        }

        return result
    }

    fun deleteMovie(movie: MovieItem) {
        viewModelScope.launch {
            videoModeRepository.deleteMovie(movie)
        }
    }
}
