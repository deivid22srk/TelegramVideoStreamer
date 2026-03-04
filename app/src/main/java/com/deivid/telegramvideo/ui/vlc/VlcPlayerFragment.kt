package com.deivid.telegramvideo.ui.vlc

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.deivid.telegramvideo.data.repository.VideoModeRepository
import com.deivid.telegramvideo.databinding.FragmentVlcPlayerBinding
import com.deivid.telegramvideo.ui.player.PlayerUiState
import com.deivid.telegramvideo.ui.player.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import javax.inject.Inject

@AndroidEntryPoint
class VlcPlayerFragment : Fragment() {

    private var _binding: FragmentVlcPlayerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlayerViewModel by viewModels()
    private val args: VlcPlayerFragmentArgs by navArgs()

    @Inject
    lateinit var videoModeRepository: VideoModeRepository

    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVlcPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFullscreen()
        setupVLC()
        observeUiState()

        viewModel.prepareVideo(args.videoItem)
    }

    private fun setupFullscreen() {
        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun setupVLC() {
        val argsList = mutableListOf<String>()
        argsList.add("-vvv") // verbosity
        libVLC = LibVLC(requireContext(), argsList)
        mediaPlayer = MediaPlayer(libVLC)
        mediaPlayer?.attachViews(binding.vlcVideoLayout, null, false, false)
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is PlayerUiState.Preparing, is PlayerUiState.Downloading -> {
                            binding.progressBar.isVisible = true
                        }
                        is PlayerUiState.Ready -> {
                            binding.progressBar.isVisible = false
                            playVideo(state)
                        }
                        is PlayerUiState.Error -> {
                            binding.progressBar.isVisible = false
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            binding.progressBar.isVisible = false
                        }
                    }
                }
            }
        }
    }

    private fun playVideo(state: PlayerUiState.Ready) {
        val media = if (state.fileId != 0) {
            // VLC não suporta nativamente o nosso TdLibDataSource.
            // Para VLC funcionar com TDLib precisariamos de um local server ou pipes.
            // Como MVP, vamos assumir que ele tenta abrir o localPath se existir, ou falhar elegantemente.
            if (state.localPath.isNotEmpty()) {
                Media(libVLC, Uri.parse(state.localPath))
            } else {
                Toast.makeText(requireContext(), "VLC requer download completo para este modo", Toast.LENGTH_LONG).show()
                return
            }
        } else {
            Media(libVLC, Uri.parse(state.localPath))
        }

        mediaPlayer?.media = media
        media.release()

        // Restaura posição se for MovieItem
        args.movieItem?.let {
            mediaPlayer?.time = it.playbackPosition
        }

        mediaPlayer?.play()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.stop()

        // Salva progresso
        val currentTime = mediaPlayer?.time ?: 0L
        args.movieItem?.let { movie ->
            if (currentTime > 0) {
                lifecycleScope.launch {
                    videoModeRepository.editMovie(movie.copy(playbackPosition = currentTime))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, binding.root).show(WindowInsetsCompat.Type.systemBars())

        mediaPlayer?.release()
        libVLC?.release()
        _binding = null
    }
}
