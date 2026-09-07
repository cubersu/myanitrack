package com.myanitrack.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * Kullanicinin bir yapim icin tuttugu liste kaydi.
 *
 * MAL API v2-deki `my_list_status` alaninin domain karsiligi. Tum yazma islemleri
 * (durum, puan, sayaclar, etiketler, tarihler) bu model uzerinden yapilir.
 */
data class MyListStatus(
    val status: ListStatus,
    val score: Int = 0,
    val numEpisodesWatched: Int = 0,
    val numChaptersRead: Int = 0,
    val numVolumesRead: Int = 0,
    val isRepeating: Boolean = false,
    val numTimesRepeated: Int = 0,
    val repeatValue: Int = 0,
    val priority: Int = 0,
    val startDate: LocalDate? = null,
    val finishDate: LocalDate? = null,
    val tags: List<String> = emptyList(),
    val comments: String = "",
    val updatedAt: Instant? = null,
) {
    /** Anime icin izlenen bolum, manga icin okunan bolum sayisi. */
    fun progress(mediaType: MediaType): Int =
        if (mediaType.isAnime) numEpisodesWatched else numChaptersRead

    fun withProgress(mediaType: MediaType, value: Int): MyListStatus =
        if (mediaType.isAnime) copy(numEpisodesWatched = value) else copy(numChaptersRead = value)

    companion object {
        /** Puan yok = 0; MAL puan araligi 1..10. */
        const val SCORE_NOT_RATED = 0
        const val SCORE_MIN = 1
        const val SCORE_MAX = 10
    }
}

/** Puanin okunabilir MAL etiketi. */
fun Int.scoreLabel(): String = when (this) {
    10 -> "Masterpiece"
    9 -> "Great"
    8 -> "Very Good"
    7 -> "Good"
    6 -> "Fine"
    5 -> "Average"
    4 -> "Bad"
    3 -> "Very Bad"
    2 -> "Horrible"
    1 -> "Appalling"
    else -> "-"
}
