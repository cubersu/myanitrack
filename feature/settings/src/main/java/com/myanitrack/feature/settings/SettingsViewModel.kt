package com.myanitrack.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myanitrack.core.domain.repository.AuthRepository
import com.myanitrack.core.domain.repository.UserPreferencesRepository
import com.myanitrack.core.domain.schedule.AiringSyncScheduler
import com.myanitrack.core.model.AuthState
import com.myanitrack.core.model.ListViewMode
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.ThemeMode
import com.myanitrack.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val userName: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository,
    private val airingSyncScheduler: AiringSyncScheduler,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferences,
        authRepository.authState,
    ) { prefs, auth ->
        SettingsUiState(
            preferences = prefs,
            userName = (auth as? AuthState.LoggedIn)?.userName,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SettingsUiState(),
    )

    fun setThemeMode(mode: ThemeMode) = launchPref { preferencesRepository.setThemeMode(mode) }

    fun setDynamicColor(enabled: Boolean) =
        launchPref { preferencesRepository.setDynamicColor(enabled) }

    fun setDefaultMediaType(type: MediaType) =
        launchPref { preferencesRepository.setDefaultMediaType(type) }

    fun setListViewMode(mode: ListViewMode) =
        launchPref { preferencesRepository.setListViewMode(mode) }

    fun setHideNsfw(hide: Boolean) = launchPref { preferencesRepository.setHideNsfw(hide) }

    /**
     * Bildirim tercihi degisince arka plan isi de guncellenmeli; aksi halde
     * kullanici kapatsa bile periyodik senkronizasyon calismaya devam ederdi.
     */
    fun setAiringNotifications(enabled: Boolean) = launchPref {
        preferencesRepository.setAiringNotifications(enabled)
        airingSyncScheduler.applyPreferences(
            notificationsEnabled = enabled,
            onlyOnWifi = uiState.value.preferences.syncOnlyOnWifi,
        )
    }

    fun setSyncOnlyOnWifi(enabled: Boolean) = launchPref {
        preferencesRepository.setSyncOnlyOnWifi(enabled)
        airingSyncScheduler.applyPreferences(
            notificationsEnabled = uiState.value.preferences.airingNotificationsEnabled,
            onlyOnWifi = enabled,
        )
    }

    /** Cikis: token silinir, sifreleme anahtari yok edilir. */
    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    private fun launchPref(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
