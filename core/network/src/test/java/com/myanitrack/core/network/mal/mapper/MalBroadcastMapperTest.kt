package com.myanitrack.core.network.mal.mapper

import com.myanitrack.core.network.mal.dto.MalBroadcastDto
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MalBroadcastMapperTest {
    @Test
    fun `MAL broadcast is interpreted in Japan time`() {
        val result = MalBroadcastDto("wednesday", "23:30").toBroadcastInfo()
        assertEquals(DayOfWeek.WEDNESDAY, result.day)
        assertEquals(LocalTime.of(23, 30), result.time)
        assertEquals(ZoneId.of("Asia/Tokyo"), result.zone)
        assertTrue(result.isComplete)
    }

    @Test
    fun `missing or invalid times do not invent an airing time`() {
        assertFalse(null.toBroadcastInfo().isComplete)
        assertFalse(MalBroadcastDto("unknown", "25:00").toBroadcastInfo().isComplete)
    }
}
