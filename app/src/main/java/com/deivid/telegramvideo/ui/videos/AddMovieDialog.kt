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
        video: VideoItem,
        remoteFileId: String,
        onConfirm: (MovieItem) -> Unit
    ) {
        val binding = DialogAddMovieBinding.inflate(LayoutInflater.from(context))

        binding.etTitle.setText(video.fileName)

        binding.cbIsSeries.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutSeriesInfo.isVisible = isChecked
        }

        AlertDialog.Builder(context)
            .setView(binding.root)
            .setPositiveButton("Adicionar") { _, _ ->
                val title = binding.etTitle.text.toString()
                val synopsis = binding.etSynopsis.text.toString()
                val coverUrl = binding.etCoverUrl.text.toString().takeIf { it.isNotEmpty() }
                val isSeries = binding.cbIsSeries.isChecked

                val movie = MovieItem(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    synopsis = synopsis,
                    coverUrl = coverUrl,
                    isSeries = isSeries,
                    seriesTitle = binding.etSeriesTitle.text.toString().takeIf { isSeries },
                    season = binding.etSeason.text.toString().toIntOrNull().takeIf { isSeries },
                    episode = binding.etEpisode.text.toString().toIntOrNull().takeIf { isSeries },
                    remoteFileId = remoteFileId,
                    fileName = video.fileName,
                    duration = video.duration,
                    width = video.width,
                    height = video.height,
                    fileSize = video.fileSize,
                    mimeType = video.mimeType,
                    caption = video.caption,
                    date = video.date
                )
                onConfirm(movie)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
