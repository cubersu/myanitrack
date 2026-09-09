package com.myanitrack.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.myanitrack.core.common.result.AppErrorException
import com.myanitrack.core.model.MediaNode
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.network.mal.MalApiService
import com.myanitrack.core.network.mal.mapper.toDomain
import com.myanitrack.core.network.util.toAppError
import kotlinx.coroutines.CancellationException

/**
 * Resmi MAL API v2 uzerinden arama sayfalamasi.
 * Jikan erisilemez oldugunda fallback olarak kullanilir.
 */
class MalSearchPagingSource(
    private val malApi: MalApiService,
    private val query: String,
    private val mediaType: MediaType,
    private val nsfw: Boolean,
) : PagingSource<Int, MediaNode>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaNode> {
        val offset = params.key ?: 0
        return try {
            val response = if (mediaType.isAnime) {
                malApi.searchAnime(query = query, offset = offset, limit = params.loadSize, nsfw = nsfw)
            } else {
                malApi.searchManga(query = query, offset = offset, limit = params.loadSize, nsfw = nsfw)
            }

            val nodes = response.data.map { it.node.toDomain(mediaType) }
            
            LoadResult.Page(
                data = nodes,
                prevKey = if (offset == 0) null else (offset - params.loadSize).coerceAtLeast(0),
                nextKey = if (response.paging.next != null) offset + params.loadSize else null
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            LoadResult.Error(AppErrorException(throwable.toAppError()))
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MediaNode>): Int? {
        return state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor)
            page?.prevKey?.plus(state.config.pageSize) ?: page?.nextKey?.minus(state.config.pageSize)
        }
    }
}
