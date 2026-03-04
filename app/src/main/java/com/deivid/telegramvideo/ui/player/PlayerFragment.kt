package com.deivid.telegramvideo.ui.player

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.navigation.fragment.navArgs
import com.deivid.telegramvideo.R
import com.deivid.telegramvideo.data.player.TdLibDataSourceFactory
import com.deivid.telegramvideo.data.repository.TelegramClient
import com.deivid.telegramvideo.databinding.FragmentPlayerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

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

    @Inject
    lateinit var telegramClient: TelegramClient

    private var player: ExoPlayer? = null
    private var playWhenReady = true
    private var currentPosition = 0L
    private var playbackState = Player.STATE_IDLE
    private var currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

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
        hideSystemUI()

        setupClickListeners()
        observeUiState()

        // Inicia a preparação do vídeo
        viewModel.prepareVideo(args.videoItem)
    }

    private fun hideSystemUI() {
        (activity as? AppCompatActivity)?.supportActionBar?.hide()

        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun showSystemUI() {
        (activity as? AppCompatActivity)?.supportActionBar?.show()

        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, binding.root).show(WindowInsetsCompat.Type.systemBars())
    }

    private fun setupClickListeners() {
        binding.btnRetry.setOnClickListener {
            viewModel.retry()
        }

        // Configura botões customizados no controller
        val btnAspectRatio = binding.playerView.findViewById<ImageButton>(R.id.btn_aspect_ratio)
        btnAspectRatio?.setOnClickListener {
            cycleAspectRatio()
        }

        val btnPip = binding.playerView.findViewById<ImageButton>(R.id.btn_pip)
        btnPip?.setOnClickListener {
            enterPipMode()
        }
    }

    private fun cycleAspectRatio() {
        currentResizeMode = when (currentResizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        binding.playerView.resizeMode = currentResizeMode

        val modeName = when (currentResizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Ajustar (Fit)"
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Preencher (Fill)"
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom"
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH -> "Largura Fixa"
            AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT -> "Altura Fixa"
            else -> "Ajustar"
        }
        Toast.makeText(requireContext(), modeName, Toast.LENGTH_SHORT).show()
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            requireActivity().enterPictureInPictureMode(params)
        } else {
            Toast.makeText(requireContext(), "PiP não suportado nesta versão do Android", Toast.LENGTH_SHORT).show()
        }
    }

    @UnstableApi
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        binding.playerView.useController = !isInPictureInPictureMode
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
                            playVideo(state)
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
    @UnstableApi
    private fun playVideo(state: PlayerUiState.Ready) {
        releasePlayer()

        val dataSourceFactory = if (state.fileId != 0) {
            TdLibDataSourceFactory(telegramClient, state.fileId, state.fileSize)
        } else {
            DefaultDataSource.Factory(requireContext())
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        player = ExoPlayer.Builder(requireContext())
            .setMediaSourceFactory(mediaSourceFactory)
            .build().also { exoPlayer ->
            binding.playerView.player = exoPlayer
            binding.playerView.resizeMode = currentResizeMode

            val mediaItem = if (state.fileId != 0) {
                MediaItem.fromUri("tdlib://file/${state.fileId}")
            } else {
                MediaItem.fromUri(File(state.localPath).toUri())
            }
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
        showSystemUI()
        releasePlayer()
        _binding = null
    }
}
