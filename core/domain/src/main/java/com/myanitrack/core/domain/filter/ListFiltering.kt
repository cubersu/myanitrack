package com.myanitrack.core.domain.filter

import com.myanitrack.core.model.ListFilter
import com.myanitrack.core.model.ListSortOption
import com.myanitrack.core.model.MediaListEntry
import com.myanitrack.core.model.SortDirection

/**
 * Liste filtreleme ve siralama kurallari.
 *
 * MAL API-si bu kadar zengin filtreleme sunmadigi ve liste zaten tumuyle yerelde
 * durdugu icin islem bellekte yapiliyor. Saf fonksiyon oldugundan dogrudan
 * birim testi yazilabiliyor.
 */
fun List<MediaListEntry>.applyFilter(filter: ListFilter): List<MediaListEntry> =
    asSequence()
        .filter { filter.status == null || it.listStatus.status == filter.status }
        .filter { !filter.hideNsfw || !it.node.nsfw }
        .filter { filter.tag == null || filter.tag in it.listStatus.tags }
        .filter { entry -> filter.query.isBlank() || entry.matchesQuery(filter.query) }
        .sortedWith(filter.comparator())
        .toList()

private fun MediaListEntry.matchesQuery(query: String): Boolean {
    val needle = query.trim()
    return node.title.contains(needle, ignoreCase = true) ||
        node.englishTitle?.contains(needle, ignoreCase = true) == true ||
        node.japaneseTitle?.contains(needle, ignoreCase = true) == true
}

private fun ListFilter.comparator(): Comparator<MediaListEntry> {
    val base: Comparator<MediaListEntry> = when (sortBy) {
        ListSortOption.TITLE ->
            compareBy(String.CASE_INSENSITIVE_ORDER) { it.node.title }

        ListSortOption.SCORE ->
            compareBy { it.listStatus.score }

        ListSortOption.PROGRESS ->
            compareBy { it.progress }

        ListSortOption.LAST_UPDATED ->
            compareBy { it.listStatus.updatedAt?.epochSecond ?: Long.MIN_VALUE }

        ListSortOption.START_DATE ->
            compareBy { it.node.startDate?.toEpochDay() ?: Long.MIN_VALUE }

        ListSortOption.MEAN_SCORE ->
            compareBy { it.node.meanScore ?: 0.0 }

        ListSortOption.POPULARITY ->
            // Dusuk siralama numarasi = daha populer.
            compareBy { it.node.popularity ?: Int.MAX_VALUE }
    }

    // Esitlikte her zaman baslik; boylece siralama kararli ve tekrarlanabilir olur.
    val stable = base.thenBy(String.CASE_INSENSITIVE_ORDER) { it.node.title }
    return if (sortDirection == SortDirection.ASCENDING) stable else stable.reversed()
}
