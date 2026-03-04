package com.deivid.telegramvideo.data.player

import androidx.media3.datasource.DataSource
import com.deivid.telegramvideo.data.repository.TelegramClient

/**
 * Factory para criar instâncias de TdLibDataSource.
 */
class TdLibDataSourceFactory(
    private val telegramClient: TelegramClient,
    private val fileId: Int,
    private val fileSize: Long
) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return TdLibDataSource(telegramClient, fileId, fileSize)
    }
}
