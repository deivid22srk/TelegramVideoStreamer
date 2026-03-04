package com.deivid.telegramvideo.ui.videomode

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.deivid.telegramvideo.data.model.ChatItem
import com.deivid.telegramvideo.databinding.DialogSelectStorageGroupBinding
import com.deivid.telegramvideo.ui.chats.ChatsUiState
import com.deivid.telegramvideo.ui.chats.ChatsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Modo de operação do diálogo de seleção de grupo.
 */
enum class StorageGroupMode {
    SAVE,    // Criar/definir grupo para salvar dados
    RESTORE  // Restaurar dados de um grupo
}

/**
 * Diálogo para selecionar o grupo do Telegram onde os dados do Modo Vídeo serão salvos/restaurados.
 */
@AndroidEntryPoint
class SelectStorageGroupDialog : DialogFragment() {

    private var _binding: DialogSelectStorageGroupBinding? = null
    private val binding get() = _binding!!

    private val chatsViewModel: ChatsViewModel by viewModels()
    private val videoModeViewModel: VideoModeViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    private lateinit var groupAdapter: GroupSelectAdapter
    private var mode: StorageGroupMode = StorageGroupMode.SAVE

    companion object {
        private const val ARG_MODE = "mode"

        fun newInstance(mode: StorageGroupMode): SelectStorageGroupDialog {
            return SelectStorageGroupDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, mode.name)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = StorageGroupMode.valueOf(
            arguments?.getString(ARG_MODE) ?: StorageGroupMode.SAVE.name
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSelectStorageGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupRecyclerView()
        setupSearch()
        observeChats()

        chatsViewModel.loadChats()
        binding.progressBar.isVisible = true
    }

    private fun setupUI() {
        when (mode) {
            StorageGroupMode.SAVE -> {
                binding.tvDialogTitle.text = "Criar Modo Filme"
                binding.tvDialogSubtitle.text =
                    "Selecione o grupo onde os dados do Modo Vídeo serão salvos. " +
                    "Os dados serão codificados e enviados como mensagem neste grupo."
            }
            StorageGroupMode.RESTORE -> {
                binding.tvDialogTitle.text = "Restaurar Dados do Modo Filme"
                binding.tvDialogSubtitle.text =
                    "Selecione o grupo de onde os dados do Modo Vídeo serão restaurados. " +
                    "O app irá buscar a mensagem de dados mais recente neste grupo."
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun setupRecyclerView() {
        groupAdapter = GroupSelectAdapter { chat ->
            onGroupSelected(chat)
        }
        binding.recyclerViewGroups.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewGroups.adapter = groupAdapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                groupAdapter.filter(s?.toString() ?: "")
            }
        })
    }

    private fun observeChats() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatsViewModel.uiState.collect { state ->
                    when (state) {
                        is ChatsUiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.recyclerViewGroups.isVisible = false
                        }
                        is ChatsUiState.Success -> {
                            binding.progressBar.isVisible = false
                            binding.recyclerViewGroups.isVisible = true
                            groupAdapter.submitFullList(state.chats)
                        }
                        is ChatsUiState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.recyclerViewGroups.isVisible = true
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun onGroupSelected(chat: ChatItem) {
        when (mode) {
            StorageGroupMode.SAVE -> {
                videoModeViewModel.setStorageChat(chat.id, chat.title)
                videoModeViewModel.saveToTelegram()
            }
            StorageGroupMode.RESTORE -> {
                videoModeViewModel.restoreFromTelegram(chat.id, chat.title)
            }
        }
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
