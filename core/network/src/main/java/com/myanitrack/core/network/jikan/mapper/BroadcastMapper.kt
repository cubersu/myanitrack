package com.myanitrack.core.network.jikan.mapper

import com.myanitrack.core.model.BroadcastInfo
import com.myanitrack.core.network.jikan.dto.JikanBroadcastDto
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Jikan `broadcast` alanini tiplenmis [BroadcastInfo]-ya cevirir.
 *
 * Ornek govde: `{"day":"Sundays","time":"17:00","timezone":"Asia/Tokyo"}`.
 * Alanlarin herhangi biri eksik ya da tanimsiz olabilir; her parca bagimsiz
 * ayristirilir ve cozulemeyen parca null kalir. Boylece kismi bilgi de
 * (orn. yalnizca gun) arayuzde gosterilebilir.
 */
fun JikanBroadcastDto?.toDomain(): BroadcastInfo {
    if (this == null) return BroadcastInfo()
    return BroadcastInfo(
        day = day.toDayOfWeekOrNull(),
        time = time.toLocalTimeOrNull(),
        zone = timezone.toZoneIdOrNull(),
        rawText = string,
    )
}

/** MAL gunleri cogul yazar: "Sundays", "Mondays". */
internal fun String?.toDayOfWeekOrNull(): DayOfWeek? {
    val normalized = this?.trim()?.lowercase()?.removeSuffix("s") ?: return null
    return DayOfWeek.entries.firstOrNull { it.name.lowercase() == normalized }
}

internal fun String?.toLocalTimeOrNull(): LocalTime? {
    val raw = this?.trim().orEmpty()
    if (raw.isEmpty()) return null
    return runCatching { LocalTime.parse(raw, DateTimeFormatter.ofPattern("HH:mm")) }
        .recoverCatching { LocalTime.parse(raw) }
        .getOrNull()
}

internal fun String?.toZoneIdOrNull(): ZoneId? {
    val raw = this?.trim().orEmpty()
    if (raw.isEmpty()) return null
    return runCatching { ZoneId.of(raw) }.getOrNull()
}
