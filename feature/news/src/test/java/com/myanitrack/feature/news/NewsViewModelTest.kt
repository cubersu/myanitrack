package com.myanitrack.feature.news

import app.cash.turbine.test
import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.domain.repository.NewsRepository
import com.myanitrack.core.model.NewsArticle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
class NewsViewModelTest {

    private val repository = mockk<NewsRepository>(relaxed = true)

    private val articles = listOf(
        NewsArticle(id = "1", title = "Haber 1", excerpt = "...", url = "https://mal/1"),
        NewsArticle(id = "2", title = "Haber 2", excerpt = "...", url = "https://mal/2"),
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        coEvery { repository.getNews(any()) } returns AppResult.Success(articles)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    @DisplayName("Acilista haberler yuklenir")
    fun `loads news`() = runTest {
        val vm = NewsViewModel(repository)
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(2, state.articles.size)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Hata durumunda liste bos kalir ve hata tasinir")
    fun `surfaces failure`() = runTest {
        coEvery { repository.getNews(any()) } returns AppResult.Failure(AppError.Network())

        val vm = NewsViewModel(repository)
        vm.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertTrue(state.articles.isEmpty())
            assertTrue(state.error is AppError.Network)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Yenileme forceRefresh ile cagrilir")
    fun `refresh forces network`() = runTest {
        val vm = NewsViewModel(repository)
        advanceUntilIdle()
        vm.refresh()
        advanceUntilIdle()

        coVerify { repository.getNews(true) }
    }
}
