package com.deivid.telegramvideo.util

import android.content.Context
import com.deivid.telegramvideo.data.model.MovieItem
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupManager(private val context: Context, private val gson: Gson) {

    fun createBackupZip(movies: List<MovieItem>): File? {
        return try {
            val json = gson.toJson(movies)
            val jsonFile = File(context.cacheDir, "library_backup.json")
            jsonFile.writeText(json)

            val zipFile = File(context.cacheDir, "video_mode_backup.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                val entry = ZipEntry(jsonFile.name)
                zos.putNextEntry(entry)
                jsonFile.inputStream().copyTo(zos)
                zos.closeEntry()
            }
            zipFile
        } catch (e: Exception) {
            null
        }
    }

    fun extractJsonFromZip(zipFile: File): String? {
        return try {
            java.util.zip.ZipFile(zipFile).use { zip ->
                val entry = zip.getEntry("library_backup.json")
                zip.getInputStream(entry).bufferedReader().readText()
            }
        } catch (e: Exception) {
            null
        }
    }
}
