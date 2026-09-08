package com.myanitrack.core.network.jikan

import com.myanitrack.core.network.jikan.JikanRateLimitInterceptor.Companion.MAX_PER_MINUTE
import com.myanitrack.core.network.jikan.JikanRateLimitInterceptor.Companion.MIN_INTERVAL_MS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Hiz sinirlayici gercek zaman yerine sahte bir saat kullanir; testler
 * bekleme yapmadan davranisi dogrular.
 *
 * Onemli davranis: istekler ARALIKLI gonderilmeli. Onceki surum "saniyede 3"
 * kuralini patlamaya izin vererek uyguluyordu ve Jikan (nginx `limit_req`)
 * es zamanli gelen isteklere 429 donuyordu.
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
    @DisplayName("Ilk istek beklemeden gecer")
    fun `first request passes immediately`() {
        val clock = FakeClock()

        interceptor(clock).awaitSlot()

        assertTrue(clock.sleeps.isEmpty())
    }

    @Test
    @DisplayName("Ardisik istekler en az MIN_INTERVAL_MS araliklarla gonderilir")
    fun `requests are spaced apart`() {
        val clock = FakeClock()
        val subject = interceptor(clock)

        val sendTimes = mutableListOf<Long>()
        repeat(5) {
            subject.awaitSlot()
            sendTimes += clock.now
        }

        sendTimes.zipWithNext { earlier, later ->
            assertTrue(
                later - earlier >= MIN_INTERVAL_MS,
                "Istekler arasi bosluk ${later - earlier} ms; en az $MIN_INTERVAL_MS olmaliydi",
            )
        }
    }

    @Test
    @DisplayName("Es zamanli uc istek patlama yapmaz (429 sebebi buydu)")
    fun `no burst of three`() {
        val clock = FakeClock()
        val subject = interceptor(clock)

        subject.awaitSlot()
        subject.awaitSlot()
        subject.awaitSlot()

        assertTrue(
            clock.now >= 2 * MIN_INTERVAL_MS,
            "Uc istek en az ${2 * MIN_INTERVAL_MS} ms-e yayilmaliydi, ${clock.now} ms surdu",
        )
    }

    @Test
    @DisplayName("Yeterli sure gectiyse beklenmez")
    fun `no wait when enough time passed`() {
        val clock = FakeClock()
        val subject = interceptor(clock)

        subject.awaitSlot()
        clock.now += MIN_INTERVAL_MS + 1
        clock.sleeps.clear()

        subject.awaitSlot()

        assertTrue(clock.sleeps.isEmpty())
    }

    @Test
    @DisplayName("Dakika penceresi de zorlanir")
    fun `minute window is enforced`() {
        val clock = FakeClock()
        val subject = interceptor(clock)

        // Araliklama kuralina takilmadan MAX_PER_MINUTE istek gonder.
        repeat(MAX_PER_MINUTE) {
            subject.awaitSlot()
            clock.now += MIN_INTERVAL_MS
        }
        clock.sleeps.clear()

        subject.awaitSlot()

        // Ilk istek t=0; dakika penceresi t=60000-de acilir.
        assertTrue(
            clock.now >= 60_000L,
            "Dakika siniri sonrasi en erken 60000 ms bekleniyordu, ${clock.now} oldu",
        )
    }

    @Test
    @DisplayName("Sunucu 429 dediginde tum trafik duraklatilir")
    fun `server backoff pauses all traffic`() {
        val clock = FakeClock()
        val subject = interceptor(clock)

        subject.applyServerBackoff(millis = 5_000L)
        val before = clock.now

        subject.awaitSlot()

        assertTrue(
            clock.now - before >= 5_000L,
            "429 sonrasi en az 5000 ms beklenmeliydi, ${clock.now - before} ms beklendi",
        )
    }

    @Test
    @DisplayName("Cok kisa Retry-After degeri alt sinira yukseltilir")
    fun `server backoff has a floor`() {
        val clock = FakeClock()
        val subject = interceptor(clock)

        subject.applyServerBackoff(millis = 10L)
        subject.awaitSlot()

        assertTrue(
            clock.now >= JikanRateLimitInterceptor.MIN_SERVER_BACKOFF_MS,
            "Alt sinir uygulanmaliydi, ${clock.now} ms beklendi",
        )
    }

    @Test
    @DisplayName("Daha uzun bir duraklama kisasini ezmez")
    fun `longer backoff wins`() {
        val clock = FakeClock()
        val subject = interceptor(clock)

        subject.applyServerBackoff(millis = 10_000L)
        subject.applyServerBackoff(millis = 1_000L)
        subject.awaitSlot()

        assertTrue(clock.now >= 10_000L, "Uzun duraklama korunmaliydi, ${clock.now} ms")
    }

    @Test
    @DisplayName("Uyku tek seferde 1 saniyeyi asmaz, gerekirse bolunur")
    fun `sleeps are chunked`() {
        val clock = FakeClock()
        val subject = interceptor(clock)

        subject.applyServerBackoff(millis = 5_000L)
        subject.awaitSlot()

        assertTrue(clock.sleeps.isNotEmpty())
        assertEquals(
            emptyList<Long>(),
            clock.sleeps.filter { it > 1_000L },
            "Tek bir uyku 1000 ms-i asmamali",
        )
    }
}
