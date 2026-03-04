package com.deivid.telegramvideo.ui.videomode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.deivid.telegramvideo.R
import com.deivid.telegramvideo.data.model.EpisodeItem
import com.deivid.telegramvideo.data.model.MovieItem
import com.deivid.telegramvideo.data.model.VideoItem
import com.deivid.telegramvideo.data.model.VideoModeType
import com.deivid.telegramvideo.databinding.FragmentMovieDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

/**
 * Fragment que exibe os detalhes de um filme ou série do Modo Vídeo.
 */
@AndroidEntryPoint
class MovieDetailFragment : Fragment() {

    private var _binding: FragmentMovieDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VideoModeViewModel by activityViewModels()
    private val args: MovieDetailFragmentArgs by navArgs()

    private var currentMovie: MovieItem? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovieDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        loadMovieData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadMovieData() {
        val movieId = args.movieId

        // Tenta carregar imediatamente do estado atual
        val currentState = viewModel.uiState.value
        if (currentState is VideoModeUiState.Success) {
            val found = (currentState.movies + currentState.series).find { it.id == movieId }
            found?.let { bindMovie(it) }
        }

        // Observa mudanças no estado para encontrar o filme
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state is VideoModeUiState.Success) {
                        val found = (state.movies + state.series).find { it.id == movieId }
                        found?.let { bindMovie(it) }
                    }
                }
            }
        }
    }

    private fun bindMovie(movie: MovieItem) {
        currentMovie = movie

        // Título na toolbar colapsável
        binding.collapsingToolbar.title = movie.title

        // Tipo
        binding.tvType.text = if (movie.type == VideoModeType.MOVIE) "FILME" else "SÉRIE"

        // Título
        binding.tvTitle.text = movie.title

        // Ano
        binding.tvYear.text = movie.year.ifEmpty { "—" }

        // Gênero
        binding.tvGenre.text = movie.genre.ifEmpty { "—" }

        // Sinopse
        if (movie.synopsis.isNotEmpty()) {
            binding.tvSynopsisLabel.isVisible = true
            binding.tvSynopsis.isVisible = true
            binding.tvSynopsis.text = movie.synopsis
        } else {
            binding.tvSynopsisLabel.isVisible = false
            binding.tvSynopsis.isVisible = false
        }

        // Capa
        if (!movie.coverImagePath.isNullOrEmpty()) {
            Glide.with(this)
                .load(File(movie.coverImagePath))
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.ic_video_placeholder)
                .into(binding.ivCover)
        } else {
            binding.ivCover.setImageResource(R.drawable.ic_video_placeholder)
        }

        // Conteúdo específico por tipo
        if (movie.type == VideoModeType.MOVIE) {
            setupMovieContent(movie)
        } else {
            setupSeriesContent(movie)
        }

        // Botão de remover
        binding.fabDelete.setOnClickListener {
            showDeleteConfirmation(movie)
        }
    }

    private fun setupMovieContent(movie: MovieItem) {
        binding.btnWatch.isVisible = true
        binding.layoutSeasons.isVisible = false

        binding.btnWatch.setOnClickListener {
            if (movie.messageId != null && movie.chatId != null && movie.fileId != null) {
                val videoItem = MovieToVideoItem(movie)
                val action = MovieDetailFragmentDirections
                    .actionMovieDetailFragmentToPlayerFragment(
                        videoItem = videoItem,
                        chatTitle = movie.title
                    )
                findNavController().navigate(action)
            } else {
                Toast.makeText(requireContext(), "Vídeo não disponível", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSeriesContent(movie: MovieItem) {
        binding.btnWatch.isVisible = false
        binding.layoutSeasons.isVisible = true

        val seasonAdapter = SeasonAdapter { episode ->
            navigateToEpisode(episode)
        }
        binding.recyclerViewSeasons.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewSeasons.adapter = seasonAdapter
        seasonAdapter.submitList(movie.seasons.sortedBy { it.seasonNumber })
    }

    private fun navigateToEpisode(episode: EpisodeItem) {
        val videoItem = EpisodeToVideoItem(episode)
        val action = MovieDetailFragmentDirections
            .actionMovieDetailFragmentToPlayerFragment(
                videoItem = videoItem,
                chatTitle = currentMovie?.title ?: "Episódio"
            )
        findNavController().navigate(action)
    }

    private fun showDeleteConfirmation(movie: MovieItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remover do Modo Vídeo")
            .setMessage("Deseja remover \"${movie.title}\" do Modo Vídeo?")
            .setPositiveButton("Remover") { _, _ ->
                viewModel.removeItem(movie.id)
                findNavController().navigateUp()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Converte um MovieItem (filme) em VideoItem para reprodução.
 */
fun MovieToVideoItem(movie: MovieItem): VideoItem {
    return VideoItem(
        messageId = movie.messageId ?: 0L,
        chatId = movie.chatId ?: 0L,
        fileId = movie.fileId ?: 0,
        fileName = movie.title,
        duration = movie.duration,
        width = 0,
        height = 0,
        fileSize = movie.fileSize,
        mimeType = movie.mimeType,
        caption = movie.synopsis,
        date = (movie.createdAt / 1000).toInt(),
        thumbnailPath = movie.coverImagePath,
        localPath = null,
        isDownloaded = false,
        downloadedSize = 0L
    )
}

/**
 * Converte um EpisodeItem em VideoItem para reprodução.
 */
fun EpisodeToVideoItem(episode: EpisodeItem): VideoItem {
    return VideoItem(
        messageId = episode.messageId,
        chatId = episode.chatId,
        fileId = episode.fileId,
        fileName = episode.title,
        duration = episode.duration,
        width = 0,
        height = 0,
        fileSize = episode.fileSize,
        mimeType = episode.mimeType,
        caption = episode.title,
        date = 0,
        thumbnailPath = episode.thumbnailPath,
        localPath = episode.localPath,
        isDownloaded = episode.localPath != null,
        downloadedSize = if (episode.localPath != null) episode.fileSize else 0L
    )
}
