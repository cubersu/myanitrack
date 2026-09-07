package com.myanitrack.core.model

import java.time.LocalDate

/**
 * Detay sayfasinin zengin verisi.
 *
 * Kaynak Jikan v4-tur (MAL API v2 bu alanlarin cogunu vermiyor). Kullanicinin
 * kendi liste kaydi ([MediaListEntry]) buna dahil degildir; detay ekrani ikisini
 * ayri ayri birlestirir cunku liste kaydi MAL API v2-den, geri kalani Jikan-dan gelir.
 */
data class MediaDetails(
    val id: Int,
    val mediaType: MediaType,
    val title: String,
    val englishTitle: String? = null,
    val japaneseTitle: String? = null,
    val synonyms: List<String> = emptyList(),
    val picture: MediaImage = MediaImage(),
    val subType: MediaSubType = MediaSubType.UNKNOWN,
    val airingStatus: AiringStatus = AiringStatus.UNKNOWN,
    val synopsis: String? = null,
    val background: String? = null,
    val source: String? = null,
    val rating: String? = null,
    val durationText: String? = null,
    val numEpisodes: Int? = null,
    val numChapters: Int? = null,
    val numVolumes: Int? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val season: Season? = null,
    val broadcast: String? = null,
    val genres: List<NamedRef> = emptyList(),
    val themes: List<NamedRef> = emptyList(),
    val demographics: List<NamedRef> = emptyList(),
    val studios: List<NamedRef> = emptyList(),
    val authors: List<NamedRef> = emptyList(),
    val statistics: MediaStatistics = MediaStatistics(),
    val trailer: PromoVideo? = null,
    val related: List<RelatedGroup> = emptyList(),
    val openingThemes: List<String> = emptyList(),
    val endingThemes: List<String> = emptyList(),
    val nsfw: Boolean = false,
) {
    val totalUnits: Int? get() = if (mediaType.isAnime) numEpisodes else numChapters

    val malUrl: String
        get() = "https://myanimelist.net/${if (mediaType.isAnime) "anime" else "manga"}/$id"

    /** Detay ekraninda listeye eklemek icin gereken minimum dugum. */
    fun toNode(): MediaNode = MediaNode(
        id = id,
        mediaType = mediaType,
        title = title,
        englishTitle = englishTitle,
        japaneseTitle = japaneseTitle,
        picture = picture,
        subType = subType,
        airingStatus = airingStatus,
        meanScore = statistics.score,
        rank = statistics.rank,
        popularity = statistics.popularity,
        numEpisodes = numEpisodes,
        numChapters = numChapters,
        numVolumes = numVolumes,
        startDate = startDate,
        endDate = endDate,
        synopsis = synopsis,
        genres = genres.map { it.name },
        studios = studios.map { it.name },
        nsfw = nsfw,
    )
}

/** MAL tarafindaki tur/studyo/yazar gibi baglantili varliklar. */
data class NamedRef(
    val id: Int,
    val name: String,
    val url: String? = null,
)

/** Detay sayfasindaki MAL istatistikleri. */
data class MediaStatistics(
    val score: Double? = null,
    val scoredBy: Int? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    val members: Int? = null,
    val favorites: Int? = null,
)

/** Iliskili yapimlar, iliski turune gore gruplanmis (Sequel, Prequel, Adaptation...). */
data class RelatedGroup(
    val relation: String,
    val entries: List<RelatedEntry>,
)

data class RelatedEntry(
    val id: Int,
    val name: String,
    val mediaType: MediaType,
)

/** YouTube tanitim videosu. */
data class PromoVideo(
    val title: String? = null,
    val youtubeId: String? = null,
    val thumbnailUrl: String? = null,
) {
    val watchUrl: String? get() = youtubeId?.let { "https://www.youtube.com/watch?v=$it" }
}

data class CharacterSummary(
    val id: Int,
    val name: String,
    val imageUrl: String? = null,
    val role: String? = null,
    val favorites: Int = 0,
    /** Japonca seslendiren; yoksa ilk bulunan dil. */
    val voiceActorName: String? = null,
    val voiceActorImageUrl: String? = null,
)

data class StaffSummary(
    val id: Int,
    val name: String,
    val imageUrl: String? = null,
    val positions: List<String> = emptyList(),
)

data class MediaReview(
    val id: Int,
    val userName: String,
    val userImageUrl: String? = null,
    val score: Int? = null,
    val text: String,
    val tags: List<String> = emptyList(),
    val isSpoiler: Boolean = false,
    val isPreliminary: Boolean = false,
    val episodesWatched: Int? = null,
    val dateText: String? = null,
    val reactionsCount: Int = 0,
    val url: String? = null,
)

data class MediaRecommendation(
    val node: MediaNode,
    val votes: Int = 0,
)

/**
 * Genel oneri akisindaki ikili: "X-i begendiysen Y-yi dene".
 * Orijinal MALClient-taki "global recommendations" ekraninin karsiligi.
 */
data class RecommendationPair(
    val liked: MediaNode,
    val suggested: MediaNode,
    val comment: String = "",
) {
    val key: String get() = "${liked.id}-${suggested.id}"
}
