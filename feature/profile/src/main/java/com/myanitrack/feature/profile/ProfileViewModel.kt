package com.myanitrack.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.common.result.getOrNull
import com.myanitrack.core.domain.repository.ProfileRepository
import com.myanitrack.core.model.FeedUpdate
import com.myanitrack.core.model.Friend
import com.myanitrack.core.model.HistoryEntry
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.UserProfileDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Profil ekranindaki sekmeler. */
enum class ProfileTab { OVERVIEW, HISTORY, FRIENDS, FEED }

data class ProfileUiState(
    val userName: String = "",
    val isOwnProfile: Boolean = true,
    val tab: ProfileTab = ProfileTab.OVERVIEW,
    val profile: UserProfileDetails? = null,
    val history: List<HistoryEntry> = emptyList(),
    val friends: List<Friend> = emptyList(),
    val feed: List<FeedUpdate> = emptyList(),
    val historyFilter: MediaType? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isFeedLoading: Boolean = false,
    val error: AppError? = null,
) {
    /** Oturum yoksa ve kullanici adi bilinmiyorsa hicbir sey cekilemez. */
    val hasUser: Boolean get() = userName.isNotBlank()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Rota argumaninda kullanici adi varsa baskasinin profili aciliyor demektir. */
    private val requestedUserName: String? =
        savedStateHandle.get<String>(KEY_USER_NAME)?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(
        ProfileUiState(isOwnProfile = requestedUserName == null),
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val userName = requestedUserName ?: profileRepository.currentUserName.first()
            if (userName.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            _uiState.update { it.copy(userName = userName) }
            load(userName, forceRefresh = false)
        }
    }

    fun selectTab(tab: ProfileTab) {
        _uiState.update { it.copy(tab = tab) }
        // Akis pahali (arkadas basina bir RSS istegi); yalnizca sekmeye
        // gelindiginde ve daha once yuklenmediyse cekilir.
        if (tab == ProfileTab.FEED && _uiState.value.feed.isEmpty()) loadFeed(forceRefresh = false)
    }

    fun setHistoryFilter(mediaType: MediaType?) {
        _uiState.update { it.copy(historyFilter = mediaType) }
        val userName = _uiState.value.userName
        if (userName.isBlank()) return
        viewModelScope.launch {
            val history = profileRepository.getHistory(userName, mediaType).getOrNull().orEmpty()
            _uiState.update { it.copy(history = history) }
        }
    }

    fun refresh() {
        val userName = _uiState.value.userName
        if (userName.isBlank()) return
        viewModelScope.launch { load(userName, forceRefresh = true) }
        if (_uiState.value.tab == ProfileTab.FEED) loadFeed(forceRefresh = true)
    }

    /**
     * Profil, gecmis ve arkadaslar paralel cekilir.
     *
     * Yalnizca profil cagrisinin basarisizligi ekrani hata durumuna dusurur;
     * gecmis ya da arkadas listesi alinamazsa o sekme bos gorunur ama profil
     * bilgileri yine gosterilir.
     */
    private suspend fun load(userName: String, forceRefresh: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = it.profile == null,
                isRefreshing = it.profile != null,
                error = null,
            )
        }

        val filter = _uiState.value.historyFilter
        val result = coroutineScope {
            val profileAsync = async { profileRepository.getProfile(userName, forceRefresh) }
            val historyAsync = async { profileRepository.getHistory(userName, filter, forceRefresh) }
            val friendsAsync = async { profileRepository.getFriends(userName, forceRefresh) }

            Triple(profileAsync.await(), historyAsync.await(), friendsAsync.await())
        }

        val (profileResult, historyResult, friendsResult) = result
        _uiState.update { current ->
            current.copy(
                profile = profileResult.getOrNull() ?: current.profile,
                history = historyResult.getOrNull().orEmpty(),
                friends = friendsResult.getOrNull().orEmpty(),
                isLoading = false,
                isRefreshing = false,
                error = (profileResult as? AppResult.Failure)?.error,
            )
        }
    }

    private fun loadFeed(forceRefresh: Boolean) {
        val userName = _uiState.value.userName
        if (userName.isBlank() || _uiState.value.isFeedLoading) return

        _uiState.update { it.copy(isFeedLoading = true) }
        viewModelScope.launch {
            // Kendi profilimizde arkadas akisi, baskasinin profilinde o kisinin
            // kendi guncellemeleri gosterilir.
            val result = if (_uiState.value.isOwnProfile) {
                profileRepository.getFriendsFeed(userName, forceRefresh)
            } else {
                profileRepository.getUserFeed(userName, forceRefresh)
            }
            _uiState.update {
                it.copy(feed = result.getOrNull().orEmpty(), isFeedLoading = false)
            }
        }
    }

    private companion object {
        const val KEY_USER_NAME = "userName"
    }
}
