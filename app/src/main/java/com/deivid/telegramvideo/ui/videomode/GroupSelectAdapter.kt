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
import com.deivid.telegramvideo.data.model.ChatItem
import com.deivid.telegramvideo.databinding.ItemGroupSelectBinding
import java.io.File

/**
 * Adapter para a lista de grupos na seleção de grupo de armazenamento.
 */
class GroupSelectAdapter(
    private val onGroupClick: (ChatItem) -> Unit
) : ListAdapter<ChatItem, GroupSelectAdapter.GroupViewHolder>(GroupDiffCallback()) {

    private var fullList: List<ChatItem> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemGroupSelectBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun submitFullList(list: List<ChatItem>) {
        fullList = list
        submitList(list)
    }

    fun filter(query: String) {
        val filtered = if (query.isEmpty()) {
            fullList
        } else {
            fullList.filter { it.title.contains(query, ignoreCase = true) }
        }
        submitList(filtered)
    }

    inner class GroupViewHolder(
        private val binding: ItemGroupSelectBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: ChatItem) {
            binding.tvGroupName.text = chat.title
            binding.tvGroupSubtitle.text = chat.subtitle

            if (!chat.photoPath.isNullOrEmpty()) {
                binding.tvGroupInitial.isVisible = false
                Glide.with(binding.root.context)
                    .load(File(chat.photoPath))
                    .circleCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.ic_chat_placeholder)
                    .into(binding.ivGroupPhoto)
            } else {
                binding.ivGroupPhoto.setImageResource(R.drawable.ic_chat_placeholder)
                binding.tvGroupInitial.isVisible = true
                binding.tvGroupInitial.text = chat.title.firstOrNull()?.uppercase() ?: "?"
            }

            binding.root.setOnClickListener { onGroupClick(chat) }
        }
    }

    private class GroupDiffCallback : DiffUtil.ItemCallback<ChatItem>() {
        override fun areItemsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean =
            oldItem == newItem
    }
}
