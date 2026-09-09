package com.myanitrack.feature.mylist

import com.myanitrack.core.model.AiringStatus
import com.myanitrack.core.model.BroadcastInfo
import com.myanitrack.core.model.MediaNode
import com.myanitrack.core.model.MediaType
import com.myanitrack.feature.mylist.component.airingCountdown
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AiringCountdownTest {
    private val node = MediaNode(id = 1, mediaType = MediaType.ANIME, title = "Anime", airingStatus = AiringStatus.AIRING)
    private val broadcast = BroadcastInfo(DayOfWeek.WEDNESDAY, LocalTime.of(23, 0), ZoneId.of("Asia/Tokyo"))
    private val airing = Instant.parse("2026-09-09T14:00:00Z")

    @Test
    fun `only appears strictly within the next 24 hours`() {
        assertNull(airingCountdown(node, broadcast, airing.minusSeconds(86400)))
        assertNull(airingCountdown(node, broadcast, airing.minusSeconds(86401)))
        assertEquals("23:59:59", airingCountdown(node, broadcast, airing.minusSeconds(86399)))
        assertEquals("01:01:01", airingCountdown(node, broadcast, airing.minusSeconds(3661)))
        assertEquals("00:00:01", airingCountdown(node, broadcast, airing.minusNanos(1)))
        assertNull(airingCountdown(node, broadcast, airing))
        assertNull(airingCountdown(node, broadcast, airing.plusSeconds(1)))
    }

    @Test
    fun `requires an airing anime with a complete broadcast time`() {
        val now = airing.minusSeconds(3600)
        assertNull(airingCountdown(node, null, now))
        assertNull(airingCountdown(node, broadcast.copy(time = null), now))
        assertNull(airingCountdown(node.copy(mediaType = MediaType.MANGA), broadcast, now))
        assertNull(airingCountdown(node.copy(airingStatus = AiringStatus.FINISHED), broadcast, now))
        assertNull(airingCountdown(node.copy(airingStatus = AiringStatus.NOT_YET_AIRED), broadcast, now))
    }
}
