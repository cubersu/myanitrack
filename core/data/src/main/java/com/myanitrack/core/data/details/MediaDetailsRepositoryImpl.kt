package com.myanitrack.core.data.details

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.myanitrack.core.common.di.AppDispatcher
import com.myanitrack.core.common.di.Dispatcher
import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.common.result.map
import com.myanitrack.core.data.cache.RemoteCache
import com.myanitrack.core.data.paging.JikanPagingSource
import com.myanitrack.core.domain.repository.MediaDetailsRepository
import com.myanitrack.core.model.CharacterSummary
import com.myanitrack.core.model.MediaDetails
import com.myanitrack.core.model.MediaRecommendation
import com.myanitrack.core.model.MediaReview
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.PromoVideo
import com.myanitrack.core.model.StaffSummary
import com.myanitrack.core.network.jikan.JikanApiService
import com.myanitrack.core.network.jikan.dto.JikanCharacterEntryDto
import com.myanitrack.core.network.jikan.dto.JikanMediaDto
import com.myanitrack.core.network.jikan.dto.JikanRecommendationDto
import com.myanitrack.core.network.jikan.dto.JikanStaffEntryDto
import com.myanitrack.core.network.jikan.dto.JikanVideosDto
import com.myanitrack.core.network.jikan.mapper.toDetails
import com.myanitrack.core.network.jikan.mapper.toDomain
import com.myanitrack.core.network.jikan.mapper.toPromoVideos
import com.myanitrack.core.network.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer

@Singleton
class MediaDetailsRepositoryImpl @Inject constructor(
    private val jikan: JikanApiService,
    private val cache: RemoteCache,
    @Dispatcher(AppDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : MediaDetailsRepository {

    override suspend fun getDetails(
        mediaType: MediaType,
        malId: Int,
        forceRefresh: Boolean,
    ): AppResult<MediaDetails> = withContext(ioDispatcher) {
        cache.cachedCall(
            key = cacheKey("details", mediaType, malId),
            serializer = JikanMediaDto.serializer(),
            ttl = RemoteCache.DETAILS_TTL,
            forceRefresh = forceRefresh,
        ) {
            safeApiCall {
                if (mediaType.isAnime) jikan.getAnimeFull(malId) else jikan.getMangaFull(malId)
            }.requireData()
        }.map { it.toDetails(mediaType) }
    }

    override suspend fun getCharacters(
        mediaType: MediaType,
        malId: Int,
    ): AppResult<List<CharacterSummary>> = withContext(ioDispatcher) {
        cache.cachedCall(
            key = cacheKey("characters", mediaType, malId),
            serializer = ListSerializer(JikanCharacterEntryDto.serializer()),
            ttl = RemoteCache.STATIC_TTL,
        ) {
            safeApiCall {
                if (mediaType.isAnime) {
                    jikan.getAnimeCharacters(malId)
                } else {
                    jikan.getMangaCharacters(malId)
                }
            }.requireData()
        }.map { entries ->
            // MAL karakterleri rastgele siralamada dondurebiliyor; ana karakterler
            // once, sonra populerlik - detay sayfasinda en anlamli sira bu.
            entries.map { it.toDomain() }
                .sortedWith(
                    compareBy<CharacterSummary> { it.role?.lowercase() != "main" }
                        .thenByDescending { it.favorites },
                )
        }
    }

    /** Staff yalnizca anime tarafinda var; manga icin Jikan uc noktasi yok. */
    override suspend fun getStaff(
        mediaType: MediaType,
        malId: Int,
    ): AppResult<List<StaffSummary>> {
        if (!mediaType.isAnime) return AppResult.Success(emptyList())
        return withContext(ioDispatcher) {
            cache.cachedCall(
                key = cacheKey("staff", mediaType, malId),
                serializer = ListSerializer(JikanStaffEntryDto.serializer()),
                ttl = RemoteCache.STATIC_TTL,
            ) {
                safeApiCall { jikan.getAnimeStaff(malId) }.requireData()
            }.map { entries -> entries.map { it.toDomain() } }
        }
    }

    override suspend fun getRecommendations(
        mediaType: MediaType,
        malId: Int,
    ): AppResult<List<MediaRecommendation>> = withContext(ioDispatcher) {
        cache.cachedCall(
            key = cacheKey("recommendations", mediaType, malId),
            serializer = ListSerializer(JikanRecommendationDto.serializer()),
            ttl = RemoteCache.DETAILS_TTL,
        ) {
            safeApiCall {
                if (mediaType.isAnime) {
                    jikan.getAnimeRecommendations(malId)
                } else {
                    jikan.getMangaRecommendations(malId)
                }
            }.requireData()
        }.map { list -> list.map { it.toDomain(mediaType) } }
    }

    override suspend fun getPromoVideos(
        mediaType: MediaType,
        malId: Int,
    ): AppResult<List<PromoVideo>> {
        if (!mediaType.isAnime) return AppResult.Success(emptyList())
        return withContext(ioDispatcher) {
            cache.cachedCall(
                key = cacheKey("videos", mediaType, malId),
                serializer = JikanVideosDto.serializer(),
                ttl = RemoteCache.DETAILS_TTL,
            ) {
                safeApiCall { jikan.getAnimeVideos(malId) }.requireData()
            }.map { it.toPromoVideos() }
        }
    }

    /**
     * Review-lar sayfali ve hacimli; onbellege alinmaz, dogrudan Paging 3-ten akar.
     * Hiz siniri interceptor katmaninda zaten uygulaniyor.
     */
    override fun reviewsPager(mediaType: MediaType, malId: Int): Flow<PagingData<MediaReview>> =
        Pager(
            config = PagingConfig(
                pageSize = JikanApiService.PAGE_SIZE,
                enablePlaceholders = false,
                initialLoadSize = JikanApiService.PAGE_SIZE,
            ),
        ) {
            JikanPagingSource { page ->
                if (mediaType.isAnime) {
                    jikan.getAnimeReviews(malId, page)
                } else {
                    jikan.getMangaReviews(malId, page)
                }
            }
        }.flow
            .map { pagingData -> pagingData.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    private fun cacheKey(kind: String, mediaType: MediaType, malId: Int) =
        "jikan:$kind:${mediaType.name.lowercase()}:$malId"
}

/**
 * Jikan `{ "data": ... }` sarmalayicisini acar.
 * `data` beklenmedik sekilde bos gelirse bunu bicim hatasi olarak isaretleriz.
 */
internal fun <T : Any> AppResult<com.myanitrack.core.network.jikan.dto.JikanResponse<T>>.requireData():
    AppResult<T> = when (this) {
    is AppResult.Failure -> this
    is AppResult.Success -> data.data
        ?.let { AppResult.Success(it) }
        ?: AppResult.Failure(AppError.Serialization("Jikan response had no data"))
}
