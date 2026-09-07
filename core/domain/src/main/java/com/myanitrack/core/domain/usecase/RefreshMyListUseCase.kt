package com.myanitrack.core.domain.usecase

import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.domain.repository.MediaListRepository
import com.myanitrack.core.model.MediaType
import javax.inject.Inject

class RefreshMyListUseCase @Inject constructor(
    private val repository: MediaListRepository,
) {
    suspend operator fun invoke(mediaType: MediaType): AppResult<Unit> =
        repository.refresh(mediaType)
}
