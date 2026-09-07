package com.myanitrack.core.data.paging

import androidx.paging.PagingSource
import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppErrorException
import com.myanitrack.core.common.result.asAppError
import com.myanitrack.core.network.jikan.dto.JikanPagedResponse
import com.myanitrack.core.network.jikan.dto.JikanPaginationDto
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response

class JikanPagingSourceTest {

    private fun page(
        items: List<String>,
        hasNext: Boolean,
    ) = JikanPagedResponse(data = items, pagination = JikanPaginationDto(hasNextPage = hasNext))

    private fun refresh(key: Int?) = PagingSource.LoadParams.Refresh(
        key = key,
        loadSize = 25,
        placeholdersEnabled = false,
    )

    @Test
    @DisplayName("Ilk yukleme 1. sayfayi ister ve onceki anahtar bos olur")
    fun `first load starts at page one`() = runTest {
        var requestedPage = -1
        val source = JikanPagingSource<String> { page ->
            requestedPage = page
            page(listOf("a", "b"), hasNext = true)
        }

        val result = source.load(refresh(null)) as PagingSource.LoadResult.Page

        assertEquals(1, requestedPage)
        assertEquals(listOf("a", "b"), result.data)
        assertNull(result.prevKey)
        assertEquals(2, result.nextKey)
    }

    @Test
    @DisplayName("has_next_page false ise sonraki anahtar uretilmez")
    fun `stops when there is no next page`() = runTest {
        val source = JikanPagingSource<String> { page(listOf("a"), hasNext = false) }

        val result = source.load(refresh(3)) as PagingSource.LoadResult.Page

        assertEquals(2, result.prevKey)
        assertNull(result.nextKey)
    }

    @Test
    @DisplayName("Ag hatasi domain hatasi olarak sarilir")
    fun `wraps network error`() = runTest {
        val source = JikanPagingSource<String> { throw IOException("offline") }

        val result = source.load(refresh(null)) as PagingSource.LoadResult.Error

        assertInstanceOf(AppErrorException::class.java, result.throwable)
        assertInstanceOf(AppError.Network::class.java, result.throwable.asAppError())
    }

    @Test
    @DisplayName("429 RateLimited hatasina cevrilir")
    fun `maps rate limit error`() = runTest {
        val source = JikanPagingSource<String> {
            throw HttpException(
                Response.error<Any>(429, "".toResponseBody("application/json".toMediaType())),
            )
        }

        val result = source.load(refresh(null)) as PagingSource.LoadResult.Error

        assertInstanceOf(AppError.RateLimited::class.java, result.throwable.asAppError())
    }

    @Test
    @DisplayName("504 Server hatasina cevrilir (Jikan MAL-a ulasamiyor)")
    fun `maps gateway error`() = runTest {
        val source = JikanPagingSource<String> {
            throw HttpException(
                Response.error<Any>(504, "".toResponseBody("application/json".toMediaType())),
            )
        }

        val error = (source.load(refresh(null)) as PagingSource.LoadResult.Error)
            .throwable.asAppError()

        assertEquals(AppError.Server(504), error)
    }
}
