package com.myanitrack.core.network.mal.mapper

import com.myanitrack.core.model.BroadcastInfo
import com.myanitrack.core.network.mal.dto.MalBroadcastDto
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId

fun MalBroadcastDto?.toBroadcastInfo(): BroadcastInfo = BroadcastInfo(
    day = DayOfWeek.entries.firstOrNull { it.name.equals(this?.dayOfWeek, ignoreCase = true) },
    time = this?.startTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
    zone = ZoneId.of("Asia/Tokyo"),
)
