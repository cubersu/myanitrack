package com.myanitrack.core.model

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Bir animenin yayin zamani bilgisi.
 *
 * Jikan `broadcast` alanini "Sundays", "17:00", "Asia/Tokyo" seklinde veriyor;
 * ayristirma ag katmaninda yapilip burada tiplenmis olarak tutuluyor. Alanlarin
 * herhangi biri eksikse (MAL sik sik bos birakiyor) [nextAiringAt] null olur ve
 * arayuz geri sayim yerine "bilinmiyor" gosterir.
 */
data class BroadcastInfo(
    val day: DayOfWeek? = null,
    val time: LocalTime? = null,
    val zone: ZoneId? = null,
    val rawText: String? = null,
) {
    val isComplete: Boolean get() = day != null && time != null && zone != null
}

/** Takvim ekranindaki tek satir. */
data class ScheduleEntry(
    val node: MediaNode,
    val broadcast: BroadcastInfo,
    val nextAiringAt: Instant? = null,
    /** Kullanicinin listesinde ve izlemede mi. */
    val isInMyList: Boolean = false,
) {
    val id: Int get() = node.id

    fun timeUntilAiring(now: Instant = Instant.now()): Duration? =
        nextAiringAt?.let { Duration.between(now, it) }?.takeIf { !it.isNegative }
}

/** Haftalik takvim: gun -> o gun yayinlanan yapimlar. */
data class WeeklySchedule(
    val days: Map<DayOfWeek, List<ScheduleEntry>> = emptyMap(),
) {
    operator fun get(day: DayOfWeek): List<ScheduleEntry> = days[day].orEmpty()

    val isEmpty: Boolean get() = days.values.all { it.isEmpty() }
}
