package com.myanitrack.core.data.profile

import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.data.cache.RemoteCache
import com.myanitrack.core.database.dao.RemoteCacheDao
import com.myanitrack.core.database.entity.RemoteCacheEntity
import com.myanitrack.core.datastore.auth.AuthSessionStore
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.network.jikan.JikanApiService
import com.myanitrack.core.network.jikan.dto.JikanFriendDto
import com.myanitrack.core.network.jikan.dto.JikanPagedResponse
import com.myanitrack.core.network.jikan.dto.JikanResponse
import com.myanitrack.core.network.jikan.dto.JikanUserMetaDto
import com.myanitrack.core.network.jikan.dto.JikanUserProfileDto
import com.myanitrack.core.network.rss.MalRssService
import com.myanitrack.core.network.rss.RssParser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ProfileRepositoryImplTest {

    private class FakeCacheDao : RemoteCacheDao {
        val entries = mutableMapOf<String, RemoteCacheEntity>()
        override suspend fun get(key: String) = entries[key]
        override suspend fun put(entry: RemoteCacheEntity) { entries[entry.cacheKey] = entry }
        override suspend fun delete(key: String) { entries.remove(key) }
        override suspend fun deleteOlderThan(threshold: Long) = Unit
        override suspend fun clear() = entries.clear()
        override suspend fun approximateSizeBytes() = 0L
    }

    private val jikan = mockk<JikanApiService>()
    private val mal = mockk<com.myanitrack.core.network.mal.MalApiService>()
    private val rssService = mockk<MalRssService>()
    private val sessionStore = mockk<AuthSessionStore>(relaxed = true)
    private val dispatcher = StandardTestDispatcher()

    private fun repository() = ProfileRepositoryImpl(
        jikan = jikan,
        mal = mal,
        rssService = rssService,
        rssParser = RssParser(),
        cache = RemoteCache(FakeCacheDao(), Json { ignoreUnknownKeys = true }),
        sessionStore = sessionStore,
        ioDispatcher = dispatcher,
    )

    private fun friend(name: String) =
        JikanFriendDto(user = JikanUserMetaDto(username = name))

    private fun feedXml(vararg titles: String) = """
        <?xml version="1.0" encoding="utf-8"?>
        <rss version="2.0"><channel>
        ${titles.joinToString("\n") { title ->
        """
          <item>
            <title>$title - TV</title>
            <link>https://myanimelist.net/anime/${title.hashCode().and(0xFFFF)}/x</link>
            <description><![CDATA[Watching - 1 of 12 episodes]]></description>
            <pubDate>Mon, 19 Apr 2021 14:44:42 -0700</pubDate>
          </item>
        """
    }}
        </channel></rss>
    """.trimIndent()

    @Test
    fun `official avatar replaces missing Jikan photo while preserving manga statistics`() = runTest(dispatcher) {
        coEvery { sessionStore.current() } returns com.myanitrack.core.model.AuthSession(
            accessToken = "test", refreshToken = "test", expiresAt = java.time.Instant.MAX, userName = "Omer",
        )
        coEvery { jikan.getUserProfile("Omer") } returns JikanResponse(JikanUserProfileDto(
            malId = 42, username = "Omer",
            statistics = com.myanitrack.core.network.jikan.dto.JikanUserStatisticsDto(
                manga = com.myanitrack.core.network.jikan.dto.JikanMangaStatsDto(completed = 5),
            ),
        ))
        coEvery { mal.getMyUser() } returns com.myanitrack.core.network.mal.dto.MalUserDto(
            id = 42, name = "Omer", picture = "https://cdn.myanimelist.net/images/userimages/42.jpg",
        )
        val profile = (repository().getProfile("Omer") as AppResult.Success).data
        assertEquals("https://cdn.myanimelist.net/images/userimages/42.jpg", profile.imageUrl)
        assertEquals(5, profile.mangaStats?.completed)
    }

    @Test
    fun `own profile uses official API when Jikan fails`() = runTest(dispatcher) {
        coEvery { sessionStore.current() } returns com.myanitrack.core.model.AuthSession(
            accessToken = "test", refreshToken = "test", expiresAt = java.time.Instant.MAX, userName = "Omer",
        )
        coEvery { jikan.getUserProfile("Omer") } throws IOException("upstream unavailable")
        coEvery { mal.getMyUser() } returns com.myanitrack.core.network.mal.dto.MalUserDto(id = 42, name = "Omer")
        val result = repository().getProfile("Omer") as AppResult.Success
        assertEquals("Omer", result.data.userName)
        assertEquals(42, result.data.malId)
    }

    @Test
    fun `other profile must not fall back to the signed in user`() = runTest(dispatcher) {
        coEvery { sessionStore.current() } returns com.myanitrack.core.model.AuthSession(
            accessToken = "test", refreshToken = "test", expiresAt = java.time.Instant.MAX, userName = "Omer",
        )
        coEvery { jikan.getUserProfile("SomeoneElse") } throws IOException("upstream unavailable")
        assertInstanceOf(AppResult.Failure::class.java, repository().getProfile("SomeoneElse"))
        coVerify(exactly = 0) { mal.getMyUser() }
    }

    @Test
    @DisplayName("Profil Jikan-dan alinip domain modeline cevrilir")
    fun `maps profile`() = runTest(dispatcher) {
        coEvery { jikan.getUserProfile("Omer") } returns
            JikanResponse(JikanUserProfileDto(malId = 42, username = "Omer", location = "TR"))

        val result = repository().getProfile("Omer")

        val profile = (result as AppResult.Success).data
        assertEquals("Omer", profile.userName)
        assertEquals(42, profile.malId)
        assertEquals("TR", profile.location)
    }

    @Test
    @DisplayName("Arkadas akisi butun arkadaslarin beslemesini birlestirir")
    fun `merges friends feeds`() = runTest(dispatcher) {
        coEvery { jikan.getUserFriends("Omer", any()) } returns
            JikanPagedResponse(data = listOf(friend("A"), friend("B")))
        coEvery { rssService.getUserFeed(any(), "A") } returns feedXml("Anime A")
        coEvery { rssService.getUserFeed(any(), "B") } returns feedXml("Anime B")

        val result = repository().getFriendsFeed("Omer")

        val updates = (result as AppResult.Success).data
        // Iki arkadas x iki besleme turu (anime + manga) = 4 oge
        assertEquals(4, updates.size)
        assertTrue(updates.any { it.userName == "A" })
        assertTrue(updates.any { it.userName == "B" })
    }

    @Test
    @DisplayName("Bir arkadasin beslemesi alinamazsa digerleri gosterilir")
    fun `skips failing friend feed`() = runTest(dispatcher) {
        coEvery { jikan.getUserFriends("Omer", any()) } returns
            JikanPagedResponse(data = listOf(friend("A"), friend("B")))
        coEvery { rssService.getUserFeed(any(), "A") } throws IOException("offline")
        coEvery { rssService.getUserFeed(any(), "B") } returns feedXml("Anime B")

        val updates = (repository().getFriendsFeed("Omer") as AppResult.Success).data

        assertTrue(updates.isNotEmpty(), "B-nin beslemesi yine gelmeliydi")
        assertTrue(updates.all { it.userName == "B" })
    }

    @Test
    @DisplayName("Arkadas listesi alinamazsa akis hatasi dondurulur")
    fun `propagates friends failure`() = runTest(dispatcher) {
        coEvery { jikan.getUserFriends("Omer", any()) } throws IOException("offline")

        val result = repository().getFriendsFeed("Omer")

        assertInstanceOf(AppError.Network::class.java, (result as AppResult.Failure).error)
    }

    @Test
    @DisplayName("Arkadasi olmayan kullanicida ag istegi yapilmaz")
    fun `no friends means no feed requests`() = runTest(dispatcher) {
        coEvery { jikan.getUserFriends("Omer", any()) } returns JikanPagedResponse(data = emptyList())

        val updates = (repository().getFriendsFeed("Omer") as AppResult.Success).data

        assertTrue(updates.isEmpty())
        coVerify(exactly = 0) { rssService.getUserFeed(any(), any()) }
    }

    @Test
    @DisplayName("Kullanici akisi anime ve manga beslemelerini birlestirir")
    fun `user feed combines both types`() = runTest(dispatcher) {
        coEvery { rssService.getUserFeed("rw", "Omer") } returns feedXml("Anime")
        coEvery { rssService.getUserFeed("rm", "Omer") } returns feedXml("Manga")

        val updates = (repository().getUserFeed("Omer") as AppResult.Success).data

        assertEquals(2, updates.size)
        coVerify { rssService.getUserFeed("rw", "Omer") }
        coVerify { rssService.getUserFeed("rm", "Omer") }
    }

    @Test
    @DisplayName("Gecmiste taninmayan tur atlanir")
    fun `history skips unknown types`() = runTest(dispatcher) {
        coEvery { jikan.getUserHistory("Omer", null) } returns JikanResponse(
            listOf(
                com.myanitrack.core.network.jikan.dto.JikanHistoryDto(
                    entry = com.myanitrack.core.network.jikan.dto.JikanMalUrlDto(
                        malId = 21,
                        type = "anime",
                        name = "One Piece",
                    ),
                    increment = 2,
                    date = "2026-09-01T10:00:00+00:00",
                ),
                com.myanitrack.core.network.jikan.dto.JikanHistoryDto(
                    entry = com.myanitrack.core.network.jikan.dto.JikanMalUrlDto(
                        malId = 5,
                        type = "character",
                        name = "Bilinmeyen",
                    ),
                    increment = 1,
                ),
            ),
        )

        val history = (repository().getHistory("Omer") as AppResult.Success).data

        assertEquals(1, history.size)
        assertEquals(MediaType.ANIME, history.first().mediaType)
        assertEquals(2, history.first().increment)
    }
}
