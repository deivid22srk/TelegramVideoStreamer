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
import com.deivid.telegramvideo.data.model.VideoItem
import com.deivid.telegramvideo.databinding.ItemVideoBinding
import com.deivid.telegramvideo.util.DateFormatter
import java.io.File

/**
 * Adapter para a grade de vídeos usando ListAdapter com DiffUtil.
 */
class VideosAdapter(
    private val onVideoClick: (VideoItem) -> Unit,
    private val onVideoLongClick: (VideoItem) -> Unit
) : ListAdapter<VideoItem, VideosAdapter.VideoViewHolder>(VideoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VideoViewHolder(
        private val binding: ItemVideoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(video: VideoItem) {
            // Duração do vídeo
            binding.tvDuration.text = video.formattedDuration()

            // Tamanho do arquivo
            binding.tvFileSize.text = video.formattedFileSize()

            // Data da mensagem
            binding.tvDate.text = DateFormatter.format(video.date)

            // Legenda (se houver)
            if (video.caption.isNotEmpty()) {
                binding.tvCaption.isVisible = true
                binding.tvCaption.text = video.caption
            } else {
                binding.tvCaption.isVisible = false
            }

            // Thumbnail do vídeo
            if (!video.thumbnailPath.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(File(video.thumbnailPath))
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.ic_video_placeholder)
                    .error(R.drawable.ic_video_placeholder)
                    .into(binding.ivThumbnail)
            } else {
                binding.ivThumbnail.setImageResource(R.drawable.ic_video_placeholder)
            }

            // Indicador de download
            if (video.isDownloaded) {
                binding.ivDownloaded.isVisible = true
            } else {
                binding.ivDownloaded.isVisible = false
            }

            binding.root.setOnClickListener { onVideoClick(video) }
            binding.root.setOnLongClickListener {
                onVideoLongClick(video)
                true
            }
        }
    }

    private class VideoDiffCallback : DiffUtil.ItemCallback<VideoItem>() {
        override fun areItemsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean =
            oldItem.messageId == newItem.messageId

        override fun areContentsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean =
            oldItem == newItem
    }
}
