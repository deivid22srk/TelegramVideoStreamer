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
import com.deivid.telegramvideo.ui.videos.VideoModeFragmentDirections
import com.deivid.telegramvideo.R
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
    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoModeBinding.inflate(inflater, container, false)
        return binding.root
    }


    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(
            onMovieClick = { movie ->
                val action = VideoModeFragmentDirections.actionVideoModeFragmentToMovieDetailsFragment(
                    movieItem = movie
                )
                findNavController().navigate(action)
            },
            onMovieDelete = { movie ->
                showDeleteConfirmDialog(movie)
            },
            onMovieEdit = { movie ->
                showEditDialog(movie)
            }
        )

        binding.recyclerViewMovies.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.recyclerViewMovies.adapter = categoryAdapter

        binding.swipeRefresh.setOnRefreshListener {
            lifecycleScope.launch {
                binding.progressBar.isVisible = true
                val result = videoModeRepository.restoreMovies()
                binding.progressBar.isVisible = false
                binding.swipeRefresh.isRefreshing = false

                result.onSuccess {
                    Toast.makeText(requireContext(), "Biblioteca atualizada!", Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(requireContext(), "Erro ao restaurar: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
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

    private fun showEditDialog(movie: MovieItem) {
        AddMovieDialog.show(
            context = requireContext(),
            video = movie.toVideoItem(),
            remoteFileId = movie.remoteFileId,
            existingMovie = movie,
            onSelectVideo = {
                val action = VideoModeFragmentDirections.actionVideoModeFragmentToChatsFragment(isPicker = true)
                findNavController().navigate(action)
            }
        ) { updatedMovie ->
            lifecycleScope.launch {
                videoModeRepository.editMovie(updatedMovie)
                Toast.makeText(requireContext(), "Item atualizado!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observePickerResult() {
        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle
        savedStateHandle?.getLiveData<com.deivid.telegramvideo.data.model.VideoItem>("selected_video")?.observe(viewLifecycleOwner) { video ->
            val remoteId = savedStateHandle.get<String>("selected_remote_id")
            if (video != null && remoteId != null) {
                // Ao selecionar um vídeo do picker, abrimos o diálogo de adição/edição com os dados do vídeo
                AddMovieDialog.show(
                    context = requireContext(),
                    video = video,
                    remoteFileId = remoteId,
                    onSelectVideo = {
                        val action = VideoModeFragmentDirections.actionVideoModeFragmentToChatsFragment(isPicker = true)
                        findNavController().navigate(action)
                    }
                ) { movie ->
                    lifecycleScope.launch {
                        videoModeRepository.addMovie(movie)
                        Toast.makeText(requireContext(), "Adicionado!", Toast.LENGTH_SHORT).show()
                    }
                }
                // Limpa o resultado para não disparar novamente
                savedStateHandle.remove<com.deivid.telegramvideo.data.model.VideoItem>("selected_video")
                savedStateHandle.remove<String>("selected_remote_id")
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSelectStorage.setOnClickListener {
            showChatSelectionDialog()
        }

        binding.fabAdd.setOnClickListener {
            AddMovieDialog.show(
                context = requireContext(),
                video = null,
                remoteFileId = null,
                onSelectVideo = {
                    val action = VideoModeFragmentDirections.actionVideoModeFragmentToChatsFragment(isPicker = true)
                    findNavController().navigate(action)
                }
            ) { movie ->
                lifecycleScope.launch {
                    videoModeRepository.addMovie(movie)
                    Toast.makeText(requireContext(), "Adicionado!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeState()
        observePickerResult()

        checkStorageAndLoad()

        // Listener para o menu de backup
        requireActivity().addMenuProvider(object : androidx.core.view.MenuProvider {
            override fun onCreateMenu(menu: android.view.Menu, menuInflater: android.view.MenuInflater) {
                menuInflater.inflate(R.menu.menu_chats, menu)
                menu.findItem(R.id.action_search)?.isVisible = false
                menu.findItem(R.id.action_video_mode)?.isVisible = false
            }
            override fun onMenuItemSelected(menuItem: android.view.MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_backup -> {
                        performBackup()
                        true
                    }
                    R.id.action_refresh -> {
                        lifecycleScope.launch {
                            videoModeRepository.restoreMovies()
                        }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun performBackup() {
        lifecycleScope.launch {
            binding.progressBar.isVisible = true
            val result = videoModeRepository.createAndUploadBackup()
            binding.progressBar.isVisible = false
            result.fold(
                onSuccess = { Toast.makeText(requireContext(), "Backup ZIP enviado!", Toast.LENGTH_SHORT).show() },
                onFailure = { Toast.makeText(requireContext(), "Erro no backup: ${it.message}", Toast.LENGTH_SHORT).show() }
            )
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.libraryItems.collect { items ->
                    // Destaque
                    val firstMovie = items.filterIsInstance<VideoLibraryItem.Movie>().firstOrNull()?.movie
                    if (firstMovie != null) {
                        binding.layoutFeatured.visibility = View.VISIBLE
                        binding.tvFeaturedTitle.text = firstMovie.title
                        if (!firstMovie.coverUrl.isNullOrEmpty()) {
                            com.bumptech.glide.Glide.with(this@VideoModeFragment)
                                .load(firstMovie.coverUrl)
                                .into(binding.ivFeaturedBackdrop)
                        }
                        binding.btnFeaturedWatch.setOnClickListener {
                            val action = VideoModeFragmentDirections.actionVideoModeFragmentToMovieDetailsFragment(firstMovie)
                            findNavController().navigate(action)
                        }
                    } else {
                        binding.layoutFeatured.visibility = View.GONE
                    }

                    // Categorias
                    val categories = mutableListOf<LibraryDisplayItem.Category>()
                    var currentTitle = ""
                    var currentMovies = mutableListOf<MovieItem>()

                    items.forEach { item ->
                        when (item) {
                            is VideoLibraryItem.Header -> {
                                if (currentTitle.isNotEmpty()) {
                                    categories.add(LibraryDisplayItem.Category(currentTitle, currentMovies))
                                }
                                currentTitle = item.title
                                currentMovies = mutableListOf()
                            }
                            is VideoLibraryItem.Movie -> {
                                currentMovies.add(item.movie)
                            }
                        }
                    }
                    if (currentTitle.isNotEmpty()) {
                        categories.add(LibraryDisplayItem.Category(currentTitle, currentMovies))
                    }

                    categoryAdapter.submitList(categories)
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
