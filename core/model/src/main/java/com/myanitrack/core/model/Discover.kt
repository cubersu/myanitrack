package com.myanitrack.core.model

import java.time.LocalDate

/** Anime sezonu. */
enum class SeasonName(val apiValue: String) {
    WINTER("winter"),
    SPRING("spring"),
    SUMMER("summer"),
    FALL("fall"),
    ;

    companion object {
        fun fromApi(value: String?): SeasonName? =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }

        /** Takvim ayindan sezon: 1-3 kis, 4-6 ilkbahar, 7-9 yaz, 10-12 sonbahar. */
        fun fromMonth(month: Int): SeasonName = when (month) {
            in 1..3 -> WINTER
            in 4..6 -> SPRING
            in 7..9 -> SUMMER
            else -> FALL
        }
    }
}

data class Season(val name: SeasonName, val year: Int) {

    fun next(): Season = when (name) {
        SeasonName.FALL -> Season(SeasonName.WINTER, year + 1)
        else -> Season(SeasonName.entries[name.ordinal + 1], year)
    }

    fun previous(): Season = when (name) {
        SeasonName.WINTER -> Season(SeasonName.FALL, year - 1)
        else -> Season(SeasonName.entries[name.ordinal - 1], year)
    }

    companion object {
        fun current(today: LocalDate = LocalDate.now()): Season =
            Season(SeasonName.fromMonth(today.monthValue), today.year)
    }
}

/**
 * Jikan `/top/{type}` uc noktasindaki filtreler.
 *
 * Anime ve manga icin gecerli filtreler farkli: anime tarafinda "airing",
 * manga tarafinda "publishing". [isAvailableFor] bunu tek yerde tutar.
 */
enum class TopCategory(val apiValue: String?) {
    ALL(null),
    AIRING("airing"),
    PUBLISHING("publishing"),
    UPCOMING("upcoming"),
    BY_POPULARITY("bypopularity"),
    FAVORITE("favorite"),
    ;

    fun isAvailableFor(mediaType: MediaType): Boolean = when (this) {
        AIRING -> mediaType.isAnime
        PUBLISHING -> mediaType.isManga
        else -> true
    }

    companion object {
        fun availableFor(mediaType: MediaType): List<TopCategory> =
            entries.filter { it.isAvailableFor(mediaType) }
    }
}

/** Arama ve gezinme sorgusu. */
data class DiscoverQuery(
    val mediaType: MediaType = MediaType.ANIME,
    val text: String = "",
    val genreId: Int? = null,
    val producerId: Int? = null,
    val orderBy: String? = null,
    val includeNsfw: Boolean = false,
) {
    val isEmpty: Boolean
        get() = text.isBlank() && genreId == null && producerId == null
}
