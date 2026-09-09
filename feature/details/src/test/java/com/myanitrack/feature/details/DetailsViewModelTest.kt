package com.myanitrack.feature.details

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.domain.repository.MediaDetailsRepository
import com.myanitrack.core.domain.repository.MediaListRepository
import com.myanitrack.core.domain.usecase.UpdateListEntryUseCase
import com.myanitrack.core.model.CharacterSummary
import com.myanitrack.core.model.ListStatus
import com.myanitrack.core.model.ListStatusUpdate
import com.myanitrack.core.model.MediaDetails
import com.myanitrack.core.model.MediaListEntry
import com.myanitrack.core.model.MediaNode
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.MyListStatus
import com.myanitrack.core.model.PromoVideo
import com.myanitrack.feature.details.navigation.MediaDetailsRoute
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {

    private val detailsRepository = mockk<MediaDetailsRepository>(relaxed = true)
    private val listRepository = mockk<MediaListRepository>(relaxed = true)
    private val entryFlow = MutableStateFlow<MediaListEntry?>(null)

    private val details = MediaDetails(
        id = 5114,
        mediaType = MediaType.ANIME,
        title = "Fullmetal Alchemist: Brotherhood",
        numEpisodes = 64,
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        every { listRepository.observeEntry(any(), any()) } returns entryFlow
        every { detailsRepository.reviewsPager(any(), any()) } returns emptyFlow()
        coEvery { detailsRepository.getDetails(any(), any(), any()) } returns
            AppResult.Success(details)
        coEvery { detailsRepository.getCharacters(any(), any()) } returns
            AppResult.Success(listOf(CharacterSummary(id = 1, name = "Edward")))
        coEvery { detailsRepository.getStaff(any(), any()) } returns AppResult.Success(emptyList())
        coEvery { detailsRepository.getRecommendations(any(), any()) } returns
            AppResult.Success(emptyList())
        coEvery { detailsRepository.getPromoVideos(any(), any()) } returns
            AppResult.Success(listOf(PromoVideo(title = "PV", youtubeId = "abc")))
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = DetailsViewModel(
        detailsRepository = detailsRepository,
        listRepository = listRepository,
        updateListEntry = UpdateListEntryUseCase(listRepository),
        savedStateHandle = SavedStateHandle().apply {
            set("mediaTypeName", MediaType.ANIME.name)
            set("malId", 5114)
        },
    )

    private fun listEntry(status: ListStatus = ListStatus.WATCHING) = MediaListEntry(
        node = MediaNode(id = 5114, mediaType = MediaType.ANIME, title = details.title),
        listStatus = MyListStatus(status = status),
    )

    @Test
    fun `main content appears while optional sections are still waiting`() = runTest {
        val pending = CompletableDeferred<AppResult<List<CharacterSummary>>>()
        coEvery { detailsRepository.getCharacters(any(), any()) } coAnswers { pending.await() }
        val vm = viewModel()
        vm.uiState.test {
            runCurrent()
            val state = expectMostRecentItem()
            assertEquals(details.title, state.details?.title)
            assertFalse(state.isLoading)
            assertTrue(state.characters.isEmpty())
            pending.complete(AppResult.Success(listOf(CharacterSummary(id = 1, name = "Edward"))))
            runCurrent()
            assertEquals("Edward", expectMostRecentItem().characters.single().name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Tum bolumler paralel yuklenip tek durumda birlestirilir")
    fun `loads all sections`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(details.title, state.details?.title)
            assertEquals(1, state.characters.size)
            assertEquals(1, state.videos.size)
            assertFalse(state.isLoading)
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Yardimci bolum hatasi sayfayi bozmaz")
    fun `secondary section failure is tolerated`() = runTest {
        coEvery { detailsRepository.getCharacters(any(), any()) } returns
            AppResult.Failure(AppError.Server(504))

        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(details.title, state.details?.title, "Ana detay yine gosterilmeli")
            assertTrue(state.characters.isEmpty())
            assertNull(state.error, "Yardimci bolum hatasi ekrani hataya dusurmemeli")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Ana detay hatasi durum ve olay olarak bildirilir")
    fun `primary failure surfaces`() = runTest {
        coEvery { detailsRepository.getDetails(any(), any(), any()) } returns
            AppResult.Failure(AppError.Network())

        val vm = viewModel()
        vm.events.test {
            advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is DetailsEvent.ShowError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Listeye ekleme secilen durumla istek gonderir")
    fun `adds to list`() = runTest {
        val update = slot<ListStatusUpdate>()
        coEvery {
            listRepository.updateEntry(any(), any(), capture(update))
        } returns AppResult.Success(listEntry())

        val vm = viewModel()
        vm.addToList(ListStatus.PLAN_TO_WATCH)
        advanceUntilIdle()

        assertEquals(ListStatus.PLAN_TO_WATCH, update.captured.status)
    }

    @Test
    @DisplayName("Listedeki kayit durumu akistan gelir")
    fun `list entry comes from repository flow`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            assertFalse(expectMostRecentItem().isInList)

            entryFlow.value = listEntry()
            advanceUntilIdle()
            assertTrue(expectMostRecentItem().isInList)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Bos guncelleme ag istegi olusturmaz")
    fun `empty update is skipped`() = runTest {
        val vm = viewModel()
        vm.saveEdit(ListStatusUpdate())
        advanceUntilIdle()

        coVerify(exactly = 0) { listRepository.updateEntry(any(), any(), any()) }
    }

    @Test
    @DisplayName("Listeden cikarma silme cagrisi yapar")
    fun `removes from list`() = runTest {
        coEvery { listRepository.deleteEntry(any(), any()) } returns AppResult.Success(Unit)

        val vm = viewModel()
        vm.removeFromList()
        advanceUntilIdle()

        coVerify { listRepository.deleteEntry(MediaType.ANIME, 5114) }
    }

    @Test
    @DisplayName("Yenileme forceRefresh ile cagrilir")
    fun `refresh forces network`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.refresh()
        advanceUntilIdle()

        coVerify { detailsRepository.getDetails(MediaType.ANIME, 5114, true) }
    }
}
