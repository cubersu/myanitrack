package com.myanitrack.core.network.jikan.mapper

import com.myanitrack.core.model.AiringStatus
import com.myanitrack.core.model.CharacterSummary
import com.myanitrack.core.model.MediaDetails
import com.myanitrack.core.model.MediaImage
import com.myanitrack.core.model.MediaNode
import com.myanitrack.core.model.MediaRecommendation
import com.myanitrack.core.model.MediaReview
import com.myanitrack.core.model.MediaStatistics
import com.myanitrack.core.model.MediaSubType
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.NamedRef
import com.myanitrack.core.model.PromoVideo
import com.myanitrack.core.model.RelatedEntry
import com.myanitrack.core.model.RelatedGroup
import com.myanitrack.core.model.Season
import com.myanitrack.core.model.SeasonName
import com.myanitrack.core.model.StaffSummary
import com.myanitrack.core.network.jikan.dto.JikanCharacterEntryDto
import com.myanitrack.core.network.jikan.dto.JikanEntryDto
import com.myanitrack.core.network.jikan.dto.JikanGenreDto
import com.myanitrack.core.network.jikan.dto.JikanMediaDto
import com.myanitrack.core.network.jikan.dto.JikanNamedDto
import com.myanitrack.core.network.jikan.dto.JikanRecommendationDto
import com.myanitrack.core.network.jikan.dto.JikanReviewDto
import com.myanitrack.core.network.jikan.dto.JikanStaffEntryDto
import com.myanitrack.core.network.jikan.dto.JikanTrailerDto
import com.myanitrack.core.network.jikan.dto.JikanVideosDto
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Jikan ISO-8601 zaman damgasi doner ("2009-04-05T00:00:00+00:00").
 * Cozulemeyen deger null olur; detay ekrani tarihi gizler.
 */
internal fun String?.toJikanDateOrNull(): LocalDate? {
    if (this.isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(this).toLocalDate() }
        .recoverCatching { LocalDate.parse(this.take(10)) }
        .getOrNull()
}

private fun JikanNamedDto.toNamedRef() = NamedRef(id = malId, name = name, url = url)

/**
 * Jikan tur adlari MAL API v2 ile ayni degil; anime tarafinda "TV"/"Movie",
 * manga tarafinda "Manga"/"Light Novel" gibi buyuk harfli geliyor.
 */
private fun String?.toSubType(): MediaSubType =
    MediaSubType.fromApi(this?.lowercase()?.replace(' ', '_'))

private fun JikanMediaDto.toAiringStatus(mediaType: MediaType): AiringStatus = when {
    status == null -> AiringStatus.UNKNOWN
    mediaType.isAnime -> when (status.lowercase()) {
        "currently airing" -> AiringStatus.AIRING
        "finished airing" -> AiringStatus.FINISHED
        "not yet aired" -> AiringStatus.NOT_YET_AIRED
        else -> AiringStatus.UNKNOWN
    }
    else -> when (status.lowercase()) {
        "publishing" -> AiringStatus.PUBLISHING
        "finished" -> AiringStatus.FINISHED_PUBLISHING
        "not yet published", "on hiatus", "discontinued" -> AiringStatus.NOT_YET_PUBLISHED
        else -> AiringStatus.UNKNOWN
    }
}

private fun JikanTrailerDto.toPromoVideo(title: String? = null): PromoVideo? {
    val id = youtubeId?.takeIf { it.isNotBlank() } ?: return null
    return PromoVideo(
        title = title,
        youtubeId = id,
        thumbnailUrl = images?.largeImageUrl ?: images?.imageUrl,
    )
}

/** Liste/izgara gosterimleri icin kisa dugum. */
fun JikanMediaDto.toNode(mediaType: MediaType): MediaNode = MediaNode(
    id = malId,
    mediaType = mediaType,
    title = title,
    englishTitle = titleEnglish?.takeIf { it.isNotBlank() },
    japaneseTitle = titleJapanese?.takeIf { it.isNotBlank() },
    picture = MediaImage(medium = images?.medium, large = images?.large),
    subType = type.toSubType(),
    airingStatus = toAiringStatus(mediaType),
    meanScore = score,
    rank = rank,
    popularity = popularity,
    numEpisodes = episodes,
    numChapters = chapters,
    numVolumes = volumes,
    startDate = (aired?.from ?: published?.from).toJikanDateOrNull(),
    endDate = (aired?.to ?: published?.to).toJikanDateOrNull(),
    synopsis = synopsis,
    genres = genres.map { it.name },
    studios = studios.map { it.name },
    // Jikan yetiskin turleri ayri bir alanda veriyor; dolu ise NSFW kabul et.
    nsfw = explicitGenres.isNotEmpty(),
)

