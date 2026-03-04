package com.deivid.telegramvideo.ui.videomode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.deivid.telegramvideo.data.model.MovieItem
import com.deivid.telegramvideo.data.model.VideoModeType
import com.deivid.telegramvideo.databinding.FragmentMovieListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment que exibe a lista de filmes ou séries dentro do ViewPager do Modo Vídeo.
 */
@AndroidEntryPoint
class MovieListFragment : Fragment() {

    private var _binding: FragmentMovieListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VideoModeViewModel by activityViewModels()
    private lateinit var movieAdapter: MovieAdapter

    private var contentType: VideoModeType = VideoModeType.MOVIE

    companion object {
        private const val ARG_TYPE = "content_type"

        fun newInstance(type: VideoModeType): MovieListFragment {
            return MovieListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TYPE, type.name)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contentType = VideoModeType.valueOf(
            arguments?.getString(ARG_TYPE) ?: VideoModeType.MOVIE.name
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovieListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeUiState()
    }

    private fun setupRecyclerView() {
        movieAdapter = MovieAdapter(
            onItemClick = { movie ->
                navigateToDetail(movie)
            }
        )
        binding.recyclerViewMovies.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewMovies.adapter = movieAdapter
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is VideoModeUiState.Success -> {
                            val items = if (contentType == VideoModeType.MOVIE) {
                                state.movies
                            } else {
                                state.series
                            }
                            if (items.isEmpty()) {
                                binding.recyclerViewMovies.isVisible = false
                                binding.tvEmpty.isVisible = true
                                binding.tvEmpty.text = if (contentType == VideoModeType.MOVIE) {
                                    "Nenhum filme adicionado.\nSegure sobre um vídeo para adicionar."
                                } else {
                                    "Nenhuma série adicionada.\nSegure sobre um vídeo para adicionar."
                                }
                            } else {
                                binding.recyclerViewMovies.isVisible = true
                                binding.tvEmpty.isVisible = false
                                movieAdapter.submitList(items)
                            }
                        }
                        is VideoModeUiState.Empty -> {
                            binding.recyclerViewMovies.isVisible = false
                            binding.tvEmpty.isVisible = true
                            binding.tvEmpty.text = if (contentType == VideoModeType.MOVIE) {
                                "Nenhum filme adicionado.\nSegure sobre um vídeo para adicionar."
                            } else {
                                "Nenhuma série adicionada.\nSegure sobre um vídeo para adicionar."
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun navigateToDetail(movie: MovieItem) {
        val action = VideoModeFragmentDirections.actionVideoModeFragmentToMovieDetailFragment(
            movieId = movie.id
        )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
