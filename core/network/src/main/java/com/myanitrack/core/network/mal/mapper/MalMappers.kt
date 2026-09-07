package com.myanitrack.core.network.mal.mapper

import com.myanitrack.core.model.AiringStatus
import com.myanitrack.core.model.AnimeStatistics
import com.myanitrack.core.model.ListStatus
import com.myanitrack.core.model.MediaImage
import com.myanitrack.core.model.MediaListEntry
import com.myanitrack.core.model.MediaNode
import com.myanitrack.core.model.MediaSubType
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.MyListStatus
import com.myanitrack.core.model.UserProfile
import com.myanitrack.core.network.mal.dto.MalAnimeStatisticsDto
import com.myanitrack.core.network.mal.dto.MalListEntryDto
import com.myanitrack.core.network.mal.dto.MalListStatusDto
import com.myanitrack.core.network.mal.dto.MalNodeDto
import com.myanitrack.core.network.mal.dto.MalUserDto
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * MAL kismi tarih dondurebiliyor: "2021-04-03", "2021-04" ya da "2021".
 * Eksik parcalar 1 kabul edilir; cozulemeyen deger null olur.
 */
fun String?.toLocalDateOrNull(): LocalDate? {
    val raw = this?.trim().orEmpty()
    if (raw.isEmpty()) return null
    val parts = raw.split("-")
    return runCatching {
        val year = parts[0].toInt()
        val month = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val day = parts.getOrNull(2)?.toIntOrNull() ?: 1
        LocalDate.of(year, month, day)
    }.getOrNull()
}

fun LocalDate?.toMalDateOrNull(): String? =
    this?.format(DateTimeFormatter.ISO_LOCAL_DATE)

private fun String?.toInstantOrNull(): Instant? =
    this?.let { runCatching { Instant.parse(it) }.getOrNull() }

fun MalNodeDto.toDomain(mediaType: MediaType): MediaNode = MediaNode(
    id = id,
    mediaType = mediaType,
    title = title,
    englishTitle = alternativeTitles?.en?.takeIf { it.isNotBlank() },
    japaneseTitle = alternativeTitles?.ja?.takeIf { it.isNotBlank() },
    picture = MediaImage(medium = mainPicture?.medium, large = mainPicture?.large),
    subType = MediaSubType.fromApi(this.mediaType),
    airingStatus = AiringStatus.fromApi(status),
    meanScore = mean,
    rank = rank,
    popularity = popularity,
    numEpisodes = numEpisodes,
    numChapters = numChapters,
    numVolumes = numVolumes,
    startDate = startDate.toLocalDateOrNull(),
    endDate = endDate.toLocalDateOrNull(),
    synopsis = synopsis,
    genres = genres.map { it.name },
    studios = studios.map { it.name },
    // MAL "white" disindaki her deger (gray/black) yetiskin icerik demek.
    nsfw = nsfw != null && nsfw != "white",
)

fun MalListStatusDto.toDomain(mediaType: MediaType): MyListStatus? {
    val listStatus = ListStatus.fromApi(status) ?: return null
    return MyListStatus(
        status = listStatus,
        score = score,
        numEpisodesWatched = numEpisodesWatched,
        numChaptersRead = numChaptersRead,
        numVolumesRead = numVolumesRead,
        isRepeating = if (mediaType.isAnime) isRewatching else isRereading,
        numTimesRepeated = if (mediaType.isAnime) numTimesRewatched else numTimesReread,
        repeatValue = if (mediaType.isAnime) rewatchValue else rereadValue,
        priority = priority,
        startDate = startDate.toLocalDateOrNull(),
        finishDate = finishDate.toLocalDateOrNull(),
        tags = tags,
        comments = comments,
        updatedAt = updatedAt.toInstantOrNull(),
    )
}

fun MalListEntryDto.toDomain(mediaType: MediaType): MediaListEntry? {
    val status = listStatus?.toDomain(mediaType) ?: return null
    return MediaListEntry(node = node.toDomain(mediaType), listStatus = status)
}

fun MalUserDto.toDomain(): UserProfile = UserProfile(
    id = id,
    name = name,
    pictureUrl = picture,
    gender = gender,
    birthday = birthday,
    location = location,
    joinedAt = joinedAt.toInstantOrNull(),
    animeStatistics = animeStatistics?.toDomain(),
)

fun MalAnimeStatisticsDto.toDomain(): AnimeStatistics = AnimeStatistics(
    numItemsWatching = numItemsWatching,
    numItemsCompleted = numItemsCompleted,
    numItemsOnHold = numItemsOnHold,
    numItemsDropped = numItemsDropped,
    numItemsPlanToWatch = numItemsPlanToWatch,
    numItems = numItems,
    numDaysWatched = numDaysWatched,
    numEpisodes = numEpisodes,
    numTimesRewatched = numTimesRewatched,
    meanScore = meanScore,
)
