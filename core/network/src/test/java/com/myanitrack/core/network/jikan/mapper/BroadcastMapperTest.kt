package com.myanitrack.core.network.jikan.mapper

import com.myanitrack.core.network.jikan.dto.JikanBroadcastDto
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class BroadcastMapperTest {

    @ParameterizedTest
    @CsvSource(
        "Sundays, SUNDAY",
        "Mondays, MONDAY",
        "Saturdays, SATURDAY",
        "Wednesday, WEDNESDAY",
    )
    @DisplayName("MAL cogul gun adlari DayOfWeek-e cevrilir")
    fun `maps day names`(raw: String, expected: String) {
        assertEquals(DayOfWeek.valueOf(expected), raw.toDayOfWeekOrNull())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   ", "Unknown", "Other"])
    @DisplayName("Tanimsiz gun degerleri null doner")
    fun `unknown days are null`(raw: String) {
        assertNull(raw.toDayOfWeekOrNull())
    }

    @Test
    @DisplayName("null gun null doner")
    fun `null day`() {
        assertNull(null.toDayOfWeekOrNull())
    }

    @ParameterizedTest
    @CsvSource("17:00, 17:00", "00:30, 00:30", "23:59, 23:59")
    @DisplayName("Yayin saati cozulur")
    fun `maps time`(raw: String, expected: String) {
        assertEquals(LocalTime.parse(expected), raw.toLocalTimeOrNull())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "25:00", "abc"])
    @DisplayName("Gecersiz saat null doner")
    fun `invalid time is null`(raw: String) {
        assertNull(raw.toLocalTimeOrNull())
    }

    @Test
    @DisplayName("Saat dilimi cozulur, gecersizse null olur")
    fun `maps zone`() {
        assertEquals(ZoneId.of("Asia/Tokyo"), "Asia/Tokyo".toZoneIdOrNull())
        assertNull("Mars/Olympus".toZoneIdOrNull())
        assertNull("".toZoneIdOrNull())
        assertNull(null.toZoneIdOrNull())
    }

    @Test
    @DisplayName("Tam yayin bilgisi eksiksiz eslenir")
    fun `maps complete broadcast`() {
        val dto = JikanBroadcastDto(
            day = "Sundays",
            time = "17:00",
            timezone = "Asia/Tokyo",
            string = "Sundays at 17:00 (JST)",
        )

        val info = dto.toDomain()

        assertEquals(DayOfWeek.SUNDAY, info.day)
        assertEquals(LocalTime.of(17, 0), info.time)
        assertEquals(ZoneId.of("Asia/Tokyo"), info.zone)
        assertEquals("Sundays at 17:00 (JST)", info.rawText)
        assertTrue(info.isComplete)
    }

    @Test
    @DisplayName("Kismi bilgi kismen eslenir, isComplete false olur")
    fun `maps partial broadcast`() {
        val info = JikanBroadcastDto(day = "Mondays", time = null, timezone = null).toDomain()

        assertEquals(DayOfWeek.MONDAY, info.day)
        assertNull(info.time)
        assertFalse(info.isComplete)
    }

    @Test
    @DisplayName("null broadcast bos bilgi doner")
    fun `null broadcast`() {
        val info = (null as JikanBroadcastDto?).toDomain()

        assertNull(info.day)
        assertFalse(info.isComplete)
    }
}
