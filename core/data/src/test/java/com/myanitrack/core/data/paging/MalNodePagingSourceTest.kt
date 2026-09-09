package com.myanitrack.core.data.paging

import androidx.paging.PagingSource
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.network.mal.dto.MalListEntryDto
import com.myanitrack.core.network.mal.dto.MalNodeDto
import com.myanitrack.core.network.mal.dto.MalPagedResponse
import com.myanitrack.core.network.mal.dto.MalPagingDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MalNodePagingSourceTest {
    @Test
    fun `different load sizes do not skip or duplicate offsets`() = runTest {
        val requests = mutableListOf<Pair<Int, Int>>()
        val source = MalNodePagingSource(MediaType.ANIME) { offset, limit ->
            requests += offset to limit
            MalPagedResponse(
                data = (offset until offset + limit).map { MalListEntryDto(MalNodeDto(it, "Anime $it")) },
                paging = MalPagingDto(next = "next"),
            )
        }
        val first = source.load(PagingSource.LoadParams.Refresh(null, 75, false)) as PagingSource.LoadResult.Page
        val second = source.load(PagingSource.LoadParams.Append(first.nextKey!!, 25, false)) as PagingSource.LoadResult.Page
        assertEquals(listOf(0 to 25, 25 to 25), requests)
        assertEquals((0 until 50).toList(), (first.data + second.data).map { it.id })
        assertNull(first.prevKey)
    }

    @Test
    fun `last page does not request another page`() = runTest {
        val source = MalNodePagingSource(MediaType.MANGA) { _, _ ->
            MalPagedResponse(data = listOf(MalListEntryDto(MalNodeDto(1, "Manga"))))
        }
        val page = source.load(PagingSource.LoadParams.Refresh(null, 25, false)) as PagingSource.LoadResult.Page
        assertNull(page.nextKey)
        assertEquals(MediaType.MANGA, page.data.single().mediaType)
    }

    @Test
    fun `cancellation is propagated`() = runTest {
        val source = MalNodePagingSource(MediaType.ANIME) { _, _ -> throw CancellationException() }
        try {
            source.load(PagingSource.LoadParams.Refresh(null, 25, false))
            fail<Unit>("Cancellation must not become a paging error")
        } catch (_: CancellationException) { }
    }
}
