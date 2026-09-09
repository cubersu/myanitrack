package com.myanitrack.core.data.discover

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.map
import com.myanitrack.core.common.di.AppDispatcher
import com.myanitrack.core.common.di.Dispatcher
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.common.result.map
import com.myanitrack.core.data.cache.RemoteCache
import com.myanitrack.core.data.details.requireData
import com.myanitrack.core.data.paging.JikanPagingSource
import com.myanitrack.core.data.paging.MalNodePagingSource
import com.myanitrack.core.data.paging.ResilientSearchPagingSource
import com.myanitrack.core.domain.repository.DiscoverRepository
import com.myanitrack.core.model.DiscoverQuery
import com.myanitrack.core.model.MediaNode
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.NamedRef
import com.myanitrack.core.model.RecommendationPair
import com.myanitrack.core.model.Season
import com.myanitrack.core.model.TopCategory
import com.myanitrack.core.network.jikan.JikanApiService
import com.myanitrack.core.network.jikan.dto.JikanGenreDto
import com.myanitrack.core.network.jikan.dto.JikanMediaDto
import com.myanitrack.core.network.jikan.dto.JikanPagedResponse
import com.myanitrack.core.network.jikan.mapper.toDomain
import com.myanitrack.core.network.jikan.mapper.toNode
import com.myanitrack.core.network.mal.MalApiService
import com.myanitrack.core.network.mal.dto.MalPagedResponse
import com.myanitrack.core.network.mal.dto.MalListEntryDto
import com.myanitrack.core.network.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer

@Singleton
class DiscoverRepositoryImpl @Inject constructor(
    private val jikan: JikanApiService,
    private val malApi: MalApiService,
    private val cache: RemoteCache,
    @Dispatcher(AppDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : DiscoverRepository {

    override fun topPager(
        mediaType: MediaType,
        category: TopCategory,
    ): Flow<PagingData<MediaNode>> {
        // MAL has no equivalent for the publishing/upcoming manga filters.
        if (mediaType.isAnime || category in setOf(TopCategory.ALL, TopCategory.BY_POPULARITY, TopCategory.FAVORITE)) {
            val ranking = category.apiValue ?: "all"
            return malNodePager(mediaType) { offset, limit ->
                if (mediaType.isAnime) malApi.getAnimeRanking(rankingType = ranking, offset = offset, limit = limit)
                else malApi.getMangaRanking(rankingType = ranking, offset = offset, limit = limit)
            }
        }
        return nodePager(mediaType) { page ->
            jikan.getTopManga(filter = category.apiValue, page = page)
        }
    }

    override fun seasonPager(season: Season, includeNsfw: Boolean): Flow<PagingData<MediaNode>> =
        malNodePager(MediaType.ANIME) { offset, limit ->
            malApi.getSeasonAnime(
                year = season.year,
                season = season.name.apiValue,
                offset = offset,
                limit = limit,
                nsfw = includeNsfw,
            )
        }

    override fun searchPager(query: DiscoverQuery): Flow<PagingData<MediaNode>> {
        if (query.isEmpty) return flowOf(PagingData.empty())
        if (query.text.isNotBlank() && query.genreId == null && query.producerId == null && query.orderBy == null) {
            return malNodePager(query.mediaType) { offset, limit ->
                if (query.mediaType.isAnime) malApi.searchAnime(query = query.text, offset = offset, limit = limit, nsfw = query.includeNsfw)
                else malApi.searchManga(query = query.text, offset = offset, limit = limit, nsfw = query.includeNsfw)
            }
        }
        return Pager(config = pagingConfig()) {
            ResilientSearchPagingSource(jikan, malApi, query)
        }.flow.flowOn(ioDispatcher)
    }

    private fun malNodePager(
        mediaType: MediaType,
        loadPage: suspend (Int, Int) -> MalPagedResponse<MalListEntryDto>,
    ): Flow<PagingData<MediaNode>> = Pager(config = pagingConfig()) {
        MalNodePagingSource(mediaType, loadPage)
    }.flow.flowOn(ioDispatcher)

    /**
     * Genel oneri akisi: her kayit birbirine onerilen iki yapim icerir.
     * Iki girdisi olmayan bozuk kayitlar atlanir.
     */
    override fun recommendationPager(mediaType: MediaType): Flow<PagingData<RecommendationPair>> =
        Pager(config = pagingConfig()) {
            JikanPagingSource { page ->
                if (mediaType.isAnime) {
                    jikan.getGlobalAnimeRecommendations(page)
                } else {
                    jikan.getGlobalMangaRecommendations(page)
                }
            }
        }.flow
            .map { pagingData ->
                pagingData
                    .map { dto ->
                        RecommendationPair(
                            liked = dto.entry.getOrNull(0)?.toNode(mediaType) ?: EMPTY_NODE,
                            suggested = dto.entry.getOrNull(1)?.toNode(mediaType) ?: EMPTY_NODE,
                            comment = dto.content,
                        )
                    }
                    // Iki gecerli girdisi olmayan bozuk kayitlari ele.
                    .filter { it.liked.id != 0 && it.suggested.id != 0 }
            }
            .flowOn(ioDispatcher)

    override suspend fun getGenres(mediaType: MediaType): AppResult<List<NamedRef>> =
        withContext(ioDispatcher) {
            cache.cachedCall(
                key = "jikan:genres:${mediaType.name.lowercase()}",
                serializer = ListSerializer(JikanGenreDto.serializer()),
                ttl = RemoteCache.GENRES_TTL,
            ) {
                safeApiCall {
                    if (mediaType.isAnime) jikan.getAnimeGenres() else jikan.getMangaGenres()
                }.requireData()
            }.map { genres ->
                genres.filter { it.count > 0 }
                    .map { it.toDomain() }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            }
        }

    private fun nodePager(
        mediaType: MediaType,
        loadPage: suspend (page: Int) -> JikanPagedResponse<JikanMediaDto>,
    ): Flow<PagingData<MediaNode>> = Pager(config = pagingConfig()) {
        JikanPagingSource(loadPage)
    }.flow
        .map { pagingData -> pagingData.map { it.toNode(mediaType) } }
        .flowOn(ioDispatcher)

    private fun pagingConfig() = PagingConfig(
        pageSize = JikanApiService.PAGE_SIZE,
        enablePlaceholders = false,
        initialLoadSize = JikanApiService.PAGE_SIZE,
        prefetchDistance = 3,
    )

    private companion object {
        val EMPTY_NODE = MediaNode(id = 0, mediaType = MediaType.ANIME, title = "")
    }
}
