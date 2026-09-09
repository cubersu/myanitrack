package com.myanitrack.feature.profile

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.domain.repository.ProfileRepository
import com.myanitrack.core.model.FeedUpdate
import com.myanitrack.core.model.Friend
import com.myanitrack.core.model.HistoryEntry
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.UserProfileDetails
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
class ProfileViewModelTest {

    private val repository = mockk<ProfileRepository>(relaxed = true)
    private val listRepository = mockk<com.myanitrack.core.domain.repository.MediaListRepository>()

    private val profile = UserProfileDetails(userName = "Omer", malId = 42)
    private val history = listOf(
        HistoryEntry(1, "One Piece", MediaType.ANIME, increment = 1, date = Instant.EPOCH),
    )
    private val friends = listOf(Friend(userName = "Arkadas"))
    private val feed = listOf(
        FeedUpdate(
            userName = "Arkadas",
            malId = 21,
            title = "One Piece",
            mediaType = MediaType.ANIME,
            statusText = "Watching",
            publishedAt = Instant.EPOCH,
            url = "https://myanimelist.net/anime/21",
        ),
    )

    @BeforeEach
    fun setUp() {
        every { listRepository.observeList(any(), any()) } returns flowOf(emptyList())
        Dispatchers.setMain(StandardTestDispatcher())
        every { repository.currentUserName } returns flowOf("Omer")
        coEvery { repository.getProfile(any(), any()) } returns AppResult.Success(profile)
        coEvery { repository.getHistory(any(), any(), any()) } returns AppResult.Success(history)
        coEvery { repository.getFriends(any(), any()) } returns AppResult.Success(friends)
        coEvery { repository.getFriendsFeed(any(), any()) } returns AppResult.Success(feed)
        coEvery { repository.getUserFeed(any(), any()) } returns AppResult.Success(feed)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(userName: String? = null) = ProfileViewModel(
        profileRepository = repository,
        listRepository = listRepository,
        savedStateHandle = SavedStateHandle().apply {
            if (userName != null) set("userName", userName)
        },
    )

    @Test
    @DisplayName("Arguman yoksa oturumdaki kullanicinin profili yuklenir")
    fun `loads own profile`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals("Omer", state.userName)
            assertTrue(state.isOwnProfile)
            assertEquals(profile, state.profile)
            assertEquals(1, state.history.size)
            assertEquals(1, state.friends.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Arguman varsa baskasinin profili yuklenir")
    fun `loads other profile`() = runTest {
        val vm = viewModel(userName = "Baskasi")
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals("Baskasi", state.userName)
            assertFalse(state.isOwnProfile)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { repository.getProfile("Baskasi", false) }
    }

    @Test
    @DisplayName("Oturum yoksa ekran bos kalir, istek gonderilmez")
    fun `handles signed out`() = runTest {
        every { repository.currentUserName } returns flowOf(null)

        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertFalse(state.hasUser)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { repository.getProfile(any(), any()) }
    }

    @Test
    @DisplayName("Akis yalnizca sekmeye gelindiginde yuklenir")
    fun `feed is lazy`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        coVerify(exactly = 0) { repository.getFriendsFeed(any(), any()) }

        vm.selectTab(ProfileTab.FEED)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.getFriendsFeed("Omer", false) }
    }

    @Test
    @DisplayName("Akis bir kez yuklendikten sonra sekme degisiminde tekrar cekilmez")
    fun `feed is not reloaded`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.selectTab(ProfileTab.FEED)
        advanceUntilIdle()
        vm.selectTab(ProfileTab.OVERVIEW)
        vm.selectTab(ProfileTab.FEED)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.getFriendsFeed(any(), any()) }
    }

    @Test
    @DisplayName("Baskasinin profilinde arkadas akisi degil kendi akisi gosterilir")
    fun `other profile shows own feed`() = runTest {
        val vm = viewModel(userName = "Baskasi")
        advanceUntilIdle()

        vm.selectTab(ProfileTab.FEED)
        advanceUntilIdle()

        coVerify { repository.getUserFeed("Baskasi", false) }
        coVerify(exactly = 0) { repository.getFriendsFeed(any(), any()) }
    }

    @Test
    @DisplayName("Gecmis filtresi repository-ye iletilir")
    fun `history filter is applied`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.setHistoryFilter(MediaType.MANGA)
        advanceUntilIdle()

        coVerify { repository.getHistory("Omer", MediaType.MANGA, any()) }
    }

    @Test
    @DisplayName("Yardimci cagri hatasi profili bozmaz")
    fun `secondary failure is tolerated`() = runTest {
        coEvery { repository.getFriends(any(), any()) } returns
            AppResult.Failure(AppError.Server(504))

        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(profile, state.profile)
            assertTrue(state.friends.isEmpty())
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Profil cagrisi basarisizsa hata tasinir")
    fun `primary failure surfaces`() = runTest {
        coEvery { repository.getProfile(any(), any()) } returns
            AppResult.Failure(AppError.NotFound)

        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertNull(state.profile)
            assertEquals(AppError.NotFound, state.error)
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

        coVerify { repository.getProfile("Omer", true) }
    }
}
