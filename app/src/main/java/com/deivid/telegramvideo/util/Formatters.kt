package com.deivid.telegramvideo.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilitários de formatação para exibição de tamanho de arquivo.
 */
object FileSizeFormatter {
    fun format(bytes: Long): String = when {
        bytes >= 1_073_741_824 -> String.format("%.1f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
        bytes >= 1_024 -> String.format("%.1f KB", bytes / 1_024.0)
        else -> "$bytes B"
    }
}

/**
 * Utilitários de formatação para duração de vídeos.
 */
object DurationFormatter {
    fun format(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format("%02d:%02d:%02d", h, m, s)
        else String.format("%02d:%02d", m, s)
    }
}

/**
 * Utilitários de formatação para datas de mensagens.
 */
object DateFormatter {
    private val todayFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val yearFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val recentFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

    fun format(unixTimestamp: Int): String {
        if (unixTimestamp == 0) return ""
        val date = Date(unixTimestamp * 1000L)
        val now = Date()
        val diffMs = now.time - date.time
        val diffDays = diffMs / (1000 * 60 * 60 * 24)

        return when {
            diffDays < 1 -> todayFormat.format(date)
            diffDays < 365 -> recentFormat.format(date)
            else -> yearFormat.format(date)
        }
    }

    fun formatFull(unixTimestamp: Int): String {
        if (unixTimestamp == 0) return ""
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(unixTimestamp * 1000L))
    }
}
