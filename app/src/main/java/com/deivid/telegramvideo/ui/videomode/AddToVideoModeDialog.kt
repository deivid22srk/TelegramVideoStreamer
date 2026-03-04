package com.deivid.telegramvideo.ui.videomode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.deivid.telegramvideo.data.model.MovieItem
import com.deivid.telegramvideo.data.model.VideoItem
import com.deivid.telegramvideo.databinding.DialogAddToVideoModeBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Diálogo para adicionar um vídeo ao Modo Vídeo como filme ou episódio de série.
 * Acionado ao segurar sobre um item de vídeo na lista.
 */
@AndroidEntryPoint
class AddToVideoModeDialog : DialogFragment() {

    private var _binding: DialogAddToVideoModeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VideoModeViewModel by activityViewModels()

    private var videoItem: VideoItem? = null
    private var seriesList: List<MovieItem> = emptyList()
    private var selectedSeriesId: String? = null

    companion object {
        private const val ARG_VIDEO = "video_item"

        fun newInstance(videoItem: VideoItem): AddToVideoModeDialog {
            return AddToVideoModeDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_VIDEO, videoItem)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        videoItem = arguments?.getParcelable(ARG_VIDEO)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddToVideoModeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val video = videoItem ?: run {
            dismiss()
            return
        }

        setupTypeSelector()
        setupSeriesSelector()
        setupButtons(video)

        // Preenche o título com a legenda do vídeo, se disponível
        if (video.caption.isNotEmpty()) {
            binding.etTitle.setText(video.caption)
        }
    }

    private fun setupTypeSelector() {
        binding.rgType.setOnCheckedChangeListener { _, checkedId ->
            val isSeries = checkedId == binding.rbSeries.id
            binding.layoutSeriesFields.isVisible = isSeries
            binding.layoutExistingSeries.isVisible = isSeries && seriesList.isNotEmpty()
        }
    }

    private fun setupSeriesSelector() {
        seriesList = viewModel.getSeriesList()

        if (seriesList.isNotEmpty()) {
            val options = mutableListOf("Nova série") + seriesList.map { it.title }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                options
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerSeries.adapter = adapter

            binding.spinnerSeries.setOnItemSelectedListener(
                object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: android.widget.AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        selectedSeriesId = if (position == 0) null
                        else seriesList[position - 1].id

                        // Se selecionou uma série existente, preenche o título
                        if (selectedSeriesId != null) {
                            val series = seriesList[position - 1]
                            binding.etTitle.setText(series.title)
                            binding.etSynopsis.setText(series.synopsis)
                            binding.etYear.setText(series.year)
                            binding.etGenre.setText(series.genre)
                        }
                    }

                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                }
            )
        }
    }

    private fun setupButtons(video: VideoItem) {
        binding.btnCancel.setOnClickListener { dismiss() }

        binding.btnAdd.setOnClickListener {
            val title = binding.etTitle.text?.toString()?.trim() ?: ""
            if (title.isEmpty()) {
                binding.tilTitle.error = "Informe um título"
                return@setOnClickListener
            }
            binding.tilTitle.error = null

            val synopsis = binding.etSynopsis.text?.toString()?.trim() ?: ""
            val year = binding.etYear.text?.toString()?.trim() ?: ""
            val genre = binding.etGenre.text?.toString()?.trim() ?: ""

            val isSeries = binding.rbSeries.isChecked

            if (isSeries) {
                val seasonNumberStr = binding.etSeasonNumber.text?.toString()?.trim() ?: "1"
                val episodeNumberStr = binding.etEpisodeNumber.text?.toString()?.trim() ?: "1"
                val seasonTitle = binding.etSeasonTitle.text?.toString()?.trim() ?: ""
                val episodeTitle = binding.etEpisodeTitle.text?.toString()?.trim() ?: ""

                val seasonNumber = seasonNumberStr.toIntOrNull() ?: 1
                val episodeNumber = episodeNumberStr.toIntOrNull() ?: 1

                if (episodeTitle.isEmpty()) {
                    binding.tilEpisodeTitle.error = "Informe o título do episódio"
                    return@setOnClickListener
                }
                binding.tilEpisodeTitle.error = null

                viewModel.addAsEpisode(
                    videoItem = video,
                    seriesId = selectedSeriesId,
                    seriesTitle = title,
                    seriesSynopsis = synopsis,
                    seriesYear = year,
                    seriesGenre = genre,
                    seasonNumber = seasonNumber,
                    seasonTitle = seasonTitle.ifEmpty { "Temporada $seasonNumber" },
                    episodeNumber = episodeNumber,
                    episodeTitle = episodeTitle
                )
                Toast.makeText(
                    requireContext(),
                    "Episódio adicionado à série \"$title\"",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                viewModel.addAsMovie(
                    videoItem = video,
                    title = title,
                    synopsis = synopsis,
                    year = year,
                    genre = genre
                )
                Toast.makeText(
                    requireContext(),
                    "Filme \"$title\" adicionado ao Modo Vídeo",
                    Toast.LENGTH_SHORT
                ).show()
            }

            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
