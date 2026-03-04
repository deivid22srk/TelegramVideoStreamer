package com.deivid.telegramvideo.ui.videos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
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
import com.deivid.telegramvideo.R
import com.deivid.telegramvideo.data.model.MovieItem
import com.deivid.telegramvideo.data.model.VideoItem
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
        setupMenu()
        setupSwipeRefresh()
        observeUiState()

        viewModel.loadVideos(args.chatId)
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                if (videosAdapter.isSelectionMode) {
                    menu.add(Menu.NONE, 100, Menu.NONE, "Adicionar à Série")
                        .setIcon(R.drawable.ic_movie)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                    menu.add(Menu.NONE, 101, Menu.NONE, "Cancelar")
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                    return
                }

                menuInflater.inflate(R.menu.menu_chats, menu)
                // Remove itens que não fazem sentido aqui
                menu.findItem(R.id.action_logout)?.isVisible = false

                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem.actionView as SearchView
                searchView.queryHint = "Filtrar vídeos…"
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean = false
                    override fun onQueryTextChange(newText: String?): Boolean {
                        viewModel.filterVideos(newText ?: "")
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    100 -> {
                        showAddSelectionToSeriesDialog()
                        true
                    }
                    101 -> {
                        videosAdapter.clearSelection()
                        true
                    }
                    R.id.action_refresh -> {
                        viewModel.refresh()
                        true
                    }
                    R.id.action_video_mode -> {
                        findNavController().navigate(R.id.action_videosFragment_to_videoModeFragment)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerView() {
        videosAdapter = VideosAdapter(
            onVideoClick = { video ->
                // Navega para o player ao clicar em um vídeo
                val action = VideosFragmentDirections.actionVideosFragmentToPlayerFragment(
                    videoItem = video,
                    chatTitle = args.chatTitle
                )
                findNavController().navigate(action)
            },
            onVideoLongClick = { video ->
                showAddToVideoModeDialog(video)
            },
            onSelectionChanged = { count ->
                requireActivity().invalidateOptionsMenu()
                if (count > 0) {
                    requireActivity().title = "$count selecionados"
                } else {
                    requireActivity().title = args.chatTitle
                }
            }
        )

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

    private fun showAddToVideoModeDialog(video: VideoItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            val remoteFileId = viewModel.getRemoteFileId(video)
            if (remoteFileId != null) {
                AddMovieDialog.show(requireContext(), video, remoteFileId) { movie ->
                    viewModel.addToVideoMode(movie)
                    Toast.makeText(requireContext(), "Adicionado ao Modo Vídeo!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Erro ao obter ID do arquivo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddSelectionToSeriesDialog() {
        val selectedVideos = videosAdapter.getSelectedVideos()
        if (selectedVideos.isEmpty()) return

        viewLifecycleOwner.lifecycleScope.launch {
            val video = selectedVideos.first()
            val remoteFileId = viewModel.getRemoteFileId(video)
            if (remoteFileId != null) {
                AddMovieDialog.show(requireContext(), video, remoteFileId) { baseMovie ->
                    lifecycleScope.launch {
                        val seriesId = java.util.UUID.randomUUID().toString()
                        val seriesTitle = baseMovie.seriesTitle ?: baseMovie.title

                        selectedVideos.forEachIndexed { index, selectedVideo ->
                            val rId = viewModel.getRemoteFileId(selectedVideo) ?: ""
                            val currentEpisode = (baseMovie.episode ?: 1) + index
                            val movie = baseMovie.copy(
                                id = java.util.UUID.randomUUID().toString(),
                                seriesId = seriesId,
                                seriesTitle = seriesTitle,
                                title = "$seriesTitle - T${baseMovie.season ?: 1} E$currentEpisode",
                                episode = currentEpisode,
                                remoteFileId = rId,
                                fileName = selectedVideo.fileName,
                                duration = selectedVideo.duration,
                                fileSize = selectedVideo.fileSize,
                                messageId = 0L
                            )
                            viewModel.addToVideoMode(movie)
                        }
                        videosAdapter.clearSelection()
                        Toast.makeText(requireContext(), "Episódios adicionados!", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Erro ao obter ID do arquivo", Toast.LENGTH_SHORT).show()
            }
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
