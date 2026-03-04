package com.deivid.telegramvideo.ui.videos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.deivid.telegramvideo.R
import com.deivid.telegramvideo.data.model.MovieItem
import com.deivid.telegramvideo.databinding.ItemHorizontalCategoryBinding
import com.deivid.telegramvideo.databinding.ItemVideoBinding
import com.deivid.telegramvideo.util.DurationFormatter

sealed class LibraryDisplayItem {
    data class Category(val title: String, val movies: List<MovieItem>) : LibraryDisplayItem()
}

class CategoryAdapter(
    private val onMovieClick: (MovieItem) -> Unit,
    private val onMovieDelete: (MovieItem) -> Unit,
    private val onMovieEdit: (MovieItem) -> Unit
) : ListAdapter<LibraryDisplayItem.Category, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemHorizontalCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(private val binding: ItemHorizontalCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: LibraryDisplayItem.Category) {
            binding.tvCategoryTitle.text = category.title
            val movieAdapter = MovieAdapter(onMovieClick, onMovieDelete, onMovieEdit)

            binding.rvHorizontalMovies.layoutManager = LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
            binding.rvHorizontalMovies.adapter = movieAdapter

            // Transformamos MovieItem em VideoLibraryItem.Movie para o adapter existente
            val libraryItems = category.movies.map { VideoLibraryItem.Movie(it) }
            movieAdapter.submitList(libraryItems)

            binding.rvHorizontalMovies.setHasFixedSize(true)
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<LibraryDisplayItem.Category>() {
        override fun areItemsTheSame(oldItem: LibraryDisplayItem.Category, newItem: LibraryDisplayItem.Category): Boolean =
            oldItem.title == newItem.title
        override fun areContentsTheSame(oldItem: LibraryDisplayItem.Category, newItem: LibraryDisplayItem.Category): Boolean =
            oldItem == newItem
    }
}
