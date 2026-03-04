package com.deivid.telegramvideo.data.repository

import android.content.Context
import android.util.Log
import com.deivid.telegramvideo.data.model.EpisodeItem
import com.deivid.telegramvideo.data.model.MovieItem
import com.deivid.telegramvideo.data.model.SeasonItem
import com.deivid.telegramvideo.data.model.VideoModeType
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.TdApi
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tag de identificação para mensagens do Modo Vídeo salvas no Telegram.
 * Usada para localizar e restaurar os dados.
 */
const val VIDEO_MODE_TAG = "#VideoModeData_v1"

/**
 * Repositório responsável por gerenciar os dados do Modo Vídeo.
 * Os dados são armazenados localmente e sincronizados com um grupo do Telegram.
 */
@Singleton
class VideoModeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telegramClient: TelegramClient
) {
    companion object {
        private const val TAG = "VideoModeRepository"
        private const val PREFS_NAME = "video_mode_prefs"
        private const val KEY_MOVIES = "movies_json"
        private const val KEY_STORAGE_CHAT_ID = "storage_chat_id"
        private const val KEY_STORAGE_CHAT_TITLE = "storage_chat_title"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson: Gson = GsonBuilder().create()

    private val _movies = MutableStateFlow<List<MovieItem>>(emptyList())
    val movies: StateFlow<List<MovieItem>> = _movies.asStateFlow()

    private val _storageChatId = MutableStateFlow<Long?>(null)
    val storageChatId: StateFlow<Long?> = _storageChatId.asStateFlow()

    private val _storageChatTitle = MutableStateFlow<String>("")
    val storageChatTitle: StateFlow<String> = _storageChatTitle.asStateFlow()

    init {
        loadLocalData()
    }

    /**
     * Carrega os dados locais do SharedPreferences.
     */
    private fun loadLocalData() {
        val json = prefs.getString(KEY_MOVIES, null)
        if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<MovieItem>>() {}.type
                val list: List<MovieItem> = gson.fromJson(json, type)
                _movies.value = list
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao carregar dados locais: ${e.message}")
                _movies.value = emptyList()
            }
        }
        val chatId = prefs.getLong(KEY_STORAGE_CHAT_ID, 0L)
        if (chatId != 0L) {
            _storageChatId.value = chatId
            _storageChatTitle.value = prefs.getString(KEY_STORAGE_CHAT_TITLE, "") ?: ""
        }
    }

    /**
     * Salva os dados localmente no SharedPreferences.
     */
    private fun saveLocalData(movies: List<MovieItem>) {
        val json = gson.toJson(movies)
        prefs.edit().putString(KEY_MOVIES, json).apply()
        _movies.value = movies
    }

    /**
     * Define o grupo do Telegram onde os dados serão salvos/restaurados.
     */
    fun setStorageChat(chatId: Long, chatTitle: String) {
        _storageChatId.value = chatId
        _storageChatTitle.value = chatTitle
        prefs.edit()
            .putLong(KEY_STORAGE_CHAT_ID, chatId)
            .putString(KEY_STORAGE_CHAT_TITLE, chatTitle)
            .apply()
    }

    /**
     * Adiciona um novo filme ao Modo Vídeo a partir de um VideoItem.
     */
    fun addMovie(movie: MovieItem) {
        val current = _movies.value.toMutableList()
        current.add(movie)
        saveLocalData(current)
    }

    /**
     * Atualiza um filme existente.
     */
    fun updateMovie(movie: MovieItem) {
        val current = _movies.value.toMutableList()
        val index = current.indexOfFirst { it.id == movie.id }
        if (index >= 0) {
            current[index] = movie
            saveLocalData(current)
        }
    }

    /**
     * Remove um filme do Modo Vídeo.
     */
    fun removeMovie(movieId: String) {
        val current = _movies.value.toMutableList()
        current.removeAll { it.id == movieId }
        saveLocalData(current)
    }

    /**
     * Adiciona um episódio a uma temporada de uma série.
     */
    fun addEpisodeToSeries(
        seriesId: String,
        seasonNumber: Int,
        seasonTitle: String,
        episode: EpisodeItem
    ) {
        val current = _movies.value.toMutableList()
        val index = current.indexOfFirst { it.id == seriesId }
        if (index < 0) return

        val series = current[index]
        val seasons = series.seasons.toMutableList()
        val seasonIndex = seasons.indexOfFirst { it.seasonNumber == seasonNumber }

        if (seasonIndex >= 0) {
            val season = seasons[seasonIndex]
            val episodes = season.episodes.toMutableList()
            episodes.add(episode)
            seasons[seasonIndex] = season.copy(episodes = episodes)
        } else {
            seasons.add(SeasonItem(seasonNumber, seasonTitle, listOf(episode)))
            seasons.sortBy { it.seasonNumber }
        }

        current[index] = series.copy(seasons = seasons)
        saveLocalData(current)
    }

    /**
     * Serializa os dados do Modo Vídeo para JSON e salva no grupo do Telegram.
     */
    suspend fun saveToTelegram(): Result<Unit> {
        val chatId = _storageChatId.value
            ?: return Result.failure(Exception("Nenhum grupo de armazenamento selecionado"))

        return try {
            val movies = _movies.value
            val json = gson.toJson(movies)
            val message = "$VIDEO_MODE_TAG\n$json"
            telegramClient.sendTextMessage(chatId, message)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar no Telegram: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Restaura os dados do Modo Vídeo a partir de um grupo do Telegram.
     * Busca a mensagem mais recente com a tag VIDEO_MODE_TAG.
     */
    suspend fun restoreFromTelegram(chatId: Long, chatTitle: String): Result<Int> {
        return try {
            val result = telegramClient.searchTextMessage(chatId, VIDEO_MODE_TAG)
            result.fold(
                onSuccess = { messageText ->
                    val jsonStart = messageText.indexOf("\n")
                    if (jsonStart < 0) {
                        return Result.failure(Exception("Formato de dados inválido"))
                    }
                    val json = messageText.substring(jsonStart + 1)
                    val type = object : TypeToken<List<MovieItem>>() {}.type
                    val movies: List<MovieItem> = gson.fromJson(json, type)
                    saveLocalData(movies)
                    setStorageChat(chatId, chatTitle)
                    Result.success(movies.size)
                },
                onFailure = { e ->
                    Result.failure(Exception("Nenhum dado encontrado neste grupo: ${e.message}"))
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao restaurar do Telegram: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Gera um novo ID único para um item do Modo Vídeo.
     */
    fun generateId(): String = UUID.randomUUID().toString()

    /**
     * Retorna apenas os filmes.
     */
    fun getMovies(): List<MovieItem> = _movies.value.filter { it.type == VideoModeType.MOVIE }

    /**
     * Retorna apenas as séries.
     */
    fun getSeries(): List<MovieItem> = _movies.value.filter { it.type == VideoModeType.SERIES }
}
