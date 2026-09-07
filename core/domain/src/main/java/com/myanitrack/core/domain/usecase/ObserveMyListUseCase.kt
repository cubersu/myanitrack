package com.myanitrack.core.domain.usecase

import com.myanitrack.core.domain.repository.MediaListRepository
import com.myanitrack.core.model.ListFilter
import com.myanitrack.core.model.MediaListEntry
import com.myanitrack.core.model.MediaType
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Filtrelenmis/siralanmis liste akisi. */
class ObserveMyListUseCase @Inject constructor(
    private val repository: MediaListRepository,
) {
    operator fun invoke(mediaType: MediaType, filter: ListFilter): Flow<List<MediaListEntry>> =
        repository.observeList(mediaType, filter)
}
