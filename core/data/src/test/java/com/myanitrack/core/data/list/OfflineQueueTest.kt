package com.myanitrack.core.data.list

import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.database.dao.MediaListDao
import com.myanitrack.core.database.entity.MediaListEntryEntity
import com.myanitrack.core.database.mapper.toEntity
import com.myanitrack.core.domain.sync.PendingSyncScheduler
import com.myanitrack.core.model.ListStatus
import com.myanitrack.core.model.ListStatusUpdate
import com.myanitrack.core.model.MediaListEntry
import com.myanitrack.core.model.MediaNode
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.MyListStatus
import com.myanitrack.core.network.mal.MalApiService
import com.myanitrack.core.network.mal.dto.MalListStatusDto
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.MockKMatcherScope
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Cevrimdisi duzenleme kuyrugu.
 *
 * Kritik davranis: gecici bir hatada kullanicinin degisikligi KAYBOLMAMALI,
 * kalici bir hatada ise yerel kayit sunucudakiyle tutarli kalmali.
 */
class OfflineQueueTest {

    private val api = mockk<MalApiService>()
    private val dao = mockk<MediaListDao>(relaxUnitFun = true)
    private val scheduler = mockk<PendingSyncScheduler>(relaxUnitFun = true)
    private val dispatcher = StandardTestDispatcher()

    private fun repository() = MediaListRepositoryImpl(api, dao, scheduler, dispatcher)

    private fun httpException(code: Int) = HttpException(
        Response.error<Any>(code, "".toResponseBody("application/json".toMediaType())),
    )

    private fun entry(progress: Int = 3) = MediaListEntry(
        node = MediaNode(id = 1, mediaType = MediaType.ANIME, title = "One Piece", numEpisodes = 12),
        listStatus = MyListStatus(status = ListStatus.WATCHING, numEpisodesWatched = progress),
    )

    /**
     * `updateAnimeListStatus` on iki parametreli; her testte tek tek `any()`
     * yazmamak icin eslestirme kapsaminda tanimli kisayol.
     */
    private suspend fun MockKMatcherScope.anyUpdateCall() = api.updateAnimeListStatus(
        any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
    )

    // --- Guncelleme ---

    @Test
    @DisplayName("Ag yokken degisiklik korunur ve senkronizasyon planlanir")
    fun `network failure queues the change`() = runTest(dispatcher) {
        coEvery { dao.getEntry("ANIME", 1) } returns entry(progress = 3).toEntity()
        coEvery { anyUpdateCall() } throws IOException("offline")

        val upserts = mutableListOf<MediaListEntryEntity>()
        coEvery { dao.upsert(capture(upserts)) } just Runs

        val result = repository().updateEntry(MediaType.ANIME, 1, ListStatusUpdate(progress = 4))

        assertInstanceOf(AppResult.Success::class.java, result)
        assertEquals(1, upserts.size, "Geri alma yazmasi olmamaliydi")
        assertEquals(4, upserts.single().numEpisodesWatched)
        assertTrue(upserts.single().pendingSync)
        verify { scheduler.scheduleSync() }
    }

    @Test
    @DisplayName("Sunucu hatasi (5xx) da gecici sayilir ve kuyruga alinir")
    fun `server error queues the change`() = runTest(dispatcher) {
        coEvery { dao.getEntry("ANIME", 1) } returns entry().toEntity()
        coEvery { anyUpdateCall() } throws httpException(503)
        coEvery { dao.upsert(any()) } just Runs

        val result = repository().updateEntry(MediaType.ANIME, 1, ListStatusUpdate(progress = 4))

        assertInstanceOf(AppResult.Success::class.java, result)
        verify { scheduler.scheduleSync() }
    }

    @Test
    @DisplayName("Yetki hatasi (401) kalicidir: degisiklik geri alinir")
    fun `unauthorized reverts the change`() = runTest(dispatcher) {
        val original = entry(progress = 3).toEntity()
        coEvery { dao.getEntry("ANIME", 1) } returns original
        coEvery { anyUpdateCall() } throws httpException(401)

        val upserts = mutableListOf<MediaListEntryEntity>()
        coEvery { dao.upsert(capture(upserts)) } just Runs

        val result = repository().updateEntry(MediaType.ANIME, 1, ListStatusUpdate(progress = 4))

        assertInstanceOf(AppResult.Failure::class.java, result)
        assertEquals(listOf(4, 3), upserts.map { it.numEpisodesWatched }, "Eski kayit geri yazilmali")
        verify(exactly = 0) { scheduler.scheduleSync() }
    }

    // --- Silme ---

