package com.myanitrack.core.domain.usecase

import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.domain.repository.MediaListRepository
import com.myanitrack.core.model.ListStatusUpdate
import com.myanitrack.core.model.MediaListEntry
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.MyListStatus
import javax.inject.Inject

/**
 * Liste kaydini gunceller ve puanin gecerli araliktan cikmasini engeller.
 */
class UpdateListEntryUseCase @Inject constructor(
    private val repository: MediaListRepository,
) {
    suspend operator fun invoke(
        mediaType: MediaType,
        malId: Int,
        update: ListStatusUpdate,
    ): AppResult<MediaListEntry> {
        val sanitized = update.copy(
            score = update.score?.coerceIn(MyListStatus.SCORE_NOT_RATED, MyListStatus.SCORE_MAX),
            progress = update.progress?.coerceAtLeast(0),
            volumesRead = update.volumesRead?.coerceAtLeast(0),
        )
        return repository.updateEntry(mediaType, malId, sanitized)
    }
}
