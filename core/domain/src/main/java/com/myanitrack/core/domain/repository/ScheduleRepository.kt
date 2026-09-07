package com.myanitrack.core.domain.repository

import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.model.ScheduleEntry
import com.myanitrack.core.model.WeeklySchedule
import java.time.DayOfWeek

/**
 * Haftalik yayin takvimi (Jikan `/schedules`, salt okunur).
 *
 * Takvim gunde bir kez degistigi ve 7 ayri istek gerektirdigi icin agresif
 * sekilde onbellege alinir; Jikan hiz siniri (3/sn, 60/dk) buna zorluyor.
 */
interface ScheduleRepository {

    /** Tek bir gunun yayin listesi. */
    suspend fun getDay(day: DayOfWeek, forceRefresh: Boolean = false): AppResult<List<ScheduleEntry>>

    /** Haftanin tamami. Gunler sirayla cekilir, hiz sinirlayici araya girer. */
    suspend fun getWeek(forceRefresh: Boolean = false): AppResult<WeeklySchedule>

    /**
     * Kullanicinin izlemekte oldugu ve yayini devam eden yapimlar icin
     * yaklasan bolumler. Bildirim zamanlayicisi bunu kullanir.
     */
    suspend fun getUpcomingForMyList(): AppResult<List<ScheduleEntry>>
}