fun JikanMediaDto.toDetails(mediaType: MediaType): MediaDetails = MediaDetails(
    id = malId,
    mediaType = mediaType,
    title = title,
    englishTitle = titleEnglish?.takeIf { it.isNotBlank() },
    japaneseTitle = titleJapanese?.takeIf { it.isNotBlank() },
    synonyms = titleSynonyms,
    picture = MediaImage(medium = images?.medium, large = images?.large),
    subType = type.toSubType(),
    airingStatus = toAiringStatus(mediaType),
    synopsis = synopsis,
    background = background?.takeIf { it.isNotBlank() },
    source = source,
    rating = rating,
    durationText = duration,
    numEpisodes = episodes,
    numChapters = chapters,
    numVolumes = volumes,
    startDate = (aired?.from ?: published?.from).toJikanDateOrNull(),
    endDate = (aired?.to ?: published?.to).toJikanDateOrNull(),
    season = SeasonName.fromApi(season)?.let { name -> year?.let { Season(name, it) } },
    broadcast = broadcast?.string,
    genres = genres.map { it.toNamedRef() },
    themes = themes.map { it.toNamedRef() },
    demographics = demographics.map { it.toNamedRef() },
    studios = studios.map { it.toNamedRef() },
    authors = authors.map { it.toNamedRef() },
    statistics = MediaStatistics(
        score = score,
        scoredBy = scoredBy,
        rank = rank,
        popularity = popularity,
        members = members,
        favorites = favorites,
    ),
    trailer = trailer?.toPromoVideo(),
    related = relations.mapNotNull { relation ->
        val entries = relation.entry.mapNotNull { it.toRelatedEntry() }
        if (entries.isEmpty()) null else RelatedGroup(relation.relation, entries)
    },
    openingThemes = theme?.openings.orEmpty(),
    endingThemes = theme?.endings.orEmpty(),
    nsfw = explicitGenres.isNotEmpty(),
)

/** `type` alani "anime"/"manga" degilse iliski gosterilemez, atlanir. */
private fun JikanNamedDto.toRelatedEntry(): RelatedEntry? {
    val resolved = when (type?.lowercase()) {
        "anime" -> MediaType.ANIME
        "manga" -> MediaType.MANGA
        else -> return null
    }
    return RelatedEntry(id = malId, name = name, mediaType = resolved)
}

fun JikanCharacterEntryDto.toDomain(): CharacterSummary {
    // Orijinal MALClient gibi once Japonca seslendireni gosteriyoruz.
    val voiceActor = voiceActors.firstOrNull { it.language.equals("Japanese", ignoreCase = true) }
        ?: voiceActors.firstOrNull()
    return CharacterSummary(
        id = character.malId,
        name = character.name,
        imageUrl = character.images?.medium,
        role = role,
        favorites = favorites,
        voiceActorName = voiceActor?.person?.name,
        voiceActorImageUrl = voiceActor?.person?.images?.medium,
    )
}

fun JikanStaffEntryDto.toDomain(): StaffSummary = StaffSummary(
    id = person.malId,
    name = person.name,
    imageUrl = person.images?.medium,
    positions = positions,
)

fun JikanReviewDto.toDomain(): MediaReview = MediaReview(
    id = malId,
    userName = user?.username.orEmpty(),
    userImageUrl = user?.images?.medium,
    score = score,
    text = review,
    tags = tags,
    isSpoiler = isSpoiler,
    isPreliminary = isPreliminary,
    episodesWatched = episodesWatched,
    dateText = date.toJikanDateOrNull()?.toString(),
    reactionsCount = reactions?.overall ?: 0,
    url = url,
)

fun JikanRecommendationDto.toDomain(mediaType: MediaType): MediaRecommendation =
    MediaRecommendation(node = entry.toNode(mediaType), votes = votes)

fun JikanEntryDto.toNode(mediaType: MediaType): MediaNode = MediaNode(
    id = malId,
    mediaType = mediaType,
    title = title,
    picture = MediaImage(medium = images?.medium, large = images?.large),
)

fun JikanVideosDto.toPromoVideos(): List<PromoVideo> =
    promo.mapNotNull { it.trailer?.toPromoVideo(it.title) }

fun JikanGenreDto.toDomain(): NamedRef = NamedRef(id = malId, name = name, url = url)
