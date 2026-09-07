package com.myanitrack.core.domain.schedule

import com.myanitrack.core.model.BroadcastInfo
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class NextEpisodeCalculatorTest {

    private val tokyo = ZoneId.of("Asia/Tokyo")

    private fun broadcast(
        day: DayOfWeek? = DayOfWeek.SUNDAY,
        time: LocalTime? = LocalTime.of(17, 0),
        zone: ZoneId? = tokyo,
    ) = BroadcastInfo(day = day, time = time, zone = zone)

    /** Tokyo yerel saatini Instant-a cevirir. */
    private fun tokyoInstant(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int = 0,
    ): Instant = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, tokyo).toInstant()

    @Test
    @DisplayName("Yayin gunu gelmemisse ayni haftanin o gunu bulunur")
    fun `finds this week`() {
        // 2026-09-09 Carsamba, hedef Pazar 17:00 -> 2026-09-13
        val now = tokyoInstant(2026, 9, 9, 12, 0)

        val next = NextEpisodeCalculator.nextAiring(broadcast(), now)

        assertEquals(tokyoInstant(2026, 9, 13, 17, 0), next)
    }

    @Test
    @DisplayName("Yayin gunu bugun ama saat gelmemisse bugun doner")
    fun `same day before air time`() {
        // 2026-09-13 Pazar 10:00, yayin 17:00
        val now = tokyoInstant(2026, 9, 13, 10, 0)

        val next = NextEpisodeCalculator.nextAiring(broadcast(), now)

        assertEquals(tokyoInstant(2026, 9, 13, 17, 0), next)
    }

    @Test
    @DisplayName("Yayin saati gectiyse gelecek haftaya kayar")
    fun `same day after air time rolls to next week`() {
        val now = tokyoInstant(2026, 9, 13, 18, 0)

        val next = NextEpisodeCalculator.nextAiring(broadcast(), now)

        assertEquals(tokyoInstant(2026, 9, 20, 17, 0), next)
    }

    @Test
    @DisplayName("Tam yayin aninda geri sayim sifirda takilmaz, sonraki haftaya gecer")
    fun `exactly at air time moves to next week`() {
        val now = tokyoInstant(2026, 9, 13, 17, 0)

        val next = NextEpisodeCalculator.nextAiring(broadcast(), now)

        assertEquals(tokyoInstant(2026, 9, 20, 17, 0), next)
    }

    @Test
    @DisplayName("Kullanicinin saat dilimi farkli olsa da sonuc ayni ana isaret eder")
    fun `works across user time zones`() {
        // Istanbul-da Pazar 11:00 = Tokyo-da Pazar 17:00
        val now = ZonedDateTime.of(2026, 9, 13, 10, 0, 0, 0, ZoneId.of("Europe/Istanbul"))
            .toInstant()

        val next = NextEpisodeCalculator.nextAiring(broadcast(), now)

        assertEquals(tokyoInstant(2026, 9, 13, 17, 0), next)
        assertEquals(
            11,
            next!!.atZone(ZoneId.of("Europe/Istanbul")).hour,
            "Tokyo 17:00, Istanbul saatiyle 11:00 olmali",
        )
    }

    @Test
    @DisplayName("Yaz saati uygulayan yayinci diliminde gecis dogru ele alinir")
    fun `handles daylight saving in broadcaster zone`() {
        // New York 2026-03-08-de yaz saatine geciyor. Pazar 20:00 yayin.
        val newYork = ZoneId.of("America/New_York")
        val info = BroadcastInfo(
            day = DayOfWeek.SUNDAY,
            time = LocalTime.of(20, 0),
            zone = newYork,
        )
        // Gecisten onceki Cuma.
        val now = ZonedDateTime.of(2026, 3, 6, 12, 0, 0, 0, newYork).toInstant()

        val next = NextEpisodeCalculator.nextAiring(info, now)

        val expected = ZonedDateTime.of(2026, 3, 8, 20, 0, 0, 0, newYork).toInstant()
        assertEquals(expected, next, "Yerel saat 20:00 korunmali, UTC ofseti degismeli")
    }

    @Test
    @DisplayName("Eksik yayin bilgisi null doner")
    fun `incomplete broadcast returns null`() {
        val now = Instant.now()
        assertNull(NextEpisodeCalculator.nextAiring(broadcast(day = null), now))
        assertNull(NextEpisodeCalculator.nextAiring(broadcast(time = null), now))
        assertNull(NextEpisodeCalculator.nextAiring(broadcast(zone = null), now))
        assertNull(NextEpisodeCalculator.nextAiring(BroadcastInfo(), now))
    }

    @Test
    @DisplayName("airsWithin yalnizca pencere icindeki gelecek yayinlara true doner")
    fun `airs within window`() {
        val now = tokyoInstant(2026, 9, 13, 12, 0)
        val inFiveHours = tokyoInstant(2026, 9, 13, 17, 0)
        val inTwoDays = tokyoInstant(2026, 9, 15, 12, 0)
        val twoHoursAgo = tokyoInstant(2026, 9, 13, 10, 0)

        assertTrue(NextEpisodeCalculator.airsWithin(inFiveHours, now, Duration.ofHours(6)))
        assertFalse(NextEpisodeCalculator.airsWithin(inFiveHours, now, Duration.ofHours(4)))
        assertFalse(NextEpisodeCalculator.airsWithin(inTwoDays, now, Duration.ofHours(6)))
        assertFalse(
            NextEpisodeCalculator.airsWithin(twoHoursAgo, now, Duration.ofHours(6)),
            "Gecmis yayin bildirilmemeli",
        )
        assertFalse(NextEpisodeCalculator.airsWithin(null, now, Duration.ofHours(6)))
    }
}
