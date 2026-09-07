package com.myanitrack.core.network.jikan

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Hiz sinirlayici gercek zaman yerine sahte bir saat kullanir; testler
 * bekleme yapmadan davranisi dogrular.
 */
class JikanRateLimitInterceptorTest {

    /** Sahte saat: uyku cagrilari zamani ilerletir. */
    private class FakeClock(var now: Long = 0L) {
        val sleeps = mutableListOf<Long>()

        fun sleep(millis: Long) {
            sleeps += millis
            now += millis
        }
    }

    private fun interceptor(clock: FakeClock) =
        JikanRateLimitInterceptor(clock = { clock.now }, sleeper = clock::sleep)

    @Test
    @DisplayName("Saniyedeki ilk 3 istek beklemeden gecer")
    fun `first three requests pass immediately`() {
        val clock = FakeClock()
        val subject = interceptor(clock)

        repeat(3) { subject.awaitSlot() }

        assertTrue(clock.sleeps.isEmpty(), "Ilk 3 istek beklememeliydi: ${clock.sleeps}")
    }

    @Test
    @DisplayName("Saniyedeki 4. istek pencere acilana kadar bekler")
    fun `fourth request waits for the one second window`() {
        val clock = FakeClock()
        val subject = interceptor(clock)

        repeat(3) { subject.awaitSlot() }
        subject.awaitSlot()

        assertTrue(clock.sleeps.isNotEmpty(), "4. istek beklemeliydi")
        // Ilk istek t=0-da yapildi; 4. istek en erken t=1000-de gonderilebilir.
        assertTrue(clock.now >= 1_000L, "Beklenen en erken gonderim 1000ms, gerceklesen ${clock.now}")
    }

    @Test
    @DisplayName("Saniye penceresi kaydiginda tekrar beklemeden gecilir")
    fun `window slides`() {
        val clock = FakeClock()
        val subject = interceptor(clock)

        repeat(3) { subject.awaitSlot() }
        clock.now += 1_001L
        clock.sleeps.clear()

        subject.awaitSlot()

        assertTrue(clock.sleeps.isEmpty(), "Pencere kaydiktan sonra beklenmemeliydi")
    }

    @Test
    @DisplayName("Dakikadaki 61. istek dakika penceresi acilana kadar bekler")
    fun `minute window is enforced`() {
        val clock = FakeClock()
        val subject = interceptor(clock)

        // Saniye limitine takilmadan 60 istek: her istek arasinda 400ms ilerlet.
        repeat(60) {
            subject.awaitSlot()
            clock.now += 400L
        }
        val beforeMinuteWait = clock.now
        clock.sleeps.clear()

        subject.awaitSlot()

        assertTrue(clock.sleeps.isNotEmpty(), "61. istek dakika penceresi icin beklemeliydi")
        assertTrue(
            clock.now > beforeMinuteWait,
            "Dakika penceresi zaman ilerletmeliydi",
        )
        // Ilk istek t=0; dakika penceresi t=60000-de acilir.
        assertTrue(clock.now >= 60_000L, "En erken 60000ms bekleniyordu, gerceklesen ${clock.now}")
    }

    @Test
    @DisplayName("Uyku tek seferde 1 saniyeyi asmaz, gerekirse bolunur")
    fun `sleeps are chunked`() {
        val clock = FakeClock()
        val subject = interceptor(clock)

        repeat(60) { subject.awaitSlot() }
        clock.sleeps.clear()
        subject.awaitSlot()

        assertTrue(clock.sleeps.isNotEmpty())
        assertEquals(
            emptyList<Long>(),
            clock.sleeps.filter { it > 1_000L },
            "Tek bir uyku 1000ms-i asmamali",
        )
    }
}
