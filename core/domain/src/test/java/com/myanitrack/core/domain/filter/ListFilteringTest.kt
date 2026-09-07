package com.myanitrack.core.domain.filter

import com.myanitrack.core.domain.entry
import com.myanitrack.core.model.ListFilter
import com.myanitrack.core.model.ListSortOption
import com.myanitrack.core.model.ListStatus
import com.myanitrack.core.model.SortDirection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ListFilteringTest {

    private val entries = listOf(
        entry(id = 1, title = "Berserk", status = ListStatus.WATCHING, score = 9, progress = 5),
        entry(id = 2, title = "aria", status = ListStatus.WATCHING, score = 7, progress = 12),
        entry(id = 3, title = "Clannad", status = ListStatus.COMPLETED, score = 10, progress = 24),
        entry(id = 4, title = "Adult Show", status = ListStatus.WATCHING, nsfw = true),
        entry(id = 5, title = "Tagged", status = ListStatus.WATCHING, tags = listOf("favorite")),
    )

    @Test
    @DisplayName("Durum filtresi yalnizca o durumdaki kayitlari birakir")
    fun `filters by status`() {
        val result = entries.applyFilter(
            ListFilter(status = ListStatus.COMPLETED, hideNsfw = false),
        )
        assertEquals(listOf(3), result.map { it.id })
    }

    @Test
    @DisplayName("status = null tum durumlari getirir")
    fun `null status keeps everything`() {
        val result = entries.applyFilter(ListFilter(status = null, hideNsfw = false))
        assertEquals(entries.size, result.size)
    }

    @Test
    @DisplayName("hideNsfw yetiskin kayitlari eler")
    fun `hides nsfw entries`() {
        val result = entries.applyFilter(ListFilter(status = null, hideNsfw = true))
        assertEquals(false, result.any { it.id == 4 })
    }

    @Test
    @DisplayName("Arama buyuk-kucuk harf duyarsiz calisir")
    fun `search is case insensitive`() {
        val result = entries.applyFilter(
            ListFilter(status = null, query = "BERS", hideNsfw = false),
        )
        assertEquals(listOf(1), result.map { it.id })
    }

    @Test
    @DisplayName("Etiket filtresi yalnizca o etikete sahip kayitlari birakir")
    fun `filters by tag`() {
        val result = entries.applyFilter(
            ListFilter(status = null, tag = "favorite", hideNsfw = false),
        )
        assertEquals(listOf(5), result.map { it.id })
    }

    @Test
    @DisplayName("Baslik siralamasi buyuk-kucuk harf duyarsizdir")
    fun `sorts titles case insensitively`() {
        val result = entries.applyFilter(
            ListFilter(status = null, hideNsfw = false, sortBy = ListSortOption.TITLE),
        )
        assertEquals(listOf("Adult Show", "aria", "Berserk", "Clannad", "Tagged"), result.map { it.node.title })
    }

    @Test
    @DisplayName("Azalan siralama artan siralamanin tersidir")
    fun `descending reverses ascending`() {
        val base = ListFilter(status = null, hideNsfw = false, sortBy = ListSortOption.SCORE)
        val ascending = entries.applyFilter(base).map { it.id }
        val descending = entries
            .applyFilter(base.copy(sortDirection = SortDirection.DESCENDING))
            .map { it.id }
        assertEquals(ascending.reversed(), descending)
    }

    @Test
    @DisplayName("Populerlikte dusuk numara daha ustte cikar")
    fun `popularity sorts lower rank first`() {
        val ranked = listOf(
            entry(id = 1, title = "A", popularity = 500),
            entry(id = 2, title = "B", popularity = 10),
            entry(id = 3, title = "C", popularity = null),
        )
        val result = ranked.applyFilter(
            ListFilter(status = null, sortBy = ListSortOption.POPULARITY),
        )
        assertEquals(listOf(2, 1, 3), result.map { it.id })
    }
}
