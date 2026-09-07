package com.myanitrack.core.domain.repository

import androidx.paging.PagingData
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.model.CharacterSummary
import com.myanitrack.core.model.MediaDetails
import com.myanitrack.core.model.MediaRecommendation
import com.myanitrack.core.model.MediaReview
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.PromoVideo
import com.myanitrack.core.model.StaffSummary
import kotlinx.coroutines.flow.Flow

/**
 * Detay sayfasinin zenginlestirme verisi (Jikan v4, salt okunur).
 *
 * Tum okuma islemleri cache-then-network: once Room-daki onbellek, tazeligi
 * gecmisse ag. Ag hata verirse ve elde bayat onbellek varsa onbellek dondurulur;
 * kullanici hicbir zaman bos ekran gormez.
 */
interface MediaDetailsRepository {

    suspend fun getDetails(
        mediaType: MediaType,
        malId: Int,
        forceRefresh: Boolean = false,
    ): AppResult<MediaDetails>

    suspend fun getCharacters(mediaType: MediaType, malId: Int): AppResult<List<CharacterSummary>>

    /** Yalnizca anime icin anlamli; manga cagrisi bos liste doner. */
    suspend fun getStaff(mediaType: MediaType, malId: Int): AppResult<List<StaffSummary>>

    suspend fun getRecommendations(
        mediaType: MediaType,
        malId: Int,
    ): AppResult<List<MediaRecommendation>>

    suspend fun getPromoVideos(mediaType: MediaType, malId: Int): AppResult<List<PromoVideo>>

    fun reviewsPager(mediaType: MediaType, malId: Int): Flow<PagingData<MediaReview>>
}
