package com.myanitrack.core.model

import java.time.LocalDate

/** Bir gorselin farkli boyutlari. */
data class MediaImage(
    val medium: String? = null,
    val large: String? = null,
) {
    val best: String? get() = large ?: medium
}

/**
 * Bir anime/manga-nin liste ve arama sonuclarinda gosterilen temel bilgisi.
 * Detay sayfasindaki zengin veri icin [MediaDetails] kullanilir.
 */
data class MediaNode(
    val id: Int,
    val mediaType: MediaType,
    val title: String,
    val englishTitle: String? = null,
    val japaneseTitle: String? = null,
    val picture: MediaImage = MediaImage(),
    val subType: MediaSubType = MediaSubType.UNKNOWN,
    val airingStatus: AiringStatus = AiringStatus.UNKNOWN,
    val meanScore: Double? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    val numEpisodes: Int? = null,
    val numChapters: Int? = null,
    val numVolumes: Int? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val synopsis: String? = null,
    val genres: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
    val nsfw: Boolean = false,
) {
    /** Toplam birim sayisi: anime icin bolum, manga icin bolum sayisi. */
    val totalUnits: Int? get() = if (mediaType.isAnime) numEpisodes else numChapters

    val malUrl: String
        get() = "https://myanimelist.net/${if (mediaType.isAnime) "anime" else "manga"}/$id"
}
