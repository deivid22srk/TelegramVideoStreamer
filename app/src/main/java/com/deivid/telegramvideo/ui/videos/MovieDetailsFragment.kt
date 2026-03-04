package com.deivid.telegramvideo.ui.videos

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.deivid.telegramvideo.ui.videos.MovieDetailsFragmentDirections
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.deivid.telegramvideo.R
import com.deivid.telegramvideo.data.model.MovieItem
import com.deivid.telegramvideo.databinding.FragmentMovieDetailsBinding
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MovieDetailsFragment : Fragment() {

    private var _binding: FragmentMovieDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: MovieDetailsFragmentArgs by navArgs()
    private val viewModel: VideoModeViewModel by viewModels()
    private lateinit var episodeAdapter: VideosAdapter

    private var allSeriesEpisodes: List<MovieItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovieDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val movie = args.movieItem

        setupUI(movie)
        if (movie.isSeries) {
            setupSeriesUI(movie)
            observeEpisodes(movie)
            observePickerResult()
        }
    }

    private fun setupUI(movie: MovieItem) {
        binding.tvTitle.text = movie.title
        binding.tvSynopsis.text = movie.synopsis

        if (!movie.coverUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(movie.coverUrl)
                .placeholder(R.drawable.ic_video_placeholder)
                .into(binding.ivBackdrop)
        }

        binding.btnWatchNow.setOnClickListener {
            val prefs = requireContext().getSharedPreferences("telegram_prefs", Context.MODE_PRIVATE)
            val playerType = prefs.getString("player_type", "EXO")

            if (playerType == "VLC") {
                val action = MovieDetailsFragmentDirections.actionMovieDetailsFragmentToVlcPlayerFragment(
                    videoItem = movie.toVideoItem(),
                    chatTitle = movie.title,
                    movieItem = movie
                )
                findNavController().navigate(action)
            } else {
                val action = MovieDetailsFragmentDirections.actionMovieDetailsFragmentToPlayerFragment(
                    videoItem = movie.toVideoItem(),
                    chatTitle = args.movieItem.title,
                    movieItem = args.movieItem
                )
                findNavController().navigate(action)
            }
        }
    }

    private fun setupSeriesUI(movie: MovieItem) {
        binding.layoutSeries.isVisible = true
        binding.btnWatchNow.isVisible = false

        binding.btnAddEpisode.setOnClickListener {
            val action = MovieDetailsFragmentDirections.actionMovieDetailsFragmentToChatsFragment(isPicker = true)
            findNavController().navigate(action)
        }

        binding.btnAddEpisode.setOnLongClickListener {
            addSeason()
            true
        }

        episodeAdapter = VideosAdapter(
            onVideoClick = { video ->
                val epMovie = allSeriesEpisodes.find { it.remoteFileId == video.remoteFileId }
                val prefs = requireContext().getSharedPreferences("telegram_prefs", Context.MODE_PRIVATE)
                val playerType = prefs.getString("player_type", "EXO")

                if (playerType == "VLC") {
                    val action = MovieDetailsFragmentDirections.actionMovieDetailsFragmentToVlcPlayerFragment(
                        videoItem = video,
                        chatTitle = movie.title,
                        movieItem = epMovie
                    )
                    findNavController().navigate(action)
                } else {
                    val action = MovieDetailsFragmentDirections.actionMovieDetailsFragmentToPlayerFragment(
                        videoItem = video,
                        chatTitle = movie.title,
                        movieItem = epMovie
                    )
                    findNavController().navigate(action)
                }
            },
            onVideoLongClick = {},
            onSelectionChanged = {}
        )

        binding.recyclerViewEpisodes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewEpisodes.adapter = episodeAdapter

        binding.tabLayoutSeasons.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val seasonNumber = tab?.position?.plus(1) ?: 1
                updateEpisodesList(seasonNumber)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeEpisodes(movie: MovieItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allMovies.collect { movies ->
                    val seriesTitle = movie.seriesTitle ?: movie.title
                    allSeriesEpisodes = movies.filter { it.seriesTitle == seriesTitle || it.title == seriesTitle }

                    setupTabs(allSeriesEpisodes)
                    if (allSeriesEpisodes.isNotEmpty()) {
                        val currentSeason = movie.season ?: 1
                        val tab = binding.tabLayoutSeasons.getTabAt(currentSeason - 1)
                        if (tab != null) {
                            tab.select()
                            updateEpisodesList(currentSeason)
                        } else {
                            binding.tabLayoutSeasons.getTabAt(0)?.select()
                            updateEpisodesList(1)
                        }
                    }
                }
            }
        }
    }

    private fun setupTabs(episodes: List<MovieItem>) {
        val maxSeason = episodes.maxOfOrNull { it.season ?: 1 } ?: 1
        val currentTabCount = binding.tabLayoutSeasons.tabCount
        if (maxSeason > currentTabCount) {
            for (i in (currentTabCount + 1)..maxSeason) {
                binding.tabLayoutSeasons.addTab(binding.tabLayoutSeasons.newTab().setText("Temporada $i"))
            }
        } else if (maxSeason < currentTabCount && maxSeason > 0) {
            // Se por algum motivo diminuiu (excluiu episódios), reconstruímos
            binding.tabLayoutSeasons.removeAllTabs()
            for (i in 1..maxSeason) {
                binding.tabLayoutSeasons.addTab(binding.tabLayoutSeasons.newTab().setText("Temporada $i"))
            }
        } else if (currentTabCount == 0) {
            binding.tabLayoutSeasons.addTab(binding.tabLayoutSeasons.newTab().setText("Temporada 1"))
        }
    }

    private fun addSeason() {
        val nextSeason = binding.tabLayoutSeasons.tabCount + 1
        binding.tabLayoutSeasons.addTab(binding.tabLayoutSeasons.newTab().setText("Temporada $nextSeason"), true)
        android.widget.Toast.makeText(requireContext(), "Temporada $nextSeason criada!", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun updateEpisodesList(season: Int) {
        val seasonEpisodes = allSeriesEpisodes.filter { (it.season ?: 1) == season }
            .sortedBy { it.episode ?: 0 }
            .map { it.toVideoItem() }
        episodeAdapter.submitList(seasonEpisodes)
    }

    private fun observePickerResult() {
        val movie = args.movieItem
        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle
        savedStateHandle?.getLiveData<com.deivid.telegramvideo.data.model.VideoItem>("selected_video")?.observe(viewLifecycleOwner) { video ->
            val remoteId = savedStateHandle.get<String>("selected_remote_id")
            if (video != null && remoteId != null) {
                lifecycleScope.launch {
                    val currentSeason = binding.tabLayoutSeasons.selectedTabPosition + 1
                    val maxEpisode = allSeriesEpisodes.filter { it.season == currentSeason }.maxOfOrNull { it.episode ?: 0 } ?: 0
                    val nextEpisode = maxEpisode + 1

                    val seriesTitle = movie.seriesTitle ?: movie.title
                    val newEpisode = MovieItem(
                        id = java.util.UUID.randomUUID().toString(),
                        title = "$seriesTitle - T$currentSeason E$nextEpisode",
                        synopsis = movie.synopsis,
                        coverUrl = movie.coverUrl,
                        isSeries = true,
                        seriesId = movie.seriesId ?: movie.id,
                        seriesTitle = seriesTitle,
                        season = currentSeason,
                        episode = nextEpisode,
                        remoteFileId = remoteId,
                        fileName = video.fileName,
                        duration = video.duration,
                        width = video.width,
                        height = video.height,
                        fileSize = video.fileSize,
                        mimeType = video.mimeType,
                        caption = video.caption,
                        date = video.date
                    )

                    (requireActivity() as? com.deivid.telegramvideo.ui.MainActivity)?.let {
                        // Idealmente MovieDetailsFragment deveria ter acesso ao repository via Hilt
                        // ou o ViewModel de VideoMode deveria ter um método addMovie.
                        // Como estamos usando o ViewModel compartilhado, vamos checar se ele tem o repo.
                    }

                    viewModel.addMovie(newEpisode)
                    android.widget.Toast.makeText(requireContext(), "Episódio adicionado!", android.widget.Toast.LENGTH_SHORT).show()
                }
                savedStateHandle.remove<com.deivid.telegramvideo.data.model.VideoItem>("selected_video")
                savedStateHandle.remove<String>("selected_remote_id")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
