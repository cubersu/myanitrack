package com.myanitrack.feature.mylist

import app.cash.turbine.test
import com.myanitrack.core.common.network.NetworkMonitor
import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.domain.repository.MediaListRepository
import com.myanitrack.core.domain.repository.UserPreferencesRepository
import com.myanitrack.core.domain.usecase.IncrementProgressUseCase
import com.myanitrack.core.domain.usecase.ObserveMyListUseCase
import com.myanitrack.core.domain.usecase.RefreshMyListUseCase
import com.myanitrack.core.domain.usecase.UpdateListEntryUseCase
import com.myanitrack.core.model.ListFilter
import com.myanitrack.core.model.ListStatus
import com.myanitrack.core.model.ListStatusUpdate
import com.myanitrack.core.model.ListViewMode
import com.myanitrack.core.model.MediaListEntry
import com.myanitrack.core.model.MediaNode
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.MyListStatus
import com.myanitrack.core.model.UserPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class MyListViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcher = MainDispatcherExtension()
    }

    private val listRepository = mockk<MediaListRepository>(relaxed = true)
    private val preferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
    private val entriesFlow = MutableStateFlow(listOf(entry(1, progress = 3)))
    private val networkMonitor = mockk<NetworkMonitor>()
    private val isOnline = MutableStateFlow(true)

    private fun entry(id: Int, progress: Int = 0, total: Int? = 12) = MediaListEntry(
        node = MediaNode(
            id = id,
            mediaType = MediaType.ANIME,
            title = "Title $id",
            numEpisodes = total,
        ),
        listStatus = MyListStatus(status = ListStatus.WATCHING, numEpisodesWatched = progress),
    )

    @BeforeEach
    fun setUp() {
        every { preferencesRepository.preferences } returns flowOf(UserPreferences())
        every { listRepository.observeList(any(), any()) } returns entriesFlow
        every { listRepository.observeStatusCounts(any()) } returns
            flowOf(mapOf(ListStatus.WATCHING to 1))
        every { listRepository.observeTags(any()) } returns flowOf(listOf("favorite"))
        coEvery { listRepository.refresh(any()) } returns AppResult.Success(Unit)
        every { listRepository.observePendingSyncCount() } returns MutableStateFlow(0)
        every { networkMonitor.isOnline } returns isOnline
    }

    private fun viewModel() = MyListViewModel(
        observeMyList = ObserveMyListUseCase(listRepository),
        refreshMyList = RefreshMyListUseCase(listRepository),
        updateListEntry = UpdateListEntryUseCase(listRepository),
        incrementProgressUseCase = IncrementProgressUseCase(UpdateListEntryUseCase(listRepository)),
        listRepository = listRepository,
        preferencesRepository = preferencesRepository,
        networkMonitor = networkMonitor,
    )

    @Test
    @DisplayName("Acilista onbellekteki liste, sayimlar ve etiketler yayinlanir")
    fun `emits list data`() = runTest {
        viewModel().uiState.test {
            skipItems(1) // baslangic degeri
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(listOf(1), state.entries.map { it.id })
            assertEquals(1, state.statusCounts[ListStatus.WATCHING])
            assertEquals(listOf("favorite"), state.availableTags)
            assertFalse(state.isInitialLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Acilista onbellek varsa tam senkronizasyon yine de tetiklenir")
    fun `refreshes on start`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(atLeast = 1) { listRepository.refresh(MediaType.ANIME) }
    }

    @Test
    @DisplayName("Durum sekmesi degisince filtre guncellenir")
    fun `changes status filter`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            vm.selectStatus(ListStatus.COMPLETED)
            advanceUntilIdle()
            assertEquals(ListStatus.COMPLETED, expectMostRecentItem().filter.status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Aramayi kapatmak sorguyu temizler")
    fun `closing search clears query`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            vm.setSearchActive(true)
            vm.setQuery("berserk")
            advanceUntilIdle()
            assertEquals("berserk", expectMostRecentItem().filter.query)

            vm.setSearchActive(false)
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals("", state.filter.query)
            assertFalse(state.isSearchActive)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("+1 dugmesi ilerlemeyi bir artiran guncelleme gonderir")
    fun `increment sends update`() = runTest {
        val update = slot<ListStatusUpdate>()
        coEvery {
            listRepository.updateEntry(any(), any(), capture(update))
        } returns AppResult.Success(entry(1, progress = 4))

        val vm = viewModel()
        vm.incrementProgress(entry(1, progress = 3))
        advanceUntilIdle()

        assertEquals(4, update.captured.progress)
    }

    @Test
    @DisplayName("Son bolume gelmis kayitta +1 istek gondermez")
    fun `increment is a no-op at the end`() = runTest {
        val vm = viewModel()
        vm.incrementProgress(entry(1, progress = 12, total = 12))
        advanceUntilIdle()

        coVerify(exactly = 0) { listRepository.updateEntry(any(), any(), any()) }
    }

    @Test
    @DisplayName("Guncelleme hatasi kullaniciya olay olarak bildirilir")
    fun `emits error event on failed update`() = runTest {
        coEvery { listRepository.updateEntry(any(), any(), any()) } returns
            AppResult.Failure(AppError.Unauthorized)

        val vm = viewModel()
        vm.events.test {
            vm.incrementProgress(entry(1, progress = 3))
            advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is MyListEvent.ShowError)
            assertEquals(AppError.Unauthorized, (event as MyListEvent.ShowError).error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Kayit silinince bildirim olayi yayinlanir")
    fun `emits deleted event`() = runTest {
        coEvery { listRepository.deleteEntry(any(), any()) } returns AppResult.Success(Unit)

        val vm = viewModel()
        vm.events.test {
            vm.deleteEntry(entry(1))
            advanceUntilIdle()
            assertEquals(MyListEvent.EntryDeleted("Title 1"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Bos guncelleme ag istegi olusturmaz")
    fun `empty update does not hit the network`() = runTest {
        val vm = viewModel()
        vm.saveEdit(entry(1), ListStatusUpdate())
        advanceUntilIdle()

        coVerify(exactly = 0) { listRepository.updateEntry(any(), any(), any()) }
    }

    @Test
    @DisplayName("Gorunum modu tercihe yazilir")
    fun `view mode is persisted`() = runTest {
        val vm = viewModel()
        vm.setViewMode(ListViewMode.COMPACT)
        advanceUntilIdle()

        coVerify { preferencesRepository.setListViewMode(ListViewMode.COMPACT) }
    }

    @Test
    @DisplayName("Siralama tercihi kaydedilir ve filtreye yansir")
    fun `sort preference flows into filter`() = runTest {
        every { preferencesRepository.preferences } returns
            flowOf(UserPreferences(hideNsfw = false))

        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            assertFalse(expectMostRecentItem().filter.hideNsfw)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Cevrimdisi durumu ve bekleyen degisiklik sayisi duruma yansir")
    fun `reports offline and pending state`() = runTest {
        every { listRepository.observePendingSyncCount() } returns MutableStateFlow(2)
        isOnline.value = false

        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertTrue(state.isOffline)
            assertEquals(2, state.pendingSyncCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Filtre repository-ye oldugu gibi iletilir")
    fun `passes filter to repository`() = runTest {
        val filter = slot<ListFilter>()
        every { listRepository.observeList(any(), capture(filter)) } returns entriesFlow

        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            vm.selectTag("favorite")
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("favorite", filter.captured.tag)
    }
}
