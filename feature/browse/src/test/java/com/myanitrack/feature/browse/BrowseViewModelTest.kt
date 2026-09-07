package com.myanitrack.feature.browse

import app.cash.turbine.test
import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.domain.repository.DiscoverRepository
import com.myanitrack.core.domain.repository.UserPreferencesRepository
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.NamedRef
import com.myanitrack.core.model.Season
import com.myanitrack.core.model.SeasonName
import com.myanitrack.core.model.TopCategory
import com.myanitrack.core.model.UserPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
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
class BrowseViewModelTest {

    private val discoverRepository = mockk<DiscoverRepository>(relaxed = true)
    private val preferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)

    private val animeGenres = listOf(NamedRef(1, "Action"), NamedRef(2, "Drama"))

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        every { preferencesRepository.preferences } returns flowOf(UserPreferences())
        every { discoverRepository.topPager(any(), any()) } returns emptyFlow()
        every { discoverRepository.seasonPager(any(), any()) } returns emptyFlow()
        every { discoverRepository.searchPager(any()) } returns emptyFlow()
        every { discoverRepository.recommendationPager(any()) } returns emptyFlow()
        coEvery { discoverRepository.getGenres(MediaType.ANIME) } returns
            AppResult.Success(animeGenres)
        coEvery { discoverRepository.getGenres(MediaType.MANGA) } returns
            AppResult.Success(listOf(NamedRef(3, "Seinen")))
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = BrowseViewModel(discoverRepository, preferencesRepository)

    @Test
    @DisplayName("Acilista tercihlerden medya turu ve turler yuklenir")
    fun `loads defaults`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(MediaType.ANIME, state.mediaType)
            assertEquals(animeGenres, state.genres)
            assertFalse(state.includeNsfw, "hideNsfw varsayilani true oldugundan NSFW kapali olmali")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Manga secilince anime-ye ozgu filtre sifirlanir")
    fun `resets airing filter for manga`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            vm.selectTopCategory(TopCategory.AIRING)
            advanceUntilIdle()
            assertEquals(TopCategory.AIRING, expectMostRecentItem().topCategory)

            vm.selectMediaType(MediaType.MANGA)
            advanceUntilIdle()
            assertEquals(
                TopCategory.ALL,
                expectMostRecentItem().topCategory,
                "AIRING manga tarafinda gecersiz, ALL-a donmeliydi",
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Manga secilince sezon sekmesinden cikilir")
    fun `leaves season tab for manga`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            vm.selectTab(BrowseTab.SEASON)
            advanceUntilIdle()

            vm.selectMediaType(MediaType.MANGA)
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(BrowseTab.TOP, state.tab)
            assertFalse(state.isSeasonTabAvailable)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Medya turu degisince o turun turleri yeniden cekilir")
    fun `reloads genres on media type change`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            vm.selectMediaType(MediaType.MANGA)
            advanceUntilIdle()
            assertEquals(listOf("Seinen"), expectMostRecentItem().genres.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { discoverRepository.getGenres(MediaType.MANGA) }
    }

    @Test
    @DisplayName("Tur listesi alinamazsa ekran calismaya devam eder")
    fun `survives genre failure`() = runTest {
        coEvery { discoverRepository.getGenres(any()) } returns
            AppResult.Failure(AppError.Server(504))

        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            assertTrue(expectMostRecentItem().genres.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Ayni ture tekrar basmak secimi kaldirir")
    fun `toggles genre selection`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            vm.selectGenre(animeGenres.first())
            advanceUntilIdle()
            assertEquals(1, expectMostRecentItem().selectedGenre?.id)

            vm.selectGenre(animeGenres.first())
            advanceUntilIdle()
            assertNull(expectMostRecentItem().selectedGenre)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Sezon ileri/geri gezinmesi yil sinirini dogru asar")
    fun `season navigation wraps years`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            advanceUntilIdle()
            val start = expectMostRecentItem().season
            vm.nextSeason()
            advanceUntilIdle()
            assertEquals(start.next(), expectMostRecentItem().season)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Sonbahardan sonra kis bir sonraki yildir")
    fun `fall rolls into next winter`() {
        val fall = Season(SeasonName.FALL, 2026)
        assertEquals(Season(SeasonName.WINTER, 2027), fall.next())
        assertEquals(Season(SeasonName.FALL, 2025), Season(SeasonName.WINTER, 2026).previous())
    }

    @Test
    @DisplayName("Manga tarafinda AIRING yerine PUBLISHING sunulur")
    fun `top categories differ per media type`() {
        val animeCategories = TopCategory.availableFor(MediaType.ANIME)
        val mangaCategories = TopCategory.availableFor(MediaType.MANGA)

        assertTrue(TopCategory.AIRING in animeCategories)
        assertFalse(TopCategory.PUBLISHING in animeCategories)
        assertTrue(TopCategory.PUBLISHING in mangaCategories)
        assertFalse(TopCategory.AIRING in mangaCategories)
    }
}
