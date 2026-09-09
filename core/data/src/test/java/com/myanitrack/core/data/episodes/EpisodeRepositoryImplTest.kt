package com.myanitrack.core.data.episodes

import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.data.cache.RemoteCache
import com.myanitrack.core.database.dao.RemoteCacheDao
import com.myanitrack.core.database.entity.RemoteCacheEntity
import com.myanitrack.core.network.mal.MalWebService
import io.mockk.*
import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EpisodeRepositoryImplTest {
    private fun row(number: Int, date: String, replies: Int = 0) = """
        <table><tr class="episode-list-data"><td class="episode-number nowrap" data-raw="$number">$number</td>
        <td class="episode-aired nowrap">$date</td><td class="episode-forum" data-raw="$replies"></td></tr></table>
    """

    @Test
    fun `parses dated releases without counting future or undated placeholders`() {
        val html = row(1, "Jul 5, 2026") + row(10, "Sep 6, 2026") + row(11, "Sep 13, 2026") + row(13, "-")
        assertEquals(10, parseEpisodePage(html, Instant.parse("2026-09-09T00:00:00Z")).latestReleased)
        assertEquals(12, parseEpisodePage(row(12, "-", 25), Instant.now()).latestReleased)
        assertNull(parseEpisodePage("<html>Service unavailable</html>", Instant.now()).latestReleased)
    }

    @Test
    fun `long series reads first and last pages and caches the result`() = runTest {
        val web = mockk<MalWebService>()
        val rows = mutableMapOf<String, RemoteCacheEntity>()
        val dao = mockk<RemoteCacheDao>()
        coEvery { dao.get(any()) } answers { rows[firstArg()] }
        coEvery { dao.put(any()) } answers { firstArg<RemoteCacheEntity>().let { rows[it.cacheKey] = it } }
        coEvery { web.getEpisodes(21, 0) } returns row(1, "Oct 20, 1999") + "<a href='/anime/21/One_Piece/episode?offset=1100'>Last</a>"
        coEvery { web.getEpisodes(21, 1100) } returns row(1140, "Aug 1, 2025")
        val repo = EpisodeRepositoryImpl(web, RemoteCache(dao, Json))
        repeat(2) { assertEquals(1140, (repo.getReleasedEpisodeCount(21) as AppResult.Success).data) }
        coVerify(exactly = 1) { web.getEpisodes(21, 0) }
        coVerify(exactly = 1) { web.getEpisodes(21, 1100) }
        confirmVerified(web)
    }
}
