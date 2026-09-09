package com.myanitrack.core.data.schedule

import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.data.cache.RemoteCache
import com.myanitrack.core.database.dao.MediaListDao
import com.myanitrack.core.database.dao.RemoteCacheDao
import com.myanitrack.core.database.entity.RemoteCacheEntity
import com.myanitrack.core.network.jikan.JikanApiService
import com.myanitrack.core.network.mal.MalApiService
import com.myanitrack.core.network.mal.dto.*
import io.mockk.*
import java.time.DayOfWeek
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScheduleRepositoryImplTest {
    @Test
    fun `official airing pages produce a cached week without seven Jikan requests`() = runTest {
        val mal = mockk<MalApiService>()
        val jikan = mockk<JikanApiService>()
        val listDao = mockk<MediaListDao>()
        val rows = mutableMapOf<String, RemoteCacheEntity>()
        val cacheDao = mockk<RemoteCacheDao>()
        coEvery { cacheDao.get(any()) } answers { rows[firstArg()] }
        coEvery { cacheDao.put(any()) } answers { firstArg<RemoteCacheEntity>().let { rows[it.cacheKey] = it }; Unit }
        every { listDao.observeByStatus(any(), any()) } returns flowOf(emptyList())
        coEvery { mal.getAnimeRanking(rankingType = "airing", limit = 100, offset = 0, fields = any()) } returns MalPagedResponse(
            data = listOf(MalListEntryDto(MalNodeDto(1, "First", broadcast = MalBroadcastDto("wednesday", "23:30")))),
            paging = MalPagingDto(next = "next"),
        )
        coEvery { mal.getAnimeRanking(rankingType = "airing", limit = 100, offset = 100, fields = any()) } returns MalPagedResponse(
            data = listOf(MalListEntryDto(MalNodeDto(2, "Second", broadcast = MalBroadcastDto("friday", "18:00")))),
        )
        val repository = ScheduleRepositoryImpl(jikan, mal, RemoteCache(cacheDao, Json), listDao, StandardTestDispatcher(testScheduler))
        val first = repository.getWeek(false) as AppResult.Success
        assertEquals("First", first.data[DayOfWeek.WEDNESDAY].single().node.title)
        assertEquals("Second", first.data[DayOfWeek.FRIDAY].single().node.title)
        assertNotNull(first.data[DayOfWeek.WEDNESDAY].single().nextAiringAt)
        assertTrue(repository.getWeek(false) is AppResult.Success)
        coVerify(exactly = 2) { mal.getAnimeRanking(any(), any(), any(), any()) }
        confirmVerified(jikan)
    }
}
