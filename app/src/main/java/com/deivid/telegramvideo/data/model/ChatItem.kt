package com.deivid.telegramvideo.data.model

/**
 * Modelo de domínio representando um chat do Telegram.
 */
data class ChatItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val lastMessage: String,
    val lastMessageDate: Int,
    val unreadCount: Int,
    val photoPath: String?
)
