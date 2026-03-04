package com.deivid.telegramvideo.data.player

import android.net.Uri
import com.deivid.telegramvideo.data.repository.TelegramClient
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import kotlinx.coroutines.runBlocking
import org.drinkless.tdlib.TdApi
import kotlin.math.min

/**
 * DataSource personalizado para o ExoPlayer que lê arquivos diretamente do TDLib.
 */
class TdLibDataSource(
    private val telegramClient: TelegramClient,
    private val fileId: Int,
    private val fileSize: Long
) : BaseDataSource(true) {

    private var uri: Uri? = null
    private var opened = false
    private var bytesRemaining: Long = 0
    private var currentPosition: Long = 0

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        uri = dataSpec.uri

        currentPosition = dataSpec.position
        val length = if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
            fileSize - currentPosition
        } else {
            dataSpec.length
        }

        bytesRemaining = length
        opened = true
        transferStarted(dataSpec)
        return length
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (bytesRemaining == 0L) {
            return C.RESULT_END_OF_INPUT
        }

        val bytesToRead = min(bytesRemaining, length.toLong()).toInt()

        return try {
            // Usamos runBlocking aqui pois o DataSource do ExoPlayer é síncrono
            val result = runBlocking {
                telegramClient.readFilePart(fileId, currentPosition, bytesToRead.toLong())
            }

            result.fold(
                onSuccess = { obj ->
                    // Attempt to access data via reflection if it's not visible
                    try {
                        val dataField = obj.javaClass.getField("data")
                        val data = dataField.get(obj) as ByteArray
                        if (data.isEmpty()) {
                            0
                        } else {
                            val bytesActuallyRead = data.size
                            System.arraycopy(data, 0, buffer, offset, bytesActuallyRead)
                            currentPosition += bytesActuallyRead.toLong()
                            bytesRemaining -= bytesActuallyRead.toLong()
                            bytesActuallyRead
                        }
                    } catch (e: Exception) {
                        -1
                    }
                },
                onFailure = {
                    -1
                }
            )
        } catch (e: Exception) {
            -1
        }
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        if (opened) {
            opened = false
            transferEnded()
        }
        uri = null
    }
}
