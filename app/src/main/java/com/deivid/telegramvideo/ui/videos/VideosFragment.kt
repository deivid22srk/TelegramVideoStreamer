package com.deivid.telegramvideo.ui.videos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.deivid.telegramvideo.databinding.FragmentVideosBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment que exibe a grade de vídeos de um chat específico.
 */
@AndroidEntryPoint
class VideosFragment : Fragment() {

    private var _binding: FragmentVideosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VideosViewModel by viewModels()
    private val args: VideosFragmentArgs by navArgs()
    private lateinit var videosAdapter: VideosAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Define o título da toolbar com o nome do chat
        requireActivity().title = args.chatTitle

        setupRecyclerView()
        setupSwipeRefresh()
        observeUiState()

        viewModel.loadVideos(args.chatId)
    }

    private fun setupRecyclerView() {
        videosAdapter = VideosAdapter { video ->
            // Navega para o player ao clicar em um vídeo
            val action = VideosFragmentDirections.actionVideosFragmentToPlayerFragment(
                videoItem = video,
                chatTitle = args.chatTitle
            )
            findNavController().navigate(action)
        }

        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewVideos.layoutManager = gridLayoutManager
        binding.recyclerViewVideos.adapter = videosAdapter

        // Paginação: carrega mais vídeos ao chegar no fim da lista
        binding.recyclerViewVideos.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    viewModel.loadMoreVideos()
                }
            }
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.swipeRefresh.isRefreshing = false
                    when (state) {
                        is VideosUiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.recyclerViewVideos.isVisible = false
                            binding.tvEmpty.isVisible = false
                        }
                        is VideosUiState.Success -> {
                            binding.progressBar.isVisible = false
                            binding.recyclerViewVideos.isVisible = true
                            binding.tvEmpty.isVisible = false
                            videosAdapter.submitList(state.videos)
                        }
                        is VideosUiState.Empty -> {
                            binding.progressBar.isVisible = false
                            binding.recyclerViewVideos.isVisible = false
                            binding.tvEmpty.isVisible = true
                        }
                        is VideosUiState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.recyclerViewVideos.isVisible = false
                            binding.tvEmpty.isVisible = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
