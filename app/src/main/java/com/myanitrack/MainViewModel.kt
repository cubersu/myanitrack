package com.myanitrack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myanitrack.core.domain.repository.AuthRepository
import com.myanitrack.core.domain.repository.UserPreferencesRepository
import com.myanitrack.core.model.AuthState
import com.myanitrack.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Acilis ekraninin ve temanin bagli oldugu uygulama duzeyi durum. */
data class MainUiState(
    val authState: AuthState = AuthState.Loading,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
) {
    val isReady: Boolean get() = authState != AuthState.Loading
}

@HiltViewModel
class MainViewModel @Inject constructor(
    authRepository: AuthRepository,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(
        authRepository.authState,
        preferencesRepository.preferences,
    ) { auth, prefs ->
        MainUiState(
            authState = auth,
            themeMode = prefs.themeMode,
            useDynamicColor = prefs.useDynamicColor,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = MainUiState(),
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
