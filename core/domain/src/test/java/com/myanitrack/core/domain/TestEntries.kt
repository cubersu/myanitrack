package com.myanitrack.core.domain

import com.myanitrack.core.model.ListStatus
import com.myanitrack.core.model.MediaListEntry
import com.myanitrack.core.model.MediaNode
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.MyListStatus
import java.time.Instant
import java.time.LocalDate

/** Testlerde okunabilirligi artiran kisa kurucu. */
fun entry(
    id: Int = 1,
    title: String = "Title $id",
    mediaType: MediaType = MediaType.ANIME,
    status: ListStatus = ListStatus.WATCHING,
    score: Int = 0,
    progress: Int = 0,
    total: Int? = 12,
    meanScore: Double? = null,
    popularity: Int? = null,
    tags: List<String> = emptyList(),
    nsfw: Boolean = false,
    updatedAt: Instant? = null,
    startDate: LocalDate? = null,
): MediaListEntry = MediaListEntry(
    node = MediaNode(
        id = id,
        mediaType = mediaType,
        title = title,
        numEpisodes = if (mediaType.isAnime) total else null,
        numChapters = if (mediaType.isManga) total else null,
        meanScore = meanScore,
        popularity = popularity,
        nsfw = nsfw,
        startDate = startDate,
    ),
    listStatus = MyListStatus(
        status = status,
        score = score,
        numEpisodesWatched = if (mediaType.isAnime) progress else 0,
        numChaptersRead = if (mediaType.isManga) progress else 0,
        tags = tags,
        updatedAt = updatedAt,
    ),
)
