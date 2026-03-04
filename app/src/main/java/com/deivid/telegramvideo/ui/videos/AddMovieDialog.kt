package com.deivid.telegramvideo.ui.videos

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.deivid.telegramvideo.data.model.MovieItem
import com.deivid.telegramvideo.data.model.VideoItem
import com.deivid.telegramvideo.databinding.DialogAddMovieBinding
import java.util.UUID

/**
 * Dialog para coletar metadados ao adicionar um vídeo ao Modo Vídeo.
 */
object AddMovieDialog {

    fun show(
        context: Context,
        video: VideoItem?,
        remoteFileId: String?,
        existingMovie: MovieItem? = null,
        onSelectVideo: () -> Unit = {},
        onConfirm: (MovieItem) -> Unit
    ) {
        val binding = DialogAddMovieBinding.inflate(LayoutInflater.from(context))

        var currentVideo = video
        var currentRemoteFileId = remoteFileId

        fun updateVideoUi() {
            if (currentVideo != null) {
                binding.tvSelectedVideo.isVisible = true
                binding.tvSelectedVideo.text = "Vídeo: ${currentVideo?.fileName}"
                if (binding.etTitle.text.isNullOrEmpty()) {
                    binding.etTitle.setText(currentVideo?.fileName)
                }
            } else {
                binding.tvSelectedVideo.isVisible = false
            }
        }

        if (existingMovie != null) {
            binding.etTitle.setText(existingMovie.title)
            binding.etSynopsis.setText(existingMovie.synopsis)
            binding.etCoverUrl.setText(existingMovie.coverUrl)
            binding.cbIsSeries.isChecked = existingMovie.isSeries
            binding.layoutSeriesInfo.isVisible = existingMovie.isSeries
            binding.etSeriesTitle.setText(existingMovie.seriesTitle)
            binding.etSeason.setText(existingMovie.season?.toString() ?: "")
            binding.etEpisode.setText(existingMovie.episode?.toString() ?: "")
            updateVideoUi()
        } else if (video != null) {
            binding.etTitle.setText(video.fileName)
            updateVideoUi()
        }

        binding.btnSelectVideo.setOnClickListener {
            onSelectVideo()
        }

        binding.cbIsSeries.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutSeriesInfo.isVisible = isChecked
        }

        AlertDialog.Builder(context)
            .setView(binding.root)
            .setTitle(if (existingMovie != null) "Editar Item" else "Adicionar ao Modo Vídeo")
            .setPositiveButton(if (existingMovie != null) "Salvar" else "Adicionar") { _, _ ->
                val title = binding.etTitle.text.toString()
                val synopsis = binding.etSynopsis.text.toString()
                val coverUrl = binding.etCoverUrl.text.toString().takeIf { it.isNotEmpty() }
                val isSeries = binding.cbIsSeries.isChecked

                val v = currentVideo ?: existingMovie?.toVideoItem()
                val rId = currentRemoteFileId ?: existingMovie?.remoteFileId

                if (v == null || rId == null) {
                    return@setPositiveButton
                }

                val movie = MovieItem(
                    id = existingMovie?.id ?: UUID.randomUUID().toString(),
                    messageId = existingMovie?.messageId ?: 0L,
                    title = title,
                    synopsis = synopsis,
                    coverUrl = coverUrl,
                    isSeries = isSeries,
                    seriesTitle = binding.etSeriesTitle.text.toString().takeIf { isSeries },
                    season = binding.etSeason.text.toString().toIntOrNull().takeIf { isSeries },
                    episode = binding.etEpisode.text.toString().toIntOrNull().takeIf { isSeries },
                    remoteFileId = rId,
                    fileName = v.fileName,
                    duration = v.duration,
                    width = v.width,
                    height = v.height,
                    fileSize = v.fileSize,
                    mimeType = v.mimeType,
                    caption = v.caption,
                    date = v.date
                )
                onConfirm(movie)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
