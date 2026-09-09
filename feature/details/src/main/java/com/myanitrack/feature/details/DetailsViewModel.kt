package com.myanitrack.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.common.result.getOrNull
import com.myanitrack.core.domain.repository.MediaDetailsRepository
import com.myanitrack.core.domain.repository.MediaListRepository
import com.myanitrack.core.domain.usecase.UpdateListEntryUseCase
import com.myanitrack.core.model.CharacterSummary
import com.myanitrack.core.model.ListStatus
import com.myanitrack.core.model.ListStatusUpdate
import com.myanitrack.core.model.MediaDetails
import com.myanitrack.core.model.MediaListEntry
import com.myanitrack.core.model.MediaRecommendation
import com.myanitrack.core.model.MediaReview
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.PromoVideo
import com.myanitrack.core.model.StaffSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailsUiState(
    val mediaType: MediaType = MediaType.ANIME,
    val malId: Int = 0,
    val details: MediaDetails? = null,
    val listEntry: MediaListEntry? = null,
    val characters: List<CharacterSummary> = emptyList(),
    val staff: List<StaffSummary> = emptyList(),
    val recommendations: List<MediaRecommendation> = emptyList(),
    val promoVideos: List<PromoVideo> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isEditing: Boolean = false,
    val error: AppError? = null,
) {
    val isInList: Boolean get() = listEntry != null

    /** Tanitim videolari: `/videos` bos donerse detaydaki fragmana duser. */
    val videos: List<PromoVideo>
        get() = promoVideos.ifEmpty { listOfNotNull(details?.trailer) }
}

sealed interface DetailsEvent {
    data class ShowError(val error: AppError) : DetailsEvent
    data class ShowMessage(val titleAdded: String) : DetailsEvent
}

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val detailsRepository: MediaDetailsRepository,
    private val listRepository: MediaListRepository,
    private val updateListEntry: UpdateListEntryUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args = DetailsArgs(savedStateHandle)

    private val loaded = MutableStateFlow(LoadedData())
    private var loadJob: Job? = null

    val uiState: StateFlow<DetailsUiState> = combine(
        loaded,
        listRepository.observeEntry(args.mediaType, args.malId),
    ) { data, entry ->
        DetailsUiState(
            mediaType = args.mediaType,
            malId = args.malId,
            // Jikan verisi yoksa listedeki temel veriyi (title, image vb.) kullan.
            details = data.details ?: entry?.node?.let { MediaDetails.fromNode(it) },
            listEntry = entry,
            characters = data.characters,
            staff = data.staff,
            recommendations = data.recommendations,
            promoVideos = data.promoVideos,
            isLoading = data.isLoading,
            isRefreshing = data.isRefreshing,
            isEditing = data.isEditing,
            error = data.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = DetailsUiState(mediaType = args.mediaType, malId = args.malId),
    )

    /** Review-lar sayfali geldigi icin ayri bir akista; ekran acilinca baslar. */
    val reviews: Flow<PagingData<MediaReview>> =
        detailsRepository.reviewsPager(args.mediaType, args.malId).cachedIn(viewModelScope)

    private val _events = Channel<DetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        load(forceRefresh = false)
    }

    fun refresh() = load(forceRefresh = true)

    fun startEditing() {
        loaded.update { it.copy(isEditing = true) }
    }

    fun stopEditing() {
        loaded.update { it.copy(isEditing = false) }
    }

    /** Detay sayfasindan listeye ekleme; MAL kaydi yoksa olusturur. */
    fun addToList(status: ListStatus) {
        viewModelScope.launch {
            when (val result = updateListEntry(args.mediaType, args.malId, ListStatusUpdate(status = status))) {
                is AppResult.Success -> _events.send(DetailsEvent.ShowMessage(result.data.node.title))
                is AppResult.Failure -> _events.send(DetailsEvent.ShowError(result.error))
            }
        }
    }

    fun saveEdit(update: ListStatusUpdate) {
        loaded.update { it.copy(isEditing = false) }
        if (update.isEmpty) return
        viewModelScope.launch {
            val result = updateListEntry(args.mediaType, args.malId, update)
            if (result is AppResult.Failure) _events.send(DetailsEvent.ShowError(result.error))
        }
    }

    fun removeFromList() {
        loaded.update { it.copy(isEditing = false) }
        viewModelScope.launch {
            val result = listRepository.deleteEntry(args.mediaType, args.malId)
            if (result is AppResult.Failure) _events.send(DetailsEvent.ShowError(result.error))
        }
    }

    /**
     * Ana detay once gosterilir; yardimci bolumler ardindan paralel yuklenir.
     *
     * Yalnizca ANA detay cagrisinin basarisizligi ekrani hata durumuna dusurur;
     * yardimci bolumler bos kalabilir. Boylece Jikan-in tekil uc noktalarindan
     * biri 504 verdiginde sayfanin geri kalani yine de gosterilir.
     */
    private fun load(forceRefresh: Boolean) {
        loadJob?.cancel()
        loaded.update {
            it.copy(
                isLoading = it.details == null,
                isRefreshing = it.details != null,
                error = null,
            )
        }

        loadJob = viewModelScope.launch {
            val type = args.mediaType
            val id = args.malId

            // Publish the main content before requesting optional enrichment.
            val detailsResult = detailsRepository.getDetails(type, id, forceRefresh)
            loaded.update { current ->
                current.copy(
                    details = detailsResult.getOrNull() ?: current.details,
                    isLoading = false,
                    isRefreshing = false,
                    error = (detailsResult as? AppResult.Failure)?.error,
                )
            }
            if (detailsResult is AppResult.Failure) {
                _events.send(DetailsEvent.ShowError(detailsResult.error))
                return@launch
            }
            coroutineScope {
                launch {
                    detailsRepository.getCharacters(type, id).getOrNull()?.let { value ->
                        loaded.update { it.copy(characters = value) }
                    }
                }
                launch {
                    detailsRepository.getStaff(type, id).getOrNull()?.let { value ->
                        loaded.update { it.copy(staff = value) }
                    }
                }
                launch {
                    detailsRepository.getRecommendations(type, id).getOrNull()?.let { value ->
                        loaded.update { it.copy(recommendations = value) }
                    }
                }
                launch {
                    detailsRepository.getPromoVideos(type, id).getOrNull()?.let { value ->
                        loaded.update { it.copy(promoVideos = value) }
                    }
                }
            }
        }
    }

    private data class LoadedData(
        val details: MediaDetails? = null,
        val characters: List<CharacterSummary> = emptyList(),
        val staff: List<StaffSummary> = emptyList(),
        val recommendations: List<MediaRecommendation> = emptyList(),
        val promoVideos: List<PromoVideo> = emptyList(),
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val isEditing: Boolean = false,
        val error: AppError? = null,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
