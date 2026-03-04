package com.deivid.telegramvideo.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.deivid.telegramvideo.R
import com.deivid.telegramvideo.databinding.FragmentCodeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment da tela de verificação do código enviado pelo Telegram.
 */
@AndroidEntryPoint
class CodeFragment : Fragment() {

    private var _binding: FragmentCodeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeUiState()
    }

    private fun setupClickListeners() {
        binding.btnVerify.setOnClickListener {
            val code = binding.etCode.text.toString().trim()
            viewModel.checkAuthCode(code)
        }

        binding.btnResend.setOnClickListener {
            // Volta para a tela anterior para reinserir o número
            findNavController().popBackStack()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is LoginUiState.Loading -> setLoadingState(true)
                        is LoginUiState.CodeVerified -> {
                            setLoadingState(false)
                            // Código verificado com sucesso, aguarda próximo estado
                        }
                        is LoginUiState.PasswordRequired -> {
                            setLoadingState(false)
                            navigateToPasswordScreen()
                        }
                        is LoginUiState.Authorized -> {
                            setLoadingState(false)
                            navigateToChats()
                        }
                        is LoginUiState.Error -> {
                            setLoadingState(false)
                            showError(state.message)
                            viewModel.resetState()
                        }
                        else -> setLoadingState(false)
                    }
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBar.isVisible = isLoading
        binding.btnVerify.isEnabled = !isLoading
        binding.etCode.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToPasswordScreen() {
        findNavController().navigate(R.id.action_codeFragment_to_passwordFragment)
    }

    private fun navigateToChats() {
        findNavController().navigate(R.id.action_codeFragment_to_chatsFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
