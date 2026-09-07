package com.myanitrack.core.domain.usecase

import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.domain.entry
import com.myanitrack.core.domain.repository.MediaListRepository
import com.myanitrack.core.model.ListStatus
import com.myanitrack.core.model.ListStatusUpdate
import com.myanitrack.core.model.MediaListEntry
import com.myanitrack.core.model.MediaType
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class IncrementProgressUseCaseTest {

    private val repository = mockk<MediaListRepository>()
    private val useCase = IncrementProgressUseCase(UpdateListEntryUseCase(repository))
    private val today = LocalDate.of(2026, 9, 7)

    @Test
    @DisplayName("Ilerleme bir artar")
    fun `increments progress by one`() = runTest {
        val captured = slot<ListStatusUpdate>()
        coEvery {
            repository.updateEntry(any(), any(), capture(captured))
        } returns AppResult.Success(mockk(relaxed = true))

        useCase(entry(progress = 3, total = 12), today)

        assertEquals(4, captured.captured.progress)
        assertNull(captured.captured.status)
    }

    @Test
    @DisplayName("Son bolumde durum otomatik Completed olur ve bitis tarihi atanir")
    fun `completes on last episode`() = runTest {
        val captured = slot<ListStatusUpdate>()
        coEvery {
            repository.updateEntry(any(), any(), capture(captured))
        } returns AppResult.Success(mockk(relaxed = true))

        useCase(entry(progress = 11, total = 12), today)

        assertEquals(12, captured.captured.progress)
        assertEquals(ListStatus.COMPLETED, captured.captured.status)
        assertEquals(today, captured.captured.finishDate)
    }

    @Test
    @DisplayName("Plan to watch listesinden ilk bolum isaretlenince Watching-e gecer")
    fun `moves from plan to watching`() = runTest {
        val captured = slot<ListStatusUpdate>()
        coEvery {
            repository.updateEntry(any(), any(), capture(captured))
        } returns AppResult.Success(mockk(relaxed = true))

        useCase(entry(status = ListStatus.PLAN_TO_WATCH, progress = 0, total = 24), today)

        assertEquals(ListStatus.WATCHING, captured.captured.status)
        assertEquals(today, captured.captured.startDate)
    }

    @Test
    @DisplayName("Toplam bilinmiyorsa ilerleme sinirsiz artar ve durum degismez")
    fun `handles unknown total`() = runTest {
        val captured = slot<ListStatusUpdate>()
        coEvery {
            repository.updateEntry(any(), any(), capture(captured))
        } returns AppResult.Success(mockk(relaxed = true))

        useCase(entry(progress = 900, total = null), today)

        assertEquals(901, captured.captured.progress)
        assertNull(captured.captured.status)
    }

    @Test
    @DisplayName("Puan gecerli araliga sikistirilir")
    fun `clamps score`() = runTest {
        val captured = slot<ListStatusUpdate>()
        coEvery {
            repository.updateEntry(any(), any(), capture(captured))
        } returns AppResult.Success(mockk(relaxed = true))

        UpdateListEntryUseCase(repository)(
            MediaType.ANIME,
            1,
            ListStatusUpdate(score = 42, progress = -5),
        )

        assertEquals(10, captured.captured.score)
        assertEquals(0, captured.captured.progress)
        assertTrue(captured.isCaptured)
    }
}
