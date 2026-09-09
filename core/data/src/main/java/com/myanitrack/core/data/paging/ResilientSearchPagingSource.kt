package com.myanitrack.core.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.myanitrack.core.common.result.AppErrorException
import com.myanitrack.core.model.DiscoverQuery
import com.myanitrack.core.model.MediaNode
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.network.jikan.JikanApiService
import com.myanitrack.core.network.jikan.mapper.toNode
import com.myanitrack.core.network.mal.MalApiService
import com.myanitrack.core.network.mal.mapper.toDomain
import com.myanitrack.core.network.util.toAppError
import kotlinx.coroutines.CancellationException

/**
 * Jikan ve MAL API'lerini birlikte kullanan dayanikli arama kaynagi.
 * Jikan basarisiz olursa (zaman asimi vb.) resmi MAL API'sine gecer.
 */
class ResilientSearchPagingSource(
    private val jikan: JikanApiService,
    private val mal: MalApiService,
    private val query: DiscoverQuery,
) : PagingSource<Int, MediaNode>() {

    private var useFallback = false

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaNode> {
        val page = params.key ?: 1
        
        // Jikan 1-tabanli sayfa kullanir, MAL 0-tabanli offset kullanir.
        val offset = (page - 1) * params.loadSize

        if (!useFallback) {
            try {
                val response = if (query.mediaType.isAnime) {
                    jikan.searchAnime(
                        query = query.text.takeIf { it.isNotBlank() },
                        page = page,
                        genres = query.genreId,
                        producers = query.producerId,
                        orderBy = query.orderBy,
                        safeForWork = !query.includeNsfw,
                    )
                } else {
                    jikan.searchManga(
                        query = query.text.takeIf { it.isNotBlank() },
                        page = page,
                        genres = query.genreId,
                        orderBy = query.orderBy,
                        safeForWork = !query.includeNsfw,
                    )
                }
                
                return LoadResult.Page(
                    data = response.data.map { it.toNode(query.mediaType) },
                    prevKey = if (page <= 1) null else page - 1,
                    nextKey = if (response.pagination.hasNextPage) page + 1 else null
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Jikan hataliysa veya filtreler MAL tarafindan desteklenmiyorsa fallback'e gec.
                // (MAL API sadece metin aramasinda iyi fallback verir).
                if (query.text.isNotBlank() && query.genreId == null && query.producerId == null) {
                    useFallback = true
                } else {
                    return LoadResult.Error(AppErrorException(e.toAppError()))
                }
            }
        }

        // Fallback: Resmi MAL API v2
        return try {
            val response = if (query.mediaType.isAnime) {
                mal.searchAnime(query = query.text, offset = offset, limit = params.loadSize, nsfw = query.includeNsfw)
            } else {
                mal.searchManga(query = query.text, offset = offset, limit = params.loadSize, nsfw = query.includeNsfw)
            }
            
            LoadResult.Page(
                data = response.data.map { it.node.toDomain(query.mediaType) },
                prevKey = if (page <= 1) null else page - 1,
                nextKey = if (response.paging.next != null) page + 1 else null
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            LoadResult.Error(AppErrorException(e.toAppError()))
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MediaNode>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }
}
