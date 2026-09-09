package com.myanitrack.core.data.schedule

import com.myanitrack.core.common.di.AppDispatcher
import com.myanitrack.core.common.di.Dispatcher
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.common.result.map
import com.myanitrack.core.data.cache.RemoteCache
import com.myanitrack.core.database.dao.MediaListDao
import com.myanitrack.core.domain.repository.ScheduleRepository
import com.myanitrack.core.domain.schedule.NextEpisodeCalculator
import com.myanitrack.core.model.ListStatus
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.ScheduleEntry
import com.myanitrack.core.model.WeeklySchedule
import com.myanitrack.core.network.jikan.JikanApiService
import com.myanitrack.core.network.jikan.dto.JikanMediaDto
import com.myanitrack.core.network.jikan.mapper.toDomain
import com.myanitrack.core.network.jikan.mapper.toNode
import com.myanitrack.core.network.mal.MalApiService
import com.myanitrack.core.network.mal.MalFields
import com.myanitrack.core.network.mal.dto.MalListEntryDto
import com.myanitrack.core.network.mal.mapper.toBroadcastInfo
import com.myanitrack.core.network.mal.mapper.toDomain as toMalNode
import com.myanitrack.core.network.util.safeApiCall
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val jikan: JikanApiService,
    private val mal: MalApiService,
    private val cache: RemoteCache,
    private val mediaListDao: MediaListDao,
    @Dispatcher(AppDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : ScheduleRepository {

    override suspend fun getDay(
        day: DayOfWeek,
        forceRefresh: Boolean,
    ): AppResult<List<ScheduleEntry>> = withContext(ioDispatcher) {
        val watchingIds = watchingAnimeIds()
        cache.cachedCall(
            key = cacheKey(day),
            serializer = ListSerializer(JikanMediaDto.serializer()),
            ttl = SCHEDULE_TTL,
            forceRefresh = forceRefresh,
        ) {
            safeApiCall { jikan.getSchedule(day = day.apiValue()) }.let { result ->
                when (result) {
                    is AppResult.Failure -> result
                    is AppResult.Success -> AppResult.Success(result.data.data)
                }
            }
        }.map { dtos -> dtos.map { it.toScheduleEntry(watchingIds) } }
    }

    /**
     * Haftanin tamami. Gunler SIRAYLA cekilir - paralel gonderilse hepsi hiz
     * sinirlayicida kuyruga girecegi icin kazanc olmaz, ama onbellekten gelen
     * gunler aninda doner.
     *
     * Bir gun alinamazsa o gun bos kalir, digerleri gosterilir; takvim tumden
     * kaybolmaz. Hicbir gun alinamazsa ilk hata dondurulur.
     */
    override suspend fun getWeek(forceRefresh: Boolean): AppResult<WeeklySchedule> =
        withContext(ioDispatcher) {
            // A single cached MAL listing covers the week, including ongoing older shows.
            // Jikan previously required seven failing requests before displaying anything.
            val official = cache.cachedCall(
                key = "mal:schedule:airing",
                serializer = ListSerializer(MalListEntryDto.serializer()),
                ttl = SCHEDULE_TTL,
                forceRefresh = forceRefresh,
            ) {
                safeApiCall {
                    val entries = mutableListOf<MalListEntryDto>()
                    var offset = 0
                    do {
                        val page = mal.getAnimeRanking(
                            rankingType = "airing", limit = 100, offset = offset,
                            fields = "${MalFields.ANIME_LIST},broadcast",
                        )
                        entries += page.data
                        offset += 100
                    } while (page.paging.next != null)
                    entries.toList()
                }
            }
            if (official is AppResult.Success) {
                val watchingIds = watchingAnimeIds()
                val entries = official.data.map { entry ->
                    val broadcast = entry.node.broadcast.toBroadcastInfo()
                    ScheduleEntry(
                        node = entry.node.toMalNode(MediaType.ANIME),
                        broadcast = broadcast,
                        nextAiringAt = NextEpisodeCalculator.nextAiring(broadcast, Instant.now()),
                        isInMyList = entry.node.id in watchingIds,
                    )
                }
                return@withContext AppResult.Success(WeeklySchedule(
                    DayOfWeek.entries.associateWith { day -> entries.filter { it.broadcast.day == day } },
                ))
            }
            val days = mutableMapOf<DayOfWeek, List<ScheduleEntry>>()
            var firstFailure: AppResult.Failure? = null

            DayOfWeek.entries.forEach { day ->
                when (val result = getDay(day, forceRefresh)) {
                    is AppResult.Success -> days[day] = result.data
                    is AppResult.Failure -> {
                        days[day] = emptyList()
                        if (firstFailure == null) firstFailure = result
                    }
                }
            }

            if (days.values.all { it.isEmpty() }) {
                firstFailure ?: AppResult.Success(WeeklySchedule(days))
            } else {
                AppResult.Success(WeeklySchedule(days))
            }
        }

    override suspend fun getUpcomingForMyList(): AppResult<List<ScheduleEntry>> =
        withContext(ioDispatcher) {
            val watchingIds = watchingAnimeIds()
            if (watchingIds.isEmpty()) return@withContext AppResult.Success(emptyList())

            val week = when (val result = getWeek()) {
                is AppResult.Failure -> return@withContext result
                is AppResult.Success -> result.data
            }

            val upcoming = week.days.values
                .flatten()
                .filter { it.node.id in watchingIds }
                .filter { it.nextAiringAt != null }
                .sortedBy { it.nextAiringAt }

            AppResult.Success(upcoming)
        }

    /** Onbellekteki listeden izlemede olan anime kimlikleri; hata olursa bos kume. */
    private suspend fun watchingAnimeIds(): Set<Int> = runCatching {
        mediaListDao.observeByStatus(MediaType.ANIME.name, ListStatus.WATCHING.name)
            .first()
            .map { it.malId }
            .toSet()
    }.getOrDefault(emptySet())

    private fun JikanMediaDto.toScheduleEntry(watchingIds: Set<Int>): ScheduleEntry {
        val broadcastInfo = broadcast.toDomain()
        return ScheduleEntry(
            node = toNode(MediaType.ANIME),
            broadcast = broadcastInfo,
            nextAiringAt = NextEpisodeCalculator.nextAiring(broadcastInfo, Instant.now()),
            isInMyList = malId in watchingIds,
        )
    }

    private fun DayOfWeek.apiValue(): String = name.lowercase()

    private fun cacheKey(day: DayOfWeek) = "jikan:schedule:${day.apiValue()}"

    private companion object {
        /** Takvim gun icinde nadiren degisir; 12 saat yeterince taze. */
        val SCHEDULE_TTL: Duration = Duration.ofHours(12)
    }
}
