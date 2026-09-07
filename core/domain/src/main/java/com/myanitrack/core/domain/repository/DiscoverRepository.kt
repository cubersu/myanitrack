package com.myanitrack.core.domain.repository

import androidx.paging.PagingData
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.model.DiscoverQuery
import com.myanitrack.core.model.MediaNode
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.NamedRef
import com.myanitrack.core.model.RecommendationPair
import com.myanitrack.core.model.Season
import com.myanitrack.core.model.TopCategory
import kotlinx.coroutines.flow.Flow

/** Kesfet ekrani: top listeler, sezonluk, arama, tur gezinmesi, oneriler. */
interface DiscoverRepository {

    fun topPager(mediaType: MediaType, category: TopCategory): Flow<PagingData<MediaNode>>

    fun seasonPager(season: Season, includeNsfw: Boolean): Flow<PagingData<MediaNode>>

    fun searchPager(query: DiscoverQuery): Flow<PagingData<MediaNode>>

    fun recommendationPager(mediaType: MediaType): Flow<PagingData<RecommendationPair>>

    /** Tur listesi nadiren degisir; uzun sureli onbellege alinir. */
    suspend fun getGenres(mediaType: MediaType): AppResult<List<NamedRef>>
}
