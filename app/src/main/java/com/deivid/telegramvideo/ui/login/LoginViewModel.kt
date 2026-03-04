package com.deivid.telegramvideo.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deivid.telegramvideo.data.repository.AuthState
import com.deivid.telegramvideo.data.repository.TelegramRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estados da UI para as telas de login.
 */
sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object PhoneNumberSent : LoginUiState()
    object CodeVerified : LoginUiState()
    object PasswordRequired : LoginUiState()
    object Authorized : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

/**
 * ViewModel responsável por toda a lógica de autenticação no Telegram.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: TelegramRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val authState: StateFlow<AuthState> = repository.authState

    init {
        // Observa mudanças no estado de autenticação
        viewModelScope.launch {
            repository.authState.collect { state ->
                when (state) {
                    is AuthState.Authorized -> _uiState.value = LoginUiState.Authorized
                    is AuthState.WaitPassword -> _uiState.value = LoginUiState.PasswordRequired
                    is AuthState.Error -> _uiState.value = LoginUiState.Error(state.message)
                    else -> { /* Outros estados tratados pelos métodos abaixo */ }
                }
            }
        }
    }

    /**
     * Envia o número de telefone para iniciar o processo de login.
     */
    fun sendPhoneNumber(phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            _uiState.value = LoginUiState.Error("Digite um número de telefone válido")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = repository.sendPhoneNumber(phoneNumber.trim())
            result.fold(
                onSuccess = {
                    _uiState.value = LoginUiState.PhoneNumberSent
                },
                onFailure = { exception ->
                    _uiState.value = LoginUiState.Error(
                        exception.message ?: "Erro ao enviar número de telefone"
                    )
                }
            )
        }
    }

    /**
     * Verifica o código de autenticação recebido.
     */
    fun checkAuthCode(code: String) {
        if (code.isBlank() || code.length < 5) {
            _uiState.value = LoginUiState.Error("Digite o código completo")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = repository.checkAuthCode(code.trim())
            result.fold(
                onSuccess = {
                    _uiState.value = LoginUiState.CodeVerified
                },
                onFailure = { exception ->
                    _uiState.value = LoginUiState.Error(
                        exception.message ?: "Código inválido"
                    )
                }
            )
        }
    }

    /**
     * Verifica a senha de dois fatores.
     */
    fun checkAuthPassword(password: String) {
        if (password.isBlank()) {
            _uiState.value = LoginUiState.Error("Digite sua senha")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = repository.checkAuthPassword(password)
            result.fold(
                onSuccess = {
                    _uiState.value = LoginUiState.Authorized
                },
                onFailure = { exception ->
                    _uiState.value = LoginUiState.Error(
                        exception.message ?: "Senha incorreta"
                    )
                }
            )
        }
    }

    /**
     * Reseta o estado para Idle.
     */
    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }

    /**
     * Verifica se o usuário já está autenticado.
     */
    fun isAlreadyAuthorized(): Boolean = repository.isAuthorized()
}
