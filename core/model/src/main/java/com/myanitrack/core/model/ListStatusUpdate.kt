package com.myanitrack.core.model

import java.time.LocalDate

/**
 * Liste kaydinda yapilacak kismi guncelleme.
 *
 * null = "bu alana dokunma". Tarihleri temizlemek null ile ifade edilemedigi icin
 * ayri [clearStartDate] / [clearFinishDate] bayraklari var.
 */
data class ListStatusUpdate(
    val status: ListStatus? = null,
    val score: Int? = null,
    val progress: Int? = null,
    val volumesRead: Int? = null,
    val isRepeating: Boolean? = null,
    val numTimesRepeated: Int? = null,
    val repeatValue: Int? = null,
    val priority: Int? = null,
    val startDate: LocalDate? = null,
    val finishDate: LocalDate? = null,
    val clearStartDate: Boolean = false,
    val clearFinishDate: Boolean = false,
    val tags: List<String>? = null,
    val comments: String? = null,
) {
    val isEmpty: Boolean
        get() = status == null && score == null && progress == null && volumesRead == null &&
            isRepeating == null && numTimesRepeated == null && repeatValue == null &&
            priority == null && startDate == null && finishDate == null &&
            !clearStartDate && !clearFinishDate && tags == null && comments == null

    /** Yerel onbellegi ag cevabini beklemeden guncellemek icin (iyimser guncelleme). */
    fun applyTo(current: MyListStatus, mediaType: MediaType): MyListStatus = current.copy(
        status = status ?: current.status,
        score = score ?: current.score,
        numEpisodesWatched = if (mediaType.isAnime) progress ?: current.numEpisodesWatched
        else current.numEpisodesWatched,
        numChaptersRead = if (mediaType.isManga) progress ?: current.numChaptersRead
        else current.numChaptersRead,
        numVolumesRead = volumesRead ?: current.numVolumesRead,
        isRepeating = isRepeating ?: current.isRepeating,
        numTimesRepeated = numTimesRepeated ?: current.numTimesRepeated,
        repeatValue = repeatValue ?: current.repeatValue,
        priority = priority ?: current.priority,
        startDate = if (clearStartDate) null else startDate ?: current.startDate,
        finishDate = if (clearFinishDate) null else finishDate ?: current.finishDate,
        tags = tags ?: current.tags,
        comments = comments ?: current.comments,
    )
}
