package com.deivid.telegramvideo.ui.videos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.deivid.telegramvideo.data.model.MovieItem
import com.deivid.telegramvideo.data.repository.TelegramRepository
import com.deivid.telegramvideo.data.repository.VideoModeRepository
import com.deivid.telegramvideo.databinding.FragmentVideoModeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fragment que exibe a biblioteca do Modo Vídeo e gerencia o armazenamento.
 */
@AndroidEntryPoint
class VideoModeFragment : Fragment() {

    private var _binding: FragmentVideoModeBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var repository: TelegramRepository
    @Inject lateinit var videoModeRepository: VideoModeRepository

    private val viewModel: VideoModeViewModel by viewModels()
    private lateinit var movieAdapter: MovieAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoModeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeState()

        checkStorageAndLoad()
    }

    private fun setupRecyclerView() {
        movieAdapter = MovieAdapter(
            onMovieClick = { movie ->
                showMovieOptionsDialog(movie)
            },
            onMovieDelete = { movie ->
                showDeleteConfirmDialog(movie)
            }
        )

        val gridLayoutManager = GridLayoutManager(requireContext(), 3)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (movieAdapter.getItemViewType(position) == 0) 3 else 1
            }
        }

        binding.recyclerViewMovies.layoutManager = gridLayoutManager
        binding.recyclerViewMovies.adapter = movieAdapter

        binding.swipeRefresh.setOnRefreshListener {
            lifecycleScope.launch {
                videoModeRepository.restoreMovies()
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showMovieOptionsDialog(movie: MovieItem) {
        val options = arrayOf("Assistir", "Editar", "Excluir")
        AlertDialog.Builder(requireContext())
            .setTitle(movie.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> playMovie(movie)
                    1 -> showEditDialog(movie)
                    2 -> showDeleteConfirmDialog(movie)
                }
            }
            .show()
    }

    private fun playMovie(movie: MovieItem) {
        val action = VideoModeFragmentDirections.actionVideoModeFragmentToPlayerFragment(
            videoItem = movie.toVideoItem(),
            chatTitle = movie.title
        )
        findNavController().navigate(action)
    }

    private fun showEditDialog(movie: MovieItem) {
        EditMovieDialog.show(requireContext(), movie) { updatedMovie ->
            viewModel.updateMovie(updatedMovie)
            Toast.makeText(requireContext(), "Atualizado com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmDialog(movie: MovieItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Excluir item")
            .setMessage("Deseja excluir '${movie.title}' da biblioteca?")
            .setPositiveButton("Excluir") { _, _ ->
                viewModel.deleteMovie(movie)
                Toast.makeText(requireContext(), "Item excluído", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupClickListeners() {
        binding.btnSelectStorage.setOnClickListener {
            showChatSelectionDialog()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.libraryItems.collect { items ->
                    movieAdapter.submitList(items)
                    binding.tvEmpty.isVisible = items.isEmpty() && videoModeRepository.storageChatId.value != 0L
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                videoModeRepository.storageChatId.collect { chatId ->
                    binding.layoutSetup.isVisible = (chatId == 0L)
                    binding.swipeRefresh.isVisible = (chatId != 0L)
                }
            }
        }
    }

    private fun checkStorageAndLoad() {
        lifecycleScope.launch {
            if (videoModeRepository.storageChatId.value != 0L) {
                binding.progressBar.isVisible = true
                videoModeRepository.restoreMovies()
                binding.progressBar.isVisible = false
            }
        }
    }

    private fun showChatSelectionDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val chatsResult = repository.getChats()
            chatsResult.fold(
                onSuccess = { chats ->
                    val titles = chats.map { it.title }.toTypedArray()
                    AlertDialog.Builder(requireContext())
                        .setTitle("Selecionar Grupo de Armazenamento")
                        .setItems(titles) { _, which ->
                            val selectedChat = chats[which]
                            videoModeRepository.setStorageChat(selectedChat.id)
                            checkStorageAndLoad()
                        }
                        .show()
                },
                onFailure = {
                    Toast.makeText(requireContext(), "Erro ao carregar chats", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
