package com.myanitrack.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.myanitrack.core.common.result.getOrNull
import com.myanitrack.core.domain.repository.DiscoverRepository
import com.myanitrack.core.domain.repository.UserPreferencesRepository
import com.myanitrack.core.model.DiscoverQuery
import com.myanitrack.core.model.MediaNode
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.NamedRef
import com.myanitrack.core.model.RecommendationPair
import com.myanitrack.core.model.Season
import com.myanitrack.core.model.TopCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Kesfet ekranindaki sekmeler. */
enum class BrowseTab { TOP, SEASON, SEARCH, RECOMMENDATIONS }

data class BrowseUiState(
    val tab: BrowseTab = BrowseTab.TOP,
    val mediaType: MediaType = MediaType.ANIME,
    val topCategory: TopCategory = TopCategory.ALL,
    val season: Season = Season.current(),
    val query: String = "",
    val selectedGenre: NamedRef? = null,
    val genres: List<NamedRef> = emptyList(),
    val includeNsfw: Boolean = false,
) {
    val availableTopCategories: List<TopCategory> get() = TopCategory.availableFor(mediaType)

    /** Sezon sekmesi yalnizca anime icin anlamli. */
    val isSeasonTabAvailable: Boolean get() = mediaType.isAnime
}

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val discoverRepository: DiscoverRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val state = MutableStateFlow(BrowseUiState())

    val uiState: StateFlow<BrowseUiState> = state

    @OptIn(ExperimentalCoroutinesApi::class)
    val topResults: Flow<PagingData<MediaNode>> =
        combine(
            state.map { it.mediaType }.distinctUntilChanged(),
            state.map { it.topCategory }.distinctUntilChanged(),
        ) { type, category -> type to category }
            .flatMapLatest { (type, category) -> discoverRepository.topPager(type, category) }
            .cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val seasonResults: Flow<PagingData<MediaNode>> =
        combine(
            state.map { it.season }.distinctUntilChanged(),
            state.map { it.includeNsfw }.distinctUntilChanged(),
        ) { season, nsfw -> season to nsfw }
            .flatMapLatest { (season, nsfw) -> discoverRepository.seasonPager(season, nsfw) }
            .cachedIn(viewModelScope)

    /**
     * Arama sonuclari. Kullanici yazarken her tusa istek gondermemek icin
     * [SEARCH_DEBOUNCE_MS] kadar bekleniyor - Jikan-in 3/sn limiti icin kritik.
     */
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: Flow<PagingData<MediaNode>> =
        state.map { current ->
            DiscoverQuery(
                mediaType = current.mediaType,
                text = current.query.trim(),
                genreId = current.selectedGenre?.id,
                includeNsfw = current.includeNsfw,
            )
        }
            .distinctUntilChanged()
            .debounce(SEARCH_DEBOUNCE_MS)
            .flatMapLatest { query -> discoverRepository.searchPager(query) }
            .cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val recommendations: Flow<PagingData<RecommendationPair>> =
        state.map { it.mediaType }
            .distinctUntilChanged()
            .flatMapLatest { type -> discoverRepository.recommendationPager(type) }
            .cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            val prefs = preferencesRepository.preferences.first()
            state.update {
                it.copy(mediaType = prefs.defaultMediaType, includeNsfw = !prefs.hideNsfw)
            }
            loadGenres(prefs.defaultMediaType)
        }
    }

    fun selectTab(tab: BrowseTab) {
        state.update { it.copy(tab = tab) }
    }

    fun selectMediaType(type: MediaType) {
        if (state.value.mediaType == type) return
        state.update { current ->
            current.copy(
                mediaType = type,
                // Filtre karsi tarafta gecersiz olabilir; guvenli varsayilana don.
                topCategory = if (current.topCategory.isAvailableFor(type)) {
                    current.topCategory
                } else {
                    TopCategory.ALL
                },
                tab = if (current.tab == BrowseTab.SEASON && !type.isAnime) {
                    BrowseTab.TOP
                } else {
                    current.tab
                },
                selectedGenre = null,
                genres = emptyList(),
            )
        }
        viewModelScope.launch { loadGenres(type) }
    }

    fun selectTopCategory(category: TopCategory) {
        state.update { it.copy(topCategory = category) }
    }

    fun setQuery(query: String) {
        state.update { it.copy(query = query) }
    }

    fun selectGenre(genre: NamedRef?) {
        state.update { it.copy(selectedGenre = if (it.selectedGenre?.id == genre?.id) null else genre) }
    }

    fun nextSeason() {
        state.update { it.copy(season = it.season.next()) }
    }

    fun previousSeason() {
        state.update { it.copy(season = it.season.previous()) }
    }

    /** Tur listesi alinamazsa ekran calismaya devam eder, sadece filtre cubugu bos kalir. */
    private suspend fun loadGenres(type: MediaType) {
        val genres = discoverRepository.getGenres(type).getOrNull().orEmpty()
        state.update { if (it.mediaType == type) it.copy(genres = genres) else it }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
