package com.deivid.telegramvideo.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.navArgs
import com.deivid.telegramvideo.databinding.FragmentPlayerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

/**
 * Fragment do player de vídeo usando ExoPlayer (Media3).
 * Suporta streaming progressivo de vídeos do Telegram.
 */
@AndroidEntryPoint
class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlayerViewModel by viewModels()
    private val args: PlayerFragmentArgs by navArgs()

    private var player: ExoPlayer? = null
    private var playWhenReady = true
    private var currentPosition = 0L
    private var playbackState = Player.STATE_IDLE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().title = args.chatTitle

        setupClickListeners()
        observeUiState()

        // Inicia a preparação do vídeo
        viewModel.prepareVideo(args.videoItem)
    }

    private fun setupClickListeners() {
        binding.btnRetry.setOnClickListener {
            viewModel.retry()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is PlayerUiState.Idle -> {
                            binding.progressBar.isVisible = false
                            binding.tvStatus.isVisible = false
                            binding.btnRetry.isVisible = false
                        }
                        is PlayerUiState.Preparing -> {
                            binding.progressBar.isVisible = true
                            binding.tvStatus.isVisible = true
                            binding.tvStatus.text = "Preparando vídeo…"
                            binding.btnRetry.isVisible = false
                        }
                        is PlayerUiState.Downloading -> {
                            binding.progressBar.isVisible = true
                            binding.tvStatus.isVisible = true
                            binding.tvStatus.text = "Baixando: ${state.progress}%"
                            binding.btnRetry.isVisible = false
                        }
                        is PlayerUiState.Ready -> {
                            binding.progressBar.isVisible = false
                            binding.tvStatus.isVisible = false
                            binding.btnRetry.isVisible = false
                            playVideo(state.localPath)
                        }
                        is PlayerUiState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.tvStatus.isVisible = true
                            binding.tvStatus.text = state.message
                            binding.btnRetry.isVisible = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    /**
     * Inicializa o ExoPlayer e começa a reprodução do vídeo.
     */
    private fun playVideo(localPath: String) {
        releasePlayer()

        player = ExoPlayer.Builder(requireContext()).build().also { exoPlayer ->
            binding.playerView.player = exoPlayer

            val mediaItem = MediaItem.fromUri(File(localPath).toUri())
            exoPlayer.setMediaItem(mediaItem)

            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    playbackState = state
                    when (state) {
                        Player.STATE_BUFFERING -> {
                            binding.progressBar.isVisible = true
                        }
                        Player.STATE_READY -> {
                            binding.progressBar.isVisible = false
                        }
                        Player.STATE_ENDED -> {
                            binding.progressBar.isVisible = false
                        }
                        Player.STATE_IDLE -> {
                            binding.progressBar.isVisible = false
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    binding.progressBar.isVisible = false
                    binding.tvStatus.isVisible = true
                    binding.tvStatus.text = "Erro ao reproduzir: ${error.message}"
                    binding.btnRetry.isVisible = true
                }
            })

            exoPlayer.playWhenReady = playWhenReady
            exoPlayer.seekTo(currentPosition)
            exoPlayer.prepare()
        }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            playWhenReady = exoPlayer.playWhenReady
            currentPosition = exoPlayer.currentPosition
            playbackState = exoPlayer.playbackState
            exoPlayer.release()
            player = null
        }
    }

    override fun onStart() {
        super.onStart()
        if (playbackState == Player.STATE_READY) {
            player?.play()
        }
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        releasePlayer()
        _binding = null
    }
}
