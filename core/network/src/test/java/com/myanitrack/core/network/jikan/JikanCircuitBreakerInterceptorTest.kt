package com.myanitrack.core.network.jikan

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Devre kesici.
 *
 * Kritik davranis: devre YALNIZCA Jikan gercekten erisilemez oldugunda acilmali.
 * Cok erken acilirsa calisabilecek ekranlar da hata gosterir - ki ilk surumde
 * tam olarak bu oluyordu, cunku yeniden denemeler ayri hata sayiliyordu.
 */
class JikanCircuitBreakerInterceptorTest {

    private class FakeClock(var now: Long = 0L)

    private fun breaker(clock: FakeClock) =
        JikanCircuitBreakerInterceptor(clock = { clock.now })

    @Test
    @DisplayName("Ust uste uc basarisiz cagri devreyi acar")
    fun `opens after repeated failures`() {
        val subject = breaker(FakeClock())

        repeat(JikanCircuitBreakerInterceptor.FAILURE_THRESHOLD) { subject.recordOutcome(504) }

        assertTrue(subject.isOpen())
    }

    @Test
    @DisplayName("Esik altinda kalan hatalar devreyi acmaz")
    fun `stays closed below threshold`() {
        val subject = breaker(FakeClock())

        repeat(JikanCircuitBreakerInterceptor.FAILURE_THRESHOLD - 1) { subject.recordOutcome(504) }

        assertFalse(subject.isOpen())
    }

    @Test
    @DisplayName("Araya giren basarili yanit sayaci sifirlar")
    fun `success resets the failure count`() {
        val subject = breaker(FakeClock())

        subject.recordOutcome(504)
        subject.recordOutcome(504)
        subject.recordOutcome(200)
        subject.recordOutcome(504)
        subject.recordOutcome(504)

        assertFalse(subject.isOpen(), "Sayac sifirlandigi icin devre kapali kalmaliydi")
    }

    @Test
    @DisplayName("Calisan bir uc, basarisiz onyuklemenin devreyi acmasini engeller")
    fun `working endpoint keeps the circuit closed`() {
        // Gercek senaryo: /top sayfa 1 calisiyor, sayfa 2 onyuklemesi 504 aliyor.
        // Bu, uygulamanin geri kalanini kilitlememeli.
        val subject = breaker(FakeClock())

        subject.recordOutcome(200)
        subject.recordOutcome(504)
        subject.recordOutcome(200)
        subject.recordOutcome(504)

        assertFalse(subject.isOpen())
    }

    @Test
    @DisplayName("Basarili yanit acik devreyi kapatir")
    fun `success closes an open circuit`() {
        val subject = breaker(FakeClock())

        repeat(JikanCircuitBreakerInterceptor.FAILURE_THRESHOLD) { subject.recordOutcome(504) }
        assertTrue(subject.isOpen())

        subject.recordOutcome(200)

        assertFalse(subject.isOpen(), "Servis dondugunde devre kapanmaliydi")
    }

    @Test
    @DisplayName("Devre sure dolunca kendiliginden kapanir")
    fun `circuit closes after the timeout`() {
        val clock = FakeClock()
        val subject = breaker(clock)

        repeat(JikanCircuitBreakerInterceptor.FAILURE_THRESHOLD) { subject.recordOutcome(504) }
        assertTrue(subject.isOpen())

        clock.now += JikanCircuitBreakerInterceptor.CIRCUIT_OPEN_MS + 1

        assertFalse(subject.isOpen())
    }

    @Test
    @DisplayName("429 hiz sinirlayici tarafindan yonetilir, 503 devresini acmaz")
    fun `rate limit responses do not open the server failure breaker`() {
        val subject = breaker(FakeClock())

        repeat(JikanCircuitBreakerInterceptor.FAILURE_THRESHOLD) { subject.recordOutcome(429) }

        assertFalse(subject.isOpen())
    }

    @Test
    @DisplayName("Istemci hatalari (404) devreyi acmaz")
    fun `client errors do not open the breaker`() {
        val subject = breaker(FakeClock())

        repeat(5) { subject.recordOutcome(404) }

        assertFalse(subject.isOpen(), "404 sunucu sorunu degil; devre acilmamali")
    }
}
