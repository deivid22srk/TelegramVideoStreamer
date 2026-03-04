package com.deivid.telegramvideo.ui.videomode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.deivid.telegramvideo.data.model.VideoModeType
import com.deivid.telegramvideo.databinding.FragmentVideoModeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment principal do Modo Vídeo.
 * Exibe as abas de Filmes e Séries com um ViewPager2.
 */
@AndroidEntryPoint
class VideoModeFragment : Fragment() {

    private var _binding: FragmentVideoModeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VideoModeViewModel by activityViewModels()

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

        setupToolbar()
        setupViewPager()
        setupFab()
        observeSyncState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupViewPager() {
        val pagerAdapter = VideoModePagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Filmes"
                1 -> "Séries"
                else -> ""
            }
        }.attach()
    }

    private fun setupFab() {
        binding.fabVideoMode.setOnClickListener {
            showManageDialog()
        }
    }

    private fun showManageDialog() {
        val storageChatTitle = viewModel.storageChatTitle.value
        val options = if (storageChatTitle.isEmpty()) {
            arrayOf("Criar Modo Filme", "Restaurar Dados do Modo Filme")
        } else {
            arrayOf(
                "Criar Modo Filme (atual: $storageChatTitle)",
                "Restaurar Dados do Modo Filme",
                "Salvar agora no Telegram"
            )
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Gerenciar Modo Vídeo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showSelectGroupDialog(StorageGroupMode.SAVE)
                    1 -> showSelectGroupDialog(StorageGroupMode.RESTORE)
                    2 -> {
                        viewModel.saveToTelegram()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showSelectGroupDialog(mode: StorageGroupMode) {
        val dialog = SelectStorageGroupDialog.newInstance(mode)
        dialog.show(childFragmentManager, "select_group_dialog")
    }

    private fun observeSyncState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.syncState.collect { state ->
                    when (state) {
                        is VideoModeSyncState.Success -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                            viewModel.resetSyncState()
                        }
                        is VideoModeSyncState.Error -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                            viewModel.resetSyncState()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Adapter do ViewPager2 para as abas de Filmes e Séries.
     */
    private inner class VideoModePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> MovieListFragment.newInstance(VideoModeType.MOVIE)
                1 -> MovieListFragment.newInstance(VideoModeType.SERIES)
                else -> MovieListFragment.newInstance(VideoModeType.MOVIE)
            }
        }
    }
}
