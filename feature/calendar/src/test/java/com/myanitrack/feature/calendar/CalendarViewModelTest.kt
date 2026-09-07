package com.myanitrack.feature.calendar

import app.cash.turbine.test
import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.domain.repository.ScheduleRepository
import com.myanitrack.core.model.BroadcastInfo
import com.myanitrack.core.model.MediaNode
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.ScheduleEntry
import com.myanitrack.core.model.WeeklySchedule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.DayOfWeek
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val repository = mockk<ScheduleRepository>(relaxed = true)

    private fun entry(
        id: Int,
        title: String,
        inMyList: Boolean = false,
        airingAt: Instant? = Instant.parse("2026-09-13T08:00:00Z"),
    ) = ScheduleEntry(
        node = MediaNode(id = id, mediaType = MediaType.ANIME, title = title),
        broadcast = BroadcastInfo(),
        nextAiringAt = airingAt,
        isInMyList = inMyList,
    )

    private val schedule = WeeklySchedule(
        days = mapOf(
            DayOfWeek.MONDAY to listOf(
                entry(1, "Bende Olan", inMyList = true),
                entry(2, "Bende Olmayan", airingAt = Instant.parse("2026-09-13T06:00:00Z")),
                entry(3, "Saati Bilinmeyen", airingAt = null),
            ),
            DayOfWeek.TUESDAY to listOf(entry(4, "Sali Yapimi")),
        ),
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        coEvery { repository.getWeek(any()) } returns AppResult.Success(schedule)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = CalendarViewModel(repository)

    @Test
    @DisplayName("Acilista haftalik takvim yuklenir")
    fun `loads week`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.schedule.days.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Secili gun degisince o gunun kayitlari gosterilir")
    fun `selects day`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            vm.selectDay(DayOfWeek.TUESDAY)
            advanceUntilIdle()
            assertEquals(listOf(4), expectMostRecentItem().visibleEntries.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Yalnizca listem filtresi calisir")
    fun `filters to my list`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            vm.selectDay(DayOfWeek.MONDAY)
            vm.toggleOnlyMyList()
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertTrue(state.onlyMyList)
            assertEquals(listOf(1), state.visibleEntries.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Kayitlar yayin saatine gore siralanir, saati bilinmeyenler sona duser")
    fun `sorts by airing time`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            vm.selectDay(DayOfWeek.MONDAY)
            advanceUntilIdle()
            assertEquals(listOf(2, 1, 3), expectMostRecentItem().visibleEntries.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Hata durumunda ekran bos kalir ve hata tasinir")
    fun `surfaces failure`() = runTest {
        coEvery { repository.getWeek(any()) } returns AppResult.Failure(AppError.Server(504))

        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(AppError.Server(504), state.error)
            assertTrue(state.isEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Yenileme forceRefresh ile cagrilir")
    fun `refresh forces network`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.refresh()
        advanceUntilIdle()

        coVerify { repository.getWeek(true) }
    }
}
