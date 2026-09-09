package com.myanitrack.core.domain.repository

import com.myanitrack.core.common.result.AppResult

interface EpisodeRepository {
    suspend fun getReleasedEpisodeCount(animeId: Int): AppResult<Int>
}
