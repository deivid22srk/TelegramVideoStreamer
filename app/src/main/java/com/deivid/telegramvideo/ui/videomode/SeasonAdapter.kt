package com.deivid.telegramvideo.ui.videomode

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.deivid.telegramvideo.data.model.EpisodeItem
import com.deivid.telegramvideo.data.model.SeasonItem
import com.deivid.telegramvideo.databinding.ItemEpisodeBinding
import com.deivid.telegramvideo.databinding.ItemSeasonBinding

/**
 * Adapter para a lista de temporadas de uma série.
 */
class SeasonAdapter(
    private val onEpisodeClick: (EpisodeItem) -> Unit
) : ListAdapter<SeasonItem, SeasonAdapter.SeasonViewHolder>(SeasonDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeasonViewHolder {
        val binding = ItemSeasonBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SeasonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SeasonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SeasonViewHolder(
        private val binding: ItemSeasonBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(season: SeasonItem) {
            binding.tvSeasonTitle.text = season.title.ifEmpty {
                "Temporada ${season.seasonNumber}"
            }
            val episodeCount = season.episodes.size
            binding.tvEpisodeCount.text = "$episodeCount episódio${if (episodeCount != 1) "s" else ""}"

            val episodeAdapter = EpisodeAdapter(onEpisodeClick)
            binding.recyclerViewEpisodes.layoutManager = LinearLayoutManager(binding.root.context)
            binding.recyclerViewEpisodes.adapter = episodeAdapter
            episodeAdapter.submitList(season.episodes.sortedBy { it.episodeNumber })
        }
    }

    private class SeasonDiffCallback : DiffUtil.ItemCallback<SeasonItem>() {
        override fun areItemsTheSame(oldItem: SeasonItem, newItem: SeasonItem): Boolean =
            oldItem.seasonNumber == newItem.seasonNumber

        override fun areContentsTheSame(oldItem: SeasonItem, newItem: SeasonItem): Boolean =
            oldItem == newItem
    }
}

/**
 * Adapter para a lista de episódios de uma temporada.
 */
class EpisodeAdapter(
    private val onEpisodeClick: (EpisodeItem) -> Unit
) : ListAdapter<EpisodeItem, EpisodeAdapter.EpisodeViewHolder>(EpisodeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val binding = ItemEpisodeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EpisodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EpisodeViewHolder(
        private val binding: ItemEpisodeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(episode: EpisodeItem) {
            binding.tvEpisodeNumber.text = "E${episode.episodeNumber}"
            binding.tvEpisodeTitle.text = episode.title.ifEmpty {
                "Episódio ${episode.episodeNumber}"
            }

            // Duração formatada
            val hours = episode.duration / 3600
            val minutes = (episode.duration % 3600) / 60
            val seconds = episode.duration % 60
            binding.tvEpisodeDuration.text = if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }

            binding.root.setOnClickListener { onEpisodeClick(episode) }
            binding.ivPlay.setOnClickListener { onEpisodeClick(episode) }
        }
    }

    private class EpisodeDiffCallback : DiffUtil.ItemCallback<EpisodeItem>() {
        override fun areItemsTheSame(oldItem: EpisodeItem, newItem: EpisodeItem): Boolean =
            oldItem.episodeNumber == newItem.episodeNumber && oldItem.messageId == newItem.messageId

        override fun areContentsTheSame(oldItem: EpisodeItem, newItem: EpisodeItem): Boolean =
            oldItem == newItem
    }
}
