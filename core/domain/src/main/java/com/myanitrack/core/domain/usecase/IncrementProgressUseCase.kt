package com.myanitrack.core.domain.usecase

import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.model.ListStatus
import com.myanitrack.core.model.ListStatusUpdate
import com.myanitrack.core.model.MediaListEntry
import java.time.LocalDate
import javax.inject.Inject

/**
 * Liste satirindaki "+1" dugmesi.
 *
 * Orijinal MALClient davranisini korur: son bolum isaretlendiginde kayit
 * otomatik olarak "Completed" durumuna gecer ve bitis tarihi bugun olur.
 * Ilk bolum isaretlendiginde ve kayit "Plan to watch" ise durum "Watching"
 * yapilip baslangic tarihi atanir.
 */
class IncrementProgressUseCase @Inject constructor(
    private val updateListEntry: UpdateListEntryUseCase,
) {
    suspend operator fun invoke(
        entry: MediaListEntry,
        today: LocalDate = LocalDate.now(),
    ): AppResult<MediaListEntry> {
        val next = entry.progress + 1
        val total = entry.total
        val reachedEnd = total != null && total > 0 && next >= total

        val update = ListStatusUpdate(
            progress = if (total != null && total > 0) next.coerceAtMost(total) else next,
            status = when {
                reachedEnd -> ListStatus.COMPLETED
                entry.listStatus.status == ListStatus.PLAN_TO_WATCH -> ListStatus.WATCHING
                else -> null
            },
            startDate = today.takeIf {
                entry.listStatus.startDate == null && entry.progress == 0
            },
            finishDate = today.takeIf { reachedEnd && entry.listStatus.finishDate == null },
        )
        return updateListEntry(entry.mediaType, entry.id, update)
    }
}
