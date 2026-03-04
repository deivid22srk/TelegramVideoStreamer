package com.deivid.telegramvideo.ui.videos

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
import com.deivid.telegramvideo.databinding.ItemMovieBinding
import com.deivid.telegramvideo.util.DurationFormatter

/**
 * Adapter para exibir os filmes/séries no Modo Vídeo com agrupamento.
 */
class MovieAdapter(
    private val onMovieClick: (MovieItem) -> Unit,
    private val onMovieDelete: (MovieItem) -> Unit
) : ListAdapter<VideoLibraryItem, RecyclerView.ViewHolder>(VideoLibraryDiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_MOVIE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is VideoLibraryItem.Header -> TYPE_HEADER
            is VideoLibraryItem.Movie -> TYPE_MOVIE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            HeaderViewHolder(view)
        } else {
            val binding = ItemMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            MovieViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is HeaderViewHolder && item is VideoLibraryItem.Header) {
            holder.bind(item)
        } else if (holder is MovieViewHolder && item is VideoLibraryItem.Movie) {
            holder.bind(item.movie)
        }
    }

    class HeaderViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        fun bind(header: VideoLibraryItem.Header) {
            (itemView as android.widget.TextView).apply {
                text = header.title
                setTypeface(null, android.graphics.Typeface.BOLD)
                textSize = 16f
                setPadding(16, 16, 16, 16)
            }
        }
    }

    inner class MovieViewHolder(private val binding: ItemMovieBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(movie: MovieItem) {
            binding.tvTitle.text = movie.title
            binding.tvSubInfo.text = if (movie.isSeries) {
                "T${movie.season} E${movie.episode} • ${DurationFormatter.format(movie.duration)}"
            } else {
                DurationFormatter.format(movie.duration)
            }

            if (!movie.coverUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(movie.coverUrl)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.ic_video_placeholder)
                    .into(binding.ivPoster)
            } else {
                binding.ivPoster.setImageResource(R.drawable.ic_video_placeholder)
            }

            binding.btnDelete.setOnClickListener { onMovieDelete(movie) }
            binding.root.setOnClickListener { onMovieClick(movie) }
        }
    }

    private class VideoLibraryDiffCallback : DiffUtil.ItemCallback<VideoLibraryItem>() {
        override fun areItemsTheSame(oldItem: VideoLibraryItem, newItem: VideoLibraryItem): Boolean {
            return if (oldItem is VideoLibraryItem.Header && newItem is VideoLibraryItem.Header) {
                oldItem.title == newItem.title
            } else if (oldItem is VideoLibraryItem.Movie && newItem is VideoLibraryItem.Movie) {
                oldItem.movie.id == newItem.movie.id
            } else false
        }

        override fun areContentsTheSame(oldItem: VideoLibraryItem, newItem: VideoLibraryItem): Boolean =
            oldItem == newItem
    }
}
