package com.myanitrack.core.network.jikan.mapper

import com.myanitrack.core.model.Friend
import com.myanitrack.core.model.HistoryEntry
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.UserMediaStatistics
import com.myanitrack.core.model.UserProfileDetails
import com.myanitrack.core.network.jikan.dto.JikanAnimeStatsDto
import com.myanitrack.core.network.jikan.dto.JikanFriendDto
import com.myanitrack.core.network.jikan.dto.JikanHistoryDto
import com.myanitrack.core.network.jikan.dto.JikanMangaStatsDto
import com.myanitrack.core.network.jikan.dto.JikanUserProfileDto
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

/** Jikan tarihleri ISO-8601 zaman damgasi ya da yalin tarih olabiliyor. */
private fun String?.toInstantOrNull(): Instant? {
    if (this.isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(this).toInstant() }
        .recoverCatching { LocalDate.parse(this.take(10)).atStartOfDay(java.time.ZoneOffset.UTC).toInstant() }
        .getOrNull()
}

private fun String?.toLocalDateOrNull(): LocalDate? {
    if (this.isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(this).toLocalDate() }
        .recoverCatching { LocalDate.parse(this.take(10)) }
        .getOrNull()
}

fun JikanUserProfileDto.toDomain(): UserProfileDetails = UserProfileDetails(
    userName = username,
    malId = malId,
    imageUrl = images?.medium ?: images?.large,
    gender = gender?.takeIf { it.isNotBlank() },
    birthday = birthday.toLocalDateOrNull(),
    location = location?.takeIf { it.isNotBlank() },
    joinedAt = joined.toInstantOrNull(),
    lastOnlineAt = lastOnline.toInstantOrNull(),
    animeStats = statistics?.anime?.toDomain(),
    mangaStats = statistics?.manga?.toDomain(),
)

fun JikanAnimeStatsDto.toDomain(): UserMediaStatistics = UserMediaStatistics(
    mediaType = MediaType.ANIME,
    daysSpent = daysWatched,
    meanScore = meanScore,
    inProgress = watching,
    completed = completed,
    onHold = onHold,
    dropped = dropped,
    planned = planToWatch,
    totalEntries = totalEntries,
    repeated = rewatched,
    unitsConsumed = episodesWatched,
)

fun JikanMangaStatsDto.toDomain(): UserMediaStatistics = UserMediaStatistics(
    mediaType = MediaType.MANGA,
    daysSpent = daysRead,
    meanScore = meanScore,
    inProgress = reading,
    completed = completed,
    onHold = onHold,
    dropped = dropped,
    planned = planToRead,
    totalEntries = totalEntries,
    repeated = reread,
    unitsConsumed = chaptersRead,
    volumesRead = volumesRead,
)

/**
 * Gecmis kaydi. `entry.type` "anime" ya da "manga" gelir; taninmayan tur
 * gelirse kayit atlanir (null doner) cunku hangi detay sayfasina gidecegimizi
 * bilemeyiz.
 */
fun JikanHistoryDto.toDomain(): HistoryEntry? {
    val mediaType = when (entry.type?.lowercase()) {
        "anime" -> MediaType.ANIME
        "manga" -> MediaType.MANGA
        else -> return null
    }
    return HistoryEntry(
        malId = entry.malId,
        title = entry.name,
        mediaType = mediaType,
        increment = increment,
        date = date.toInstantOrNull(),
    )
}

fun JikanFriendDto.toDomain(): Friend = Friend(
    userName = user.username,
    imageUrl = user.images?.medium ?: user.images?.large,
    lastOnlineAt = lastOnline.toInstantOrNull(),
    friendsSince = friendsSince.toInstantOrNull(),
)
