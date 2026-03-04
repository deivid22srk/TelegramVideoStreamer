package com.deivid.telegramvideo.ui.chats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.deivid.telegramvideo.R
import com.deivid.telegramvideo.databinding.FragmentChatsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment que exibe a lista de chats do usuário autenticado.
 */
@AndroidEntryPoint
class ChatsFragment : Fragment() {

    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatsViewModel by viewModels()
    private val args: ChatsFragmentArgs by navArgs()
    private lateinit var chatsAdapter: ChatsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupMenu()
        setupSwipeRefresh()
        setupFab()
        observeUiState()
    }

    private fun setupFab() {
        binding.fabVideoMode.setOnClickListener {
            findNavController().navigate(R.id.action_chatsFragment_to_videoModeFragment)
        }
    }

    private fun setupRecyclerView() {
        chatsAdapter = ChatsAdapter { chat ->
            // Navega para a tela de vídeos do chat selecionado
            val action = ChatsFragmentDirections.actionChatsFragmentToVideosFragment(
                chatId = chat.id,
                chatTitle = chat.title,
                isPicker = args.isPicker
            )
            findNavController().navigate(action)
        }
        binding.recyclerViewChats.adapter = chatsAdapter
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_chats, menu)

                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem.actionView as SearchView
                searchView.queryHint = getString(R.string.search_chats)
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean = false
                    override fun onQueryTextChange(newText: String?): Boolean {
                        viewModel.searchChats(newText ?: "")
                        return true
                    }
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_logout -> {
                        showLogoutDialog()
                        true
                    }
                    R.id.action_refresh -> {
                        viewModel.loadChats()
                        true
                    }
                    R.id.action_video_mode -> {
                        findNavController().navigate(R.id.action_chatsFragment_to_videoModeFragment)
                        true
                    }
                    R.id.action_settings -> {
                        val action = ChatsFragmentDirections.actionChatsFragmentToSetupFragment(isEditing = true)
                        findNavController().navigate(action)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadChats()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.swipeRefresh.isRefreshing = false
                    when (state) {
                        is ChatsUiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.recyclerViewChats.isVisible = false
                            binding.tvEmpty.isVisible = false
                        }
                        is ChatsUiState.Success -> {
                            binding.progressBar.isVisible = false
                            if (state.chats.isEmpty()) {
                                binding.recyclerViewChats.isVisible = false
                                binding.tvEmpty.isVisible = true
                            } else {
                                binding.recyclerViewChats.isVisible = true
                                binding.tvEmpty.isVisible = false
                                chatsAdapter.submitList(state.chats)
                            }
                        }
                        is ChatsUiState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.recyclerViewChats.isVisible = false
                            binding.tvEmpty.isVisible = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                        is ChatsUiState.LoggedOut -> {
                            navigateToLogin()
                        }
                    }
                }
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirm)
            .setPositiveButton(R.string.yes) { _, _ -> viewModel.logout() }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun navigateToLogin() {
        findNavController().navigate(R.id.action_chatsFragment_to_loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
