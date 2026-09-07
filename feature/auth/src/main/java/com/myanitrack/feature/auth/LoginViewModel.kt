package com.myanitrack.feature.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myanitrack.core.common.auth.AuthRedirectBus
import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.domain.model.AuthRequest
import com.myanitrack.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Giris ekraninin durumu. */
data class LoginUiState(
    val isClientConfigured: Boolean = true,
    val isAuthorizing: Boolean = false,
    val isExchangingToken: Boolean = false,
    val error: AppError? = null,
) {
    val isBusy: Boolean get() = isAuthorizing || isExchangingToken
}

sealed interface LoginEvent {
    /** Tarayicida acilacak MAL yetkilendirme adresi. */
    data class OpenAuthorizeUrl(val url: String) : LoginEvent

    data object LoginSucceeded : LoginEvent
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authRedirectBus: AuthRedirectBus,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(isClientConfigured = authRepository.isClientConfigured),
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        // Tarayicidan donus surec olumunden sonra da gelebilir; bu yuzden
        // bekleyen istek SavedStateHandle-da tutuluyor.
        viewModelScope.launch {
            authRedirectBus.redirects.collect(::onRedirect)
        }
    }

    fun startLogin() {
        if (_uiState.value.isBusy) return
        if (!authRepository.isClientConfigured) {
            _uiState.update { it.copy(isClientConfigured = false) }
            return
        }
        val request = authRepository.createAuthRequest()
        savedStateHandle[KEY_VERIFIER] = request.codeVerifier
        savedStateHandle[KEY_STATE] = request.state
        _uiState.update { it.copy(isAuthorizing = true, error = null) }
        viewModelScope.launch { _events.send(LoginEvent.OpenAuthorizeUrl(request.authorizeUrl)) }
    }

    /** Kullanici tarayiciyi kapatip geri donduyse butonu tekrar kullanilabilir yap. */
    fun onAuthorizationAbandoned() {
        if (_uiState.value.isExchangingToken) return
        _uiState.update { it.copy(isAuthorizing = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun onRedirect(uri: String) {
        val verifier = savedStateHandle.get<String>(KEY_VERIFIER)
        val state = savedStateHandle.get<String>(KEY_STATE)
        if (verifier == null || state == null) {
            // Beklemedigimiz bir yonlendirme; sessizce yok say.
            authRedirectBus.clear()
            return
        }

        _uiState.update { it.copy(isAuthorizing = false, isExchangingToken = true, error = null) }

        viewModelScope.launch {
            val request = AuthRequest(authorizeUrl = "", codeVerifier = verifier, state = state)
            val result = authRepository.completeLogin(uri, request)
            authRedirectBus.clear()
            savedStateHandle.remove<String>(KEY_VERIFIER)
            savedStateHandle.remove<String>(KEY_STATE)

            when (result) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isExchangingToken = false) }
                    _events.send(LoginEvent.LoginSucceeded)
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isExchangingToken = false, error = result.error)
                }
            }
        }
    }

    private companion object {
        const val KEY_VERIFIER = "oauth_code_verifier"
        const val KEY_STATE = "oauth_state"
    }
}