    @Test
    @DisplayName("Ag yokken silme pendingDelete ile isaretlenir")
    fun `network failure queues the delete`() = runTest(dispatcher) {
        coEvery { dao.getEntry("ANIME", 1) } returns entry().toEntity()
        coEvery { api.deleteAnimeListStatus(1) } throws IOException("offline")

        val upserts = mutableListOf<MediaListEntryEntity>()
        coEvery { dao.upsert(capture(upserts)) } just Runs

        val result = repository().deleteEntry(MediaType.ANIME, 1)

        assertInstanceOf(AppResult.Success::class.java, result)
        assertTrue(upserts.single().pendingDelete)
        coVerify(exactly = 0) { dao.delete(any(), any()) }
        verify { scheduler.scheduleSync() }
    }

    @Test
    @DisplayName("Kalici hatada silme geri alinir")
    fun `permanent delete failure reverts`() = runTest(dispatcher) {
        val original = entry().toEntity()
        coEvery { dao.getEntry("ANIME", 1) } returns original
        coEvery { api.deleteAnimeListStatus(1) } throws httpException(403)

        val upserts = mutableListOf<MediaListEntryEntity>()
        coEvery { dao.upsert(capture(upserts)) } just Runs

        val result = repository().deleteEntry(MediaType.ANIME, 1)

        assertInstanceOf(AppResult.Failure::class.java, result)
        assertFalse(upserts.last().pendingDelete, "Isaret geri alinmaliydi")
    }

    // --- Kuyrugu gonderme ---

    @Test
    @DisplayName("Bekleyen guncelleme tum alanlariyla gonderilir")
    fun `sync pushes full state`() = runTest(dispatcher) {
        val pending = entry(progress = 7).toEntity(pendingSync = true)
        coEvery { dao.getPending() } returns listOf(pending)
        coEvery { anyUpdateCall() } returns
            MalListStatusDto(status = "watching", numEpisodesWatched = 7)
        coEvery { dao.upsert(any()) } just Runs

        val progress = slot<Int>()
        coEvery {
            api.updateAnimeListStatus(
                any(), any(), any(), capture(progress), any(), any(),
                any(), any(), any(), any(), any(), any(),
            )
        } returns MalListStatusDto(status = "watching", numEpisodesWatched = 7)

        val result = repository().syncPendingChanges()

        assertInstanceOf(AppResult.Success::class.java, result)
        assertEquals(7, progress.captured)
    }

    @Test
    @DisplayName("Bekleyen silme MAL-e gonderilip satir kaldirilir")
    fun `sync pushes delete`() = runTest(dispatcher) {
        coEvery { dao.getPending() } returns listOf(entry().toEntity(pendingDelete = true))
        coEvery { api.deleteAnimeListStatus(1) } just Runs

        val result = repository().syncPendingChanges()

        assertInstanceOf(AppResult.Success::class.java, result)
        coVerify { dao.delete("ANIME", 1) }
    }

    @Test
    @DisplayName("Gecici hata isin tekrar denenmesi icin hata dondurur")
    fun `sync returns failure on transient error`() = runTest(dispatcher) {
        coEvery { dao.getPending() } returns listOf(entry().toEntity(pendingSync = true))
        coEvery { anyUpdateCall() } throws IOException("offline")

        val result = repository().syncPendingChanges()

        assertInstanceOf(AppError.Network::class.java, (result as AppResult.Failure).error)
        coVerify(exactly = 0) { dao.clearPendingFlags(any(), any()) }
    }

    @Test
    @DisplayName("Kalici hatada bayrak temizlenir; kuyruk tikanmaz")
    fun `sync clears flags on permanent error`() = runTest(dispatcher) {
        coEvery { dao.getPending() } returns listOf(entry().toEntity(pendingSync = true))
        coEvery { anyUpdateCall() } throws httpException(403)

        val result = repository().syncPendingChanges()

        assertInstanceOf(AppResult.Success::class.java, result)
        coVerify { dao.clearPendingFlags("ANIME", 1) }
    }

    @Test
    @DisplayName("Bekleyen kayit yoksa ag istegi yapilmaz")
    fun `sync is a no-op when queue is empty`() = runTest(dispatcher) {
        coEvery { dao.getPending() } returns emptyList()

        val result = repository().syncPendingChanges()

        assertInstanceOf(AppResult.Success::class.java, result)
        coVerify(exactly = 0) { anyUpdateCall() }
    }

    @Test
    @DisplayName("Gecici hatalarin siniflandirmasi")
    fun `classifies transient errors`() {
        assertTrue(AppError.Network().isTransient)
        assertTrue(AppError.Server(503).isTransient)
        assertTrue(AppError.RateLimited().isTransient)

        assertFalse(AppError.Unauthorized.isTransient)
        assertFalse(AppError.Forbidden.isTransient)
        assertFalse(AppError.NotFound.isTransient)
        assertFalse(AppError.Serialization().isTransient)
    }
}
