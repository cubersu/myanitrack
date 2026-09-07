package com.myanitrack.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.myanitrack.core.common.result.AppErrorException
import com.myanitrack.core.network.jikan.dto.JikanPagedResponse
import com.myanitrack.core.network.util.toAppError
import kotlinx.coroutines.CancellationException

/**
 * Jikan-in sayfa numarali sayfalamasi icin ortak [PagingSource].
 *
 * Jikan `pagination.has_next_page` dondugu icin bir sonraki anahtari tahmin
 * etmemize gerek yok. Hiz siniri ve yeniden deneme OkHttp interceptor
 * katmaninda halledildiginden burada yalnizca sayfa mantigi var.
 *
 * @param loadPage 1-tabanli sayfa numarasi alip Jikan yanitini donduren cagri.
 */
class JikanPagingSource<T : Any>(
    private val loadPage: suspend (page: Int) -> JikanPagedResponse<T>,
) : PagingSource<Int, T>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val page = params.key ?: FIRST_PAGE
        return try {
            val response = loadPage(page)
            LoadResult.Page(
                data = response.data,
                prevKey = if (page <= FIRST_PAGE) null else page - 1,
                nextKey = if (response.pagination.hasNextPage) page + 1 else null,
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            // Hata eslemesi burada yapilir: UI katmani ham HTTP/IO istisnasi gormez,
            // LoadState.Error icinden domain hatasini okur.
            LoadResult.Error(AppErrorException(throwable.toAppError()))
        }
    }

    /**
     * Yenilemede kullanicinin bulundugu yere en yakin sayfadan basla.
     * Jikan sabit sayfa boyutu kullandigi icin komsu anahtarlardan turetmek guvenli.
     */
    override fun getRefreshKey(state: PagingState<Int, T>): Int? =
        state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor)
            page?.prevKey?.plus(1) ?: page?.nextKey?.minus(1)
        }

    private companion object {
        const val FIRST_PAGE = 1
    }
}
