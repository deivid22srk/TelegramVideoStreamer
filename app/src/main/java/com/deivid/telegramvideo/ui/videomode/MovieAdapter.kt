package com.deivid.telegramvideo.ui.videomode

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.deivid.telegramvideo.R
import com.deivid.telegramvideo.data.model.MovieItem
import com.deivid.telegramvideo.data.model.VideoModeType
import com.deivid.telegramvideo.databinding.ItemMovieBinding
import java.io.File

/**
 * Adapter para a lista de filmes e séries no Modo Vídeo.
 */
class MovieAdapter(
    private val onItemClick: (MovieItem) -> Unit,
    private val onItemLongClick: (MovieItem) -> Boolean = { false }
) : ListAdapter<MovieItem, MovieAdapter.MovieViewHolder>(MovieDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MovieViewHolder(
        private val binding: ItemMovieBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: MovieItem) {
            // Título
            binding.tvTitle.text = movie.title

            // Tipo
            binding.tvType.text = if (movie.type == VideoModeType.MOVIE) "FILME" else "SÉRIE"

            // Ano
            binding.tvYear.text = movie.year.ifEmpty { "—" }

            // Gênero
            binding.tvGenre.text = movie.genre.ifEmpty { "—" }

            // Indicador de temporadas (apenas para séries)
            if (movie.type == VideoModeType.SERIES) {
                binding.tvSeasons.isVisible = true
                val seasonCount = movie.seasons.size
                binding.tvSeasons.text = "$seasonCount temp."
            } else {
                binding.tvSeasons.isVisible = false
            }

            // Capa
            if (!movie.coverImagePath.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(File(movie.coverImagePath))
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.ic_video_placeholder)
                    .error(R.drawable.ic_video_placeholder)
                    .into(binding.ivCover)
            } else {
                binding.ivCover.setImageResource(R.drawable.ic_video_placeholder)
            }

            binding.root.setOnClickListener { onItemClick(movie) }
            binding.root.setOnLongClickListener { onItemLongClick(movie) }
        }
    }

    private class MovieDiffCallback : DiffUtil.ItemCallback<MovieItem>() {
        override fun areItemsTheSame(oldItem: MovieItem, newItem: MovieItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MovieItem, newItem: MovieItem): Boolean =
            oldItem == newItem
    }
}
