package com.myanitrack.feature.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myanitrack.core.common.network.NetworkMonitor
import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.common.result.getOrNull
import com.myanitrack.core.domain.repository.EpisodeRepository
import com.myanitrack.core.domain.repository.ScheduleRepository
import com.myanitrack.core.model.BroadcastInfo
import com.myanitrack.core.model.AiringStatus
import com.myanitrack.core.domain.repository.MediaListRepository
import com.myanitrack.core.domain.repository.UserPreferencesRepository
import com.myanitrack.core.domain.usecase.IncrementProgressUseCase
import com.myanitrack.core.domain.usecase.ObserveMyListUseCase
import com.myanitrack.core.domain.usecase.RefreshMyListUseCase
import com.myanitrack.core.domain.usecase.UpdateListEntryUseCase
import com.myanitrack.core.model.ListFilter
import com.myanitrack.core.model.ListSortOption
import com.myanitrack.core.model.ListStatus
import com.myanitrack.core.model.ListStatusUpdate
import com.myanitrack.core.model.ListViewMode
import com.myanitrack.core.model.MediaListEntry
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.SortDirection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyListUiState(
    val mediaType: MediaType = MediaType.ANIME,
    val filter: ListFilter = ListFilter(),
    val viewMode: ListViewMode = ListViewMode.DETAILED_GRID,
    val entries: List<MediaListEntry> = emptyList(),
    val allEntries: List<MediaListEntry> = emptyList(),
    val broadcasts: Map<Int, BroadcastInfo> = emptyMap(),
    val statusCounts: Map<ListStatus, Int> = emptyMap(),
    val availableTags: List<String> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSearchActive: Boolean = false,
    val editingEntry: MediaListEntry? = null,
    val isOffline: Boolean = false,
    val pendingSyncCount: Int = 0,
    val error: AppError? = null,
    val releasedEpisodes: Map<Int, Int> = emptyMap(),
) {
    val isEmpty: Boolean get() = entries.isEmpty() && !isInitialLoading
}

/** Kullaniciya bir kez gosterilecek olaylar. */
sealed interface MyListEvent {
    data class ShowError(val error: AppError) : MyListEvent
    data class EntryDeleted(val title: String) : MyListEvent
}

