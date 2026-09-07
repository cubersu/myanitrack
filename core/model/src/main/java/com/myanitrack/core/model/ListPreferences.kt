package com.myanitrack.core.model

/** Liste ekranindaki gorunum bicimi (orijinal MALClient-taki uc gorunum). */
enum class ListViewMode {
    /** Kapak agirlikli izgara. */
    GRID,

    /** Tek satirlik kompakt liste. */
    COMPACT,

    /** Kapak + puan + ilerleme + etiket iceren detayli izgara. */
    DETAILED_GRID,
}

/** Liste siralama olcutu. */
enum class ListSortOption {
    TITLE,
    SCORE,
    PROGRESS,
    LAST_UPDATED,
    START_DATE,
    MEAN_SCORE,
    POPULARITY,
}

enum class SortDirection { ASCENDING, DESCENDING }

/**
 * Liste ekraninin filtre/siralama durumu. Tamami yerel; MAL-e gonderilmez,
 * Room-dan okunan veriye uygulanir.
 */
data class ListFilter(
    val status: ListStatus? = ListStatus.WATCHING,
    val query: String = "",
    val tag: String? = null,
    val hideNsfw: Boolean = true,
    val sortBy: ListSortOption = ListSortOption.TITLE,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
) {
    val isFiltering: Boolean get() = query.isNotBlank() || tag != null
}
