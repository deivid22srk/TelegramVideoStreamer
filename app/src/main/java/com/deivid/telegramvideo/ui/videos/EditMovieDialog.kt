package com.deivid.telegramvideo.ui.videos

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.deivid.telegramvideo.data.model.MovieItem
import com.deivid.telegramvideo.databinding.DialogAddMovieBinding

/**
 * Dialog para editar metadados de um filme ou série na biblioteca.
 */
object EditMovieDialog {

    fun show(
        context: Context,
        movie: MovieItem,
        onConfirm: (MovieItem) -> Unit
    ) {
        val binding = DialogAddMovieBinding.inflate(LayoutInflater.from(context))

        binding.etTitle.setText(movie.title)
        binding.etSynopsis.setText(movie.synopsis)
        binding.etCoverUrl.setText(movie.coverUrl)
        binding.cbIsSeries.isChecked = movie.isSeries
        binding.layoutSeriesInfo.isVisible = movie.isSeries

        if (movie.isSeries) {
            binding.etSeriesTitle.setText(movie.seriesTitle)
            binding.etSeason.setText(movie.season?.toString())
            binding.etEpisode.setText(movie.episode?.toString())
        }

        binding.cbIsSeries.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutSeriesInfo.isVisible = isChecked
        }

        AlertDialog.Builder(context)
            .setTitle("Editar Detalhes")
            .setView(binding.root)
            .setPositiveButton("Salvar") { _, _ ->
                val title = binding.etTitle.text.toString()
                val synopsis = binding.etSynopsis.text.toString()
                val coverUrl = binding.etCoverUrl.text.toString().takeIf { it.isNotEmpty() }
                val isSeries = binding.cbIsSeries.isChecked

                val updatedMovie = movie.copy(
                    title = title,
                    synopsis = synopsis,
                    coverUrl = coverUrl,
                    isSeries = isSeries,
                    seriesTitle = binding.etSeriesTitle.text.toString().takeIf { isSeries },
                    season = binding.etSeason.text.toString().toIntOrNull().takeIf { isSeries },
                    episode = binding.etEpisode.text.toString().toIntOrNull().takeIf { isSeries }
                )
                onConfirm(updatedMovie)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
