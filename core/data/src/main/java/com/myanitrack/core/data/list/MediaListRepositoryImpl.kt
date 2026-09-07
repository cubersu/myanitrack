package com.myanitrack.core.data.list

import com.myanitrack.core.common.di.AppDispatcher
import com.myanitrack.core.common.di.Dispatcher
import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.database.dao.MediaListDao
import com.myanitrack.core.database.entity.MediaListEntryEntity
import com.myanitrack.core.database.mapper.toDomain
import com.myanitrack.core.database.mapper.toEntity
import com.myanitrack.core.domain.filter.applyFilter
import com.myanitrack.core.domain.repository.MediaListRepository
import com.myanitrack.core.model.ListFilter
import com.myanitrack.core.model.ListStatus
import com.myanitrack.core.model.ListStatusUpdate
import com.myanitrack.core.model.MediaListEntry
import com.myanitrack.core.model.MediaNode
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.network.mal.MalApiService
import com.myanitrack.core.network.mal.dto.MalListEntryDto
import com.myanitrack.core.network.mal.dto.MalListStatusDto
import com.myanitrack.core.network.mal.mapper.toDomain
import com.myanitrack.core.network.mal.mapper.toMalDateOrNull
import com.myanitrack.core.network.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class MediaListRepositoryImpl @Inject constructor(
    private val apiService: MalApiService,
    private val dao: MediaListDao,
    @Dispatcher(AppDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : MediaListRepository {

    override fun observeList(
        mediaType: MediaType,
        filter: ListFilter,
    ): Flow<List<MediaListEntry>> = dao.observeAll(mediaType.name)
        .map { entities -> entities.map { it.toDomain() }.applyFilter(filter) }
        .flowOn(ioDispatcher)

    override fun observeEntry(mediaType: MediaType, malId: Int): Flow<MediaListEntry?> =
        dao.observeEntry(mediaType.name, malId)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)

    override fun observeTags(mediaType: MediaType): Flow<List<String>> =
        dao.observeAll(mediaType.name)
            .map { entities ->
                entities.flatMap { it.tags }
                    .distinct()
                    .sortedWith(String.CASE_INSENSITIVE_ORDER)
            }
            .flowOn(ioDispatcher)

    override fun observeStatusCounts(mediaType: MediaType): Flow<Map<ListStatus, Int>> =
        dao.observeAll(mediaType.name)
            .map { entities ->
                entities.groupingBy { entity ->
                    ListStatus.entries.firstOrNull { it.name == entity.listStatus }
                        ?: ListStatus.WATCHING
                }.eachCount()
            }
            .flowOn(ioDispatcher)

    /**
     * Listenin tamamini ceker ve yerel tabloyu tazeler.
     *
     * MAL sayfa basina en fazla 1000 kayit dondugu icin `paging.next` takip
     * edilerek tum sayfalar toplanir, sonra tek islemde tabloya yazilir.
     */
    override suspend fun refresh(mediaType: MediaType): AppResult<Unit> = withContext(ioDispatcher) {
        when (val fetched = safeApiCall { fetchAllPages(mediaType) }) {
            is AppResult.Failure -> fetched
            is AppResult.Success -> {
                val entries = fetched.data.mapNotNull { it.toDomain(mediaType)?.toEntity() }
                runCatching { dao.replaceAll(mediaType.name, entries) }.fold(
                    onSuccess = { AppResult.Success(Unit) },
                    onFailure = { AppResult.Failure(AppError.Storage(it)) },
                )
            }
        }
    }

    private suspend fun fetchAllPages(mediaType: MediaType): List<MalListEntryDto> {
        val collected = mutableListOf<MalListEntryDto>()
        var page = if (mediaType.isAnime) {
            apiService.getMyAnimeList(offset = 0)
        } else {
            apiService.getMyMangaList(offset = 0)
        }
        collected += page.data
        var next = page.paging.next
        var guard = 0
        while (next != null && guard++ < MAX_PAGES) {
            page = apiService.getListPage(next)
            collected += page.data
            next = page.paging.next
        }
        return collected
    }

    /**
     * Iyimser guncelleme: once yerel kayit degistirilir (UI aninda tepki verir),
     * sonra MAL-e gonderilir. Istek basarisiz olursa onceki kayit geri yazilir.
     */
    override suspend fun updateEntry(
        mediaType: MediaType,
        malId: Int,
        update: ListStatusUpdate,
    ): AppResult<MediaListEntry> = withContext(ioDispatcher) {
        val existing = dao.getEntry(mediaType.name, malId)
        existing?.toDomain()
            ?.let { entry -> entry.copy(listStatus = update.applyTo(entry.listStatus, mediaType)) }
            ?.let { dao.upsert(it.toEntity(pendingSync = true)) }

        when (val response = safeApiCall { sendUpdate(mediaType, malId, update) }) {
            is AppResult.Failure -> {
                existing?.let { dao.upsert(it) }
                response
            }

            is AppResult.Success -> {
                val serverStatus = response.data.toDomain(mediaType)
                    ?: return@withContext AppResult.Failure(AppError.Serialization("my_list_status"))

                val node = existing?.toDomain()?.node
                    ?: when (val fetched = fetchNode(mediaType, malId)) {
                        is AppResult.Failure -> return@withContext fetched
                        is AppResult.Success -> fetched.data
                    }

                val entry = MediaListEntry(node = node, listStatus = serverStatus)
                dao.upsert(entry.toEntity())
                AppResult.Success(entry)
            }
        }
    }

    private suspend fun sendUpdate(
        mediaType: MediaType,
        malId: Int,
        update: ListStatusUpdate,
    ): MalListStatusDto {
        val status = update.status?.apiValue(mediaType)
        val tags = update.tags?.joinToString(",")
        // MAL tarih alanini bosaltmak icin bos dize bekliyor; null "degistirme" demek.
        val startDate = if (update.clearStartDate) "" else update.startDate.toMalDateOrNull()
        val finishDate = if (update.clearFinishDate) "" else update.finishDate.toMalDateOrNull()

        return if (mediaType.isAnime) {
            apiService.updateAnimeListStatus(
                id = malId,
                status = status,
                score = update.score,
                numWatchedEpisodes = update.progress,
                isRewatching = update.isRepeating,
                numTimesRewatched = update.numTimesRepeated,
                rewatchValue = update.repeatValue,
                priority = update.priority,
                startDate = startDate,
                finishDate = finishDate,
                tags = tags,
                comments = update.comments,
            )
        } else {
            apiService.updateMangaListStatus(
                id = malId,
                status = status,
                score = update.score,
                numChaptersRead = update.progress,
                numVolumesRead = update.volumesRead,
                isRereading = update.isRepeating,
                numTimesReread = update.numTimesRepeated,
                rereadValue = update.repeatValue,
                priority = update.priority,
                startDate = startDate,
                finishDate = finishDate,
                tags = tags,
                comments = update.comments,
            )
        }
    }

    /** Listede olmayan bir yapim eklendiginde kapak/baslik bilgisini cekmek icin. */
    private suspend fun fetchNode(mediaType: MediaType, malId: Int): AppResult<MediaNode> =
        when (
            val result = safeApiCall {
                if (mediaType.isAnime) apiService.getAnime(malId) else apiService.getManga(malId)
            }
        ) {
            is AppResult.Failure -> result
            is AppResult.Success -> AppResult.Success(result.data.toDomain(mediaType))
        }

    override suspend fun deleteEntry(mediaType: MediaType, malId: Int): AppResult<Unit> =
        withContext(ioDispatcher) {
            val existing: MediaListEntryEntity? = dao.getEntry(mediaType.name, malId)
            dao.delete(mediaType.name, malId)

            val response = safeApiCall {
                if (mediaType.isAnime) {
                    apiService.deleteAnimeListStatus(malId)
                } else {
                    apiService.deleteMangaListStatus(malId)
                }
            }
            when (response) {
                is AppResult.Failure ->
                    // 404 = kayit MAL tarafinda zaten yok; yereli silmis olmamiz dogru.
                    if (response.error is AppError.NotFound) {
                        AppResult.Success(Unit)
                    } else {
                        existing?.let { dao.upsert(it) }
                        response
                    }

                is AppResult.Success -> AppResult.Success(Unit)
            }
        }

    private companion object {
        /** Sonsuz sayfalama dongusune karsi guvenlik siniri (200 x 1000 kayit). */
        const val MAX_PAGES = 200
    }
}
