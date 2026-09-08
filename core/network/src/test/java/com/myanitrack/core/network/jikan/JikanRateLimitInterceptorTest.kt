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

    // --- Devre kesici ---

    @Test
    @DisplayName("Ust uste uc sunucu hatasi devreyi acar")
    fun `opens after repeated upstream failures`() {
        val clock = FakeClock()
        val subject = interceptor(clock)

        repeat(JikanRateLimitInterceptor.FAILURE_THRESHOLD) { subject.recordResponse(504) }

        assertTrue(subject.isCircuitOpen(), "Devre acilmaliydi")
    }

    @Test
    @DisplayName("Esik altinda kalan hatalar devreyi acmaz")
    fun `stays closed below threshold`() {
        val clock = FakeClock()
        val subject = interceptor(clock)

        repeat(JikanRateLimitInterceptor.FAILURE_THRESHOLD - 1) { subject.recordResponse(504) }

        assertTrue(!subject.isCircuitOpen())
    }

    @Test
    @DisplayName("Araya giren basarili yanit sayaci sifirlar")
    fun `success resets the failure count`() {
        val clock = FakeClock()
        val subject = interceptor(clock)

        subject.recordResponse(504)
        subject.recordResponse(504)
        subject.recordResponse(200)
        subject.recordResponse(504)
        subject.recordResponse(504)

        assertTrue(!subject.isCircuitOpen(), "Sayac sifirlandigi icin devre kapali kalmaliydi")
    }

    @Test
    @DisplayName("Basarili yanit acik devreyi kapatir")
    fun `success closes an open circuit`() {
        val clock = FakeClock()
        val subject = interceptor(clock)

        repeat(JikanRateLimitInterceptor.FAILURE_THRESHOLD) { subject.recordResponse(504) }
        assertTrue(subject.isCircuitOpen())

        subject.recordResponse(200)

        assertTrue(!subject.isCircuitOpen(), "Servis dondugunde devre kapanmaliydi")
    }

    @Test
    @DisplayName("Devre sure dolunca kendiliginden kapanir")
    fun `circuit closes after the timeout`() {
        val clock = FakeClock()
        val subject = interceptor(clock)

        repeat(JikanRateLimitInterceptor.FAILURE_THRESHOLD) { subject.recordResponse(504) }
        assertTrue(subject.isCircuitOpen())

        clock.now += JikanRateLimitInterceptor.CIRCUIT_OPEN_MS + 1

        assertTrue(!subject.isCircuitOpen())
    }

    @Test
    @DisplayName("429 da devre kesiciyi besler")
    fun `rate limit responses feed the breaker`() {
        val clock = FakeClock()
        val subject = interceptor(clock)

        repeat(JikanRateLimitInterceptor.FAILURE_THRESHOLD) { subject.recordResponse(429) }

        assertTrue(subject.isCircuitOpen())
    }

    @Test
    @DisplayName("Istemci hatalari (404) devreyi acmaz")
    fun `client errors do not open the breaker`() {
        val clock = FakeClock()
        val subject = interceptor(clock)

        repeat(5) { subject.recordResponse(404) }

        assertTrue(!subject.isCircuitOpen(), "404 sunucu sorunu degil; devre acilmamali")
    }
}