@HiltViewModel
class MyListViewModel @Inject constructor(
    private val observeMyList: ObserveMyListUseCase,
    private val refreshMyList: RefreshMyListUseCase,
    private val updateListEntry: UpdateListEntryUseCase,
    private val incrementProgressUseCase: IncrementProgressUseCase,
    private val listRepository: MediaListRepository,
    private val preferencesRepository: UserPreferencesRepository,
    networkMonitor: NetworkMonitor,
    private val episodeRepository: EpisodeRepository,
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {

    private val mediaType = MutableStateFlow(MediaType.ANIME)

    /** Yalnizca ekrana ozel filtreler; siralama ve NSFW tercihlerden gelir. */
    private val screenFilter = MutableStateFlow(ListFilter())

    private val transient = MutableStateFlow(TransientState())
    private val releasedEpisodes = MutableStateFlow<Map<Int, Int>>(emptyMap())
    private val broadcasts = MutableStateFlow<Map<Int, BroadcastInfo>>(emptyMap())

    /**
     * Cevrimdisi durumu ve gonderilmeyi bekleyen degisiklik sayisi.
     * Ikisi de yalnizca bilgilendirme amacli; liste her durumda yerelden okunuyor.
     */
    private val connectivity = combine(
        networkMonitor.isOnline,
        listRepository.observePendingSyncCount(),
    ) { isOnline, pending -> Connectivity(isOffline = !isOnline, pendingCount = pending) }

    private val effectiveFilter: StateFlow<ListFilter> =
        combine(screenFilter, preferencesRepository.preferences) { filter, prefs ->
            filter.copy(
                sortBy = prefs.listSortOption,
                sortDirection = prefs.listSortDirection,
                hideNsfw = prefs.hideNsfw,
            )
        }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ListFilter())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val listData = combine(mediaType, effectiveFilter) { type, filter -> type to filter.copy(status = null) }
        .distinctUntilChanged()
        .flatMapLatest { (type, filter) ->
            combine(
                observeMyList(type, filter),
                listRepository.observeStatusCounts(type),
                listRepository.observeTags(type),
            ) { entries, counts, tags -> ListData(entries, counts, tags) }
        }

    /**
     * Ekrana ozel durumlar tek nesnede toplandi: `combine` yalnizca bes akisa
     * kadar tipli asiri yuklemeye sahip, altisinda `Array<Any>` fallback-ine
     * dusup tip guvenligini kaybediyor.
     */
    private val screenState = combine(
        transient,
        connectivity,
        preferencesRepository.preferences.map { it.listViewMode }.distinctUntilChanged(),
        releasedEpisodes,
        broadcasts,
    ) { flags, connection, viewMode, episodes, schedule -> ScreenState(flags, connection, viewMode, episodes, schedule) }

    val uiState: StateFlow<MyListUiState> = combine(
        mediaType,
        effectiveFilter,
        listData,
        screenState,
    ) { type, filter, data, screen ->
        val flags = screen.flags
        val connection = screen.connectivity
        val viewMode = screen.viewMode
        MyListUiState(
            mediaType = type,
            filter = filter,
            viewMode = viewMode,
            entries = data.entries.filter { it.mediaType == type && (filter.status == null || it.listStatus.status == filter.status) },
            allEntries = data.entries.filter { it.mediaType == type },
            broadcasts = screen.broadcasts,
            statusCounts = data.counts,
            availableTags = data.tags,
            isInitialLoading = flags.isInitialLoading,
            isRefreshing = flags.isRefreshing,
            isSearchActive = flags.isSearchActive,
            editingEntry = flags.editingEntry,
            isOffline = connection.isOffline,
            pendingSyncCount = connection.pendingCount,
            error = flags.error,
            releasedEpisodes = screen.releasedEpisodes,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = MyListUiState(),
    )

    private val _events = Channel<MyListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch { loadBroadcasts() }
        viewModelScope.launch {
            listData.map { data ->
                data.entries.filter {
                    it.mediaType.isAnime && it.listStatus.status == ListStatus.WATCHING &&
                        it.node.airingStatus !in setOf(AiringStatus.FINISHED, AiringStatus.NOT_YET_AIRED)
                }.map { it.id }.distinct().sorted()
            }.distinctUntilChanged().collectLatest { ids ->
                // Sequential and cached: opening the list must not create a request burst.
                ids.forEach { id ->
                    episodeRepository.getReleasedEpisodeCount(id).getOrNull()?.let { count ->
                        releasedEpisodes.update { it + (id to count) }
                    }
                }
            }
        }
        viewModelScope.launch {
            val prefs = preferencesRepository.preferences.first()
            mediaType.value = prefs.defaultMediaType
            // Onbellek bossa ilk acilista otomatik senkronize et; doluysa
            // once yerel veri gosterilir, tazeleme arka planda calisir.
            syncInitial(prefs.defaultMediaType)
        }
    }

    fun selectMediaType(type: MediaType) {
        if (mediaType.value == type) return
        mediaType.value = type
        transient.update { it.copy(isInitialLoading = true) }
        viewModelScope.launch {
            preferencesRepository.setDefaultMediaType(type)
            syncInitial(type)
        }
    }

    fun selectStatus(status: ListStatus?) {
        screenFilter.update { it.copy(status = status) }
    }

    fun setQuery(query: String) {
        screenFilter.update { it.copy(query = query) }
    }

    fun setSearchActive(active: Boolean) {
        transient.update { it.copy(isSearchActive = active) }
        if (!active) screenFilter.update { it.copy(query = "") }
    }

    fun selectTag(tag: String?) {
        screenFilter.update { it.copy(tag = tag) }
    }

    fun setViewMode(mode: ListViewMode) {
        viewModelScope.launch { preferencesRepository.setListViewMode(mode) }
    }

    fun setSort(option: ListSortOption, direction: SortDirection) {
        viewModelScope.launch { preferencesRepository.setListSort(option, direction) }
    }

    fun refresh() {
        if (transient.value.isRefreshing) return
        transient.update { it.copy(isRefreshing = true) }
        viewModelScope.launch { loadBroadcasts() }
        viewModelScope.launch {
            val result = refreshMyList(mediaType.value)
            transient.update { it.copy(isRefreshing = false, isInitialLoading = false) }
            if (result is AppResult.Failure) emitError(result.error)
        }
    }

    fun incrementProgress(entry: MediaListEntry) {
        if (!entry.canIncrement) return
        viewModelScope.launch {
            val result = incrementProgressUseCase(entry)
            if (result is AppResult.Failure) emitError(result.error)
        }
    }

    fun startEditing(entry: MediaListEntry) {
        transient.update { it.copy(editingEntry = entry) }
    }

    fun stopEditing() {
        transient.update { it.copy(editingEntry = null) }
    }

    fun saveEdit(entry: MediaListEntry, update: ListStatusUpdate) {
        transient.update { it.copy(editingEntry = null) }
        if (update.isEmpty) return
        viewModelScope.launch {
            val result = updateListEntry(entry.mediaType, entry.id, update)
            if (result is AppResult.Failure) emitError(result.error)
        }
    }

    fun deleteEntry(entry: MediaListEntry) {
        transient.update { it.copy(editingEntry = null) }
        viewModelScope.launch {
            when (val result = listRepository.deleteEntry(entry.mediaType, entry.id)) {
                is AppResult.Success -> _events.send(MyListEvent.EntryDeleted(entry.node.title))
                is AppResult.Failure -> emitError(result.error)
            }
        }
    }

    private suspend fun syncInitial(type: MediaType) {
        val hasCache = listRepository.observeList(type, ListFilter(status = null))
            .first()
            .isNotEmpty()
        transient.update { it.copy(isInitialLoading = !hasCache, isRefreshing = hasCache) }

        val result = refreshMyList(type)
        transient.update { it.copy(isInitialLoading = false, isRefreshing = false) }
        // Onbellek varken tazeleme hatasi kullaniciyi rahatsiz etmemeli;
        // yerel veri zaten ekranda. Onbellek yoksa hata gosterilir.
        if (result is AppResult.Failure && !hasCache) emitError(result.error)
    }

    private suspend fun loadBroadcasts() {
        // A single cached weekly schedule supplies every card; clock ticks never use the network.
        scheduleRepository.getWeek().getOrNull()?.let { week ->
            broadcasts.value = week.days.values.flatten().associate { it.id to it.broadcast }
        }
    }

    private suspend fun emitError(error: AppError) {
        transient.update { it.copy(error = error) }
        _events.send(MyListEvent.ShowError(error))
        transient.update { it.copy(error = null) }
    }

    private data class Connectivity(val isOffline: Boolean, val pendingCount: Int)

    private data class ScreenState(
        val flags: TransientState,
        val connectivity: Connectivity,
        val viewMode: ListViewMode,
        val releasedEpisodes: Map<Int, Int>,
        val broadcasts: Map<Int, BroadcastInfo>,
    )

    private data class ListData(
        val entries: List<MediaListEntry>,
        val counts: Map<ListStatus, Int>,
        val tags: List<String>,
    )

    private data class TransientState(
        val isInitialLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val isSearchActive: Boolean = false,
        val editingEntry: MediaListEntry? = null,
        val error: AppError? = null,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
