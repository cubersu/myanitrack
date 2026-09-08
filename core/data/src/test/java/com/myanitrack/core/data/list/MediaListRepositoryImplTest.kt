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
import com.myanitrack.core.network.mal.dto.MalListEntryDto
import com.myanitrack.core.network.mal.dto.MalListStatusDto
import com.myanitrack.core.network.mal.dto.MalNodeDto
import com.myanitrack.core.network.mal.dto.MalPagedResponse
import com.myanitrack.core.network.mal.dto.MalPagingDto
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import java.io.IOException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response

class MediaListRepositoryImplTest {

    private val api = mockk<MalApiService>()
    private val dao = mockk<MediaListDao>(relaxUnitFun = true)
    private val dispatcher = StandardTestDispatcher()

    private val scheduler = mockk<PendingSyncScheduler>(relaxUnitFun = true)

    private fun repository() = MediaListRepositoryImpl(api, dao, scheduler, dispatcher)

    private fun httpException(code: Int) = HttpException(
        Response.error<Any>(code, "".toResponseBody("application/json".toMediaType())),
    )

    private fun dto(id: Int, status: String = "watching") = MalListEntryDto(
        node = MalNodeDto(id = id, title = "Title $id", numEpisodes = 12),
        listStatus = MalListStatusDto(status = status, numEpisodesWatched = 3),
    )

    private fun localEntry(id: Int = 1, progress: Int = 3) = MediaListEntry(
        node = MediaNode(
            id = id,
            mediaType = MediaType.ANIME,
            title = "Title $id",
            numEpisodes = 12,
        ),
        listStatus = MyListStatus(status = ListStatus.WATCHING, numEpisodesWatched = progress),
    )

    @Test
    @DisplayName("refresh butun sayfalari toplayip tabloyu tek islemde degistirir")
    fun `refresh follows paging`() = runTest(dispatcher) {
        coEvery { api.getMyAnimeList(any(), any(), any(), any(), any(), any()) } returns
            MalPagedResponse(
                data = listOf(dto(1), dto(2)),
                paging = MalPagingDto(next = "https://api.myanimelist.net/v2/next"),
            )
        coEvery { api.getListPage(any()) } returns
            MalPagedResponse(data = listOf(dto(3)), paging = MalPagingDto())

        val captured = slot<List<MediaListEntryEntity>>()
        coEvery { dao.replaceAll(any(), capture(captured)) } just Runs

        val result = repository().refresh(MediaType.ANIME)

        assertInstanceOf(AppResult.Success::class.java, result)
        assertEquals(listOf(1, 2, 3), captured.captured.map { it.malId })
    }

    @Test
    @DisplayName("Liste durumu olmayan kayitlar onbellege yazilmaz")
    fun `refresh skips entries without list status`() = runTest(dispatcher) {
        coEvery { api.getMyAnimeList(any(), any(), any(), any(), any(), any()) } returns
            MalPagedResponse(
                data = listOf(dto(1), MalListEntryDto(node = MalNodeDto(id = 2), listStatus = null)),
                paging = MalPagingDto(),
            )
        val captured = slot<List<MediaListEntryEntity>>()
        coEvery { dao.replaceAll(any(), capture(captured)) } just Runs

        repository().refresh(MediaType.ANIME)

        assertEquals(listOf(1), captured.captured.map { it.malId })
    }

    @Test
    @DisplayName("Ag hatasi Network hatasina cevrilir ve onbellek silinmez")
    fun `refresh maps network failure`() = runTest(dispatcher) {
        coEvery { api.getMyAnimeList(any(), any(), any(), any(), any(), any()) } throws IOException()

        val result = repository().refresh(MediaType.ANIME)

        assertInstanceOf(AppError.Network::class.java, (result as AppResult.Failure).error)
        coVerify(exactly = 0) { dao.replaceAll(any(), any()) }
    }

    @Test
    @DisplayName("401 Unauthorized hatasina cevrilir")
    fun `refresh maps 401`() = runTest(dispatcher) {
        coEvery { api.getMyAnimeList(any(), any(), any(), any(), any(), any()) } throws
            httpException(401)

        val result = repository().refresh(MediaType.ANIME)

        assertEquals(AppError.Unauthorized, (result as AppResult.Failure).error)
    }

    @Test
    @DisplayName("Guncelleme once yerelde uygulanir, KALICI hata olursa geri alinir")
    fun `update reverts optimistic write on permanent failure`() = runTest(dispatcher) {
        val existing = localEntry(progress = 3).toEntity()
        coEvery { dao.getEntry("ANIME", 1) } returns existing
        // 403 kalicidir: tekrar denemek duzeltmez, degisiklik geri alinmali.
        // Gecici hatalarin kuyruga alinmasi OfflineQueueTest icinde test ediliyor.
        coEvery {
            api.updateAnimeListStatus(
                any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(),
            )
        } throws httpException(403)

        val upserts = mutableListOf<MediaListEntryEntity>()
        coEvery { dao.upsert(capture(upserts)) } just Runs

        val result = repository().updateEntry(
            MediaType.ANIME,
            1,
            ListStatusUpdate(progress = 4),
        )

        assertInstanceOf(AppResult.Failure::class.java, result)
        // Once iyimser kayit (4), sonra eski kayit (3) geri yazilir.
        assertEquals(listOf(4, 3), upserts.map { it.numEpisodesWatched })
        assertTrue(upserts.first().pendingSync)
    }

    @Test
    @DisplayName("Basarili guncellemede sunucudan donen durum onbellege yazilir")
    fun `update stores server response`() = runTest(dispatcher) {
        coEvery { dao.getEntry("ANIME", 1) } returns localEntry(progress = 3).toEntity()
        coEvery {
            api.updateAnimeListStatus(
                any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(),
            )
        } returns MalListStatusDto(status = "completed", score = 9, numEpisodesWatched = 12)

        val upserts = mutableListOf<MediaListEntryEntity>()
        coEvery { dao.upsert(capture(upserts)) } just Runs

        val result = repository().updateEntry(MediaType.ANIME, 1, ListStatusUpdate(progress = 12))

        val entry = (result as AppResult.Success).data
        assertEquals(ListStatus.COMPLETED, entry.listStatus.status)
        assertEquals(9, entry.listStatus.score)
        assertEquals(false, upserts.last().pendingSync)
    }

    @Test
    @DisplayName("MAL tarafinda zaten silinmis kayit (404) yerelde de silinir")
    fun `delete treats 404 as success`() = runTest(dispatcher) {
        coEvery { dao.getEntry("ANIME", 1) } returns localEntry().toEntity()
        coEvery { api.deleteAnimeListStatus(1) } throws httpException(404)

        val result = repository().deleteEntry(MediaType.ANIME, 1)

        assertInstanceOf(AppResult.Success::class.java, result)
        coVerify(exactly = 1) { dao.delete("ANIME", 1) }
    }

    @Test
    @DisplayName("Silme sirasindaki KALICI hatada kayit geri yuklenir")
    fun `delete restores entry on permanent failure`() = runTest(dispatcher) {
        val existing = localEntry().toEntity()
        coEvery { dao.getEntry("ANIME", 1) } returns existing
        // Gecici hatada silme kuyruga alinir; o yol OfflineQueueTest icinde.
        coEvery { api.deleteAnimeListStatus(1) } throws httpException(403)

        val result = repository().deleteEntry(MediaType.ANIME, 1)

        assertInstanceOf(AppResult.Failure::class.java, result)
        coVerify(exactly = 1) { dao.upsert(existing) }
    }
}
