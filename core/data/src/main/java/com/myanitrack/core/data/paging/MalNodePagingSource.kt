package com.myanitrack.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.myanitrack.core.common.result.AppErrorException
import com.myanitrack.core.model.MediaNode
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.network.mal.dto.MalListEntryDto
import com.myanitrack.core.network.mal.dto.MalPagedResponse
import com.myanitrack.core.network.mal.mapper.toDomain
import com.myanitrack.core.network.util.toAppError
import kotlinx.coroutines.CancellationException

/** Fixed page size keeps offsets correct when Paging requests a larger initial load. */
internal class MalNodePagingSource(
    private val mediaType: MediaType,
    private val loadPage: suspend (offset: Int, limit: Int) -> MalPagedResponse<MalListEntryDto>,
) : PagingSource<Int, MediaNode>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaNode> = try {
        val offset = params.key ?: 0
        val response = loadPage(offset, PAGE_SIZE)
        LoadResult.Page(
            data = response.data.map { it.node.toDomain(mediaType) },
            prevKey = if (offset == 0) null else (offset - PAGE_SIZE).coerceAtLeast(0),
            nextKey = if (response.paging.next != null) offset + PAGE_SIZE else null,
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (exception: Exception) {
        LoadResult.Error(AppErrorException(exception.toAppError()))
    }

    override fun getRefreshKey(state: PagingState<Int, MediaNode>): Int? =
        state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.let { page ->
                page.prevKey?.plus(PAGE_SIZE) ?: page.nextKey?.minus(PAGE_SIZE)
            }
        }

    private companion object { const val PAGE_SIZE = 25 }
}
