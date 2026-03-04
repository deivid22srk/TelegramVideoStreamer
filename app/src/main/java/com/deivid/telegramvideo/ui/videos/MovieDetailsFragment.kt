package com.deivid.telegramvideo.ui.videos

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
            val action = MovieDetailsFragmentDirections.actionMovieDetailsFragmentToPlayerFragment(
                videoItem = movie.toVideoItem(),
                chatTitle = args.movieItem.title,
                movieItem = args.movieItem
            )
            findNavController().navigate(action)
        }
    }

    private fun setupSeriesUI(movie: MovieItem) {
        binding.layoutSeries.isVisible = true
        binding.btnWatchNow.isVisible = false

        episodeAdapter = VideosAdapter(
            onVideoClick = { video ->
                val action = MovieDetailsFragmentDirections.actionMovieDetailsFragmentToPlayerFragment(
                    videoItem = video,
                    chatTitle = movie.title
                )
                findNavController().navigate(action)
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
        binding.tabLayoutSeasons.removeAllTabs()
        for (i in 1..maxSeason) {
            binding.tabLayoutSeasons.addTab(binding.tabLayoutSeasons.newTab().setText("Temporada $i"))
        }
    }

    private fun updateEpisodesList(season: Int) {
        val seasonEpisodes = allSeriesEpisodes.filter { (it.season ?: 1) == season }
            .sortedBy { it.episode ?: 0 }
            .map { it.toVideoItem() }
        episodeAdapter.submitList(seasonEpisodes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
