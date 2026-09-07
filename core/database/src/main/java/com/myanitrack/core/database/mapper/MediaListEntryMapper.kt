package com.myanitrack.core.database.mapper

import com.myanitrack.core.database.entity.MediaListEntryEntity
import com.myanitrack.core.model.AiringStatus
import com.myanitrack.core.model.ListStatus
import com.myanitrack.core.model.MediaImage
import com.myanitrack.core.model.MediaListEntry
import com.myanitrack.core.model.MediaNode
import com.myanitrack.core.model.MediaSubType
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.MyListStatus

fun MediaListEntryEntity.toDomain(): MediaListEntry {
    val type = MediaType.valueOf(mediaType)
    return MediaListEntry(
        node = MediaNode(
            id = malId,
            mediaType = type,
            title = title,
            englishTitle = englishTitle,
            japaneseTitle = japaneseTitle,
            picture = MediaImage(medium = pictureMedium, large = pictureLarge),
            subType = MediaSubType.fromApi(subType),
            airingStatus = AiringStatus.fromApi(airingStatus),
            meanScore = meanScore,
            rank = rank,
            popularity = popularity,
            numEpisodes = numEpisodes,
            numChapters = numChapters,
            numVolumes = numVolumes,
            startDate = startDate,
            endDate = endDate,
            synopsis = synopsis,
            genres = genres,
            studios = studios,
            nsfw = nsfw,
        ),
        listStatus = MyListStatus(
            status = ListStatus.entries.firstOrNull { it.name == listStatus } ?: ListStatus.WATCHING,
            score = score,
            numEpisodesWatched = numEpisodesWatched,
            numChaptersRead = numChaptersRead,
            numVolumesRead = numVolumesRead,
            isRepeating = isRepeating,
            numTimesRepeated = numTimesRepeated,
            repeatValue = repeatValue,
            priority = priority,
            startDate = userStartDate,
            finishDate = userFinishDate,
            tags = tags,
            comments = comments,
            updatedAt = updatedAt,
        ),
    )
}

fun MediaListEntry.toEntity(pendingSync: Boolean = false): MediaListEntryEntity =
    MediaListEntryEntity(
        mediaType = node.mediaType.name,
        malId = node.id,
        title = node.title,
        englishTitle = node.englishTitle,
        japaneseTitle = node.japaneseTitle,
        pictureMedium = node.picture.medium,
        pictureLarge = node.picture.large,
        subType = node.subType.apiValue,
        airingStatus = node.airingStatus.apiValue,
        meanScore = node.meanScore,
        rank = node.rank,
        popularity = node.popularity,
        numEpisodes = node.numEpisodes,
        numChapters = node.numChapters,
        numVolumes = node.numVolumes,
        startDate = node.startDate,
        endDate = node.endDate,
        synopsis = node.synopsis,
        genres = node.genres,
        studios = node.studios,
        nsfw = node.nsfw,
        listStatus = listStatus.status.name,
        score = listStatus.score,
        numEpisodesWatched = listStatus.numEpisodesWatched,
        numChaptersRead = listStatus.numChaptersRead,
        numVolumesRead = listStatus.numVolumesRead,
        isRepeating = listStatus.isRepeating,
        numTimesRepeated = listStatus.numTimesRepeated,
        repeatValue = listStatus.repeatValue,
        priority = listStatus.priority,
        userStartDate = listStatus.startDate,
        userFinishDate = listStatus.finishDate,
        tags = listStatus.tags,
        comments = listStatus.comments,
        updatedAt = listStatus.updatedAt,
        pendingSync = pendingSync,
    )
