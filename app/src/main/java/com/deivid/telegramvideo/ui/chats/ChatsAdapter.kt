package com.deivid.telegramvideo.ui.chats

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
import com.deivid.telegramvideo.databinding.ItemChatBinding
import com.deivid.telegramvideo.util.DateFormatter
import java.io.File

/**
 * Adapter para a lista de chats usando ListAdapter com DiffUtil.
 */
class ChatsAdapter(
    private val onChatClick: (ChatItem) -> Unit
) : ListAdapter<ChatItem, ChatsAdapter.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatViewHolder(
        private val binding: ItemChatBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: ChatItem) {
            binding.tvChatTitle.text = chat.title
            binding.tvLastMessage.text = chat.lastMessage.ifEmpty { chat.subtitle }
            binding.tvDate.text = DateFormatter.format(chat.lastMessageDate)

            // Badge de mensagens não lidas
            if (chat.unreadCount > 0) {
                binding.tvUnreadCount.isVisible = true
                binding.tvUnreadCount.text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString()
            } else {
                binding.tvUnreadCount.isVisible = false
            }

            // Foto do chat
            if (!chat.photoPath.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(File(chat.photoPath))
                    .circleCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.ic_chat_placeholder)
                    .error(R.drawable.ic_chat_placeholder)
                    .into(binding.ivChatPhoto)
            } else {
                // Exibe a inicial do nome do chat como avatar
                binding.ivChatPhoto.setImageResource(R.drawable.ic_chat_placeholder)
                binding.tvChatInitial.isVisible = true
                binding.tvChatInitial.text = chat.title.firstOrNull()?.uppercase() ?: "?"
            }

            binding.root.setOnClickListener { onChatClick(chat) }
        }
    }

    private class ChatDiffCallback : DiffUtil.ItemCallback<ChatItem>() {
        override fun areItemsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean =
            oldItem == newItem
    }
}
