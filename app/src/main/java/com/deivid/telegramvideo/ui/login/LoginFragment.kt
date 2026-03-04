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
import com.deivid.telegramvideo.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Fragment da tela de login com número de telefone.
 * Permite ao usuário inserir seu número para receber o código de verificação.
 */
@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Verifica se já está autenticado
        if (viewModel.isAlreadyAuthorized()) {
            navigateToChats()
            return
        }

        setupClickListeners()
        observeUiState()
    }

    private fun setupClickListeners() {
        binding.btnSendCode.setOnClickListener {
            val phone = binding.etPhone.text.toString().trim()
            viewModel.sendPhoneNumber(phone)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is LoginUiState.Idle -> {
                            setLoadingState(false)
                        }
                        is LoginUiState.Loading -> {
                            setLoadingState(true)
                        }
                        is LoginUiState.PhoneNumberSent -> {
                            setLoadingState(false)
                            navigateToCodeScreen()
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
        binding.btnSendCode.isEnabled = !isLoading
        binding.etPhone.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToCodeScreen() {
        findNavController().navigate(R.id.action_loginFragment_to_codeFragment)
    }

    private fun navigateToChats() {
        findNavController().navigate(R.id.action_loginFragment_to_chatsFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
