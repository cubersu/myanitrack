package com.myanitrack.core.network.jikan

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Jikan erisilemez oldugunda israrci olmayi birakir.
 *
 * Jikan, MAL-a ulasamadiginda onbelleginde olmayan her uc icin 504 doner. Bu
 * bizim duzeltebilecegimiz bir sey degil; istek gondermeye devam etmek yalnizca
 * dakikalik istek butcesini yakar ve ustune 429 aldirir.
 *
 * Ust uste [FAILURE_THRESHOLD] basarisiz cagridan sonra devre acilir ve
 * [CIRCUIT_OPEN_MS] boyunca istekler AGA HIC CIKMADAN 503 ile doner. Repository
 * katmani bayat onbellege duser, kullanici bosuna beklemez. Tek bir basarili
 * yanit devreyi hemen kapatir.
 */
class JikanCircuitBreakerInterceptor(
    private val clock: () -> Long = { System.currentTimeMillis() },
) : Interceptor {

    private var consecutiveFailures = 0
    private var openUntil = 0L
    private val lock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        if (isOpen()) {
            return unavailableResponse(chain)
        }

        val response = chain.proceed(chain.request())
        recordOutcome(response.code)
        return response
    }

    internal fun isOpen(): Boolean = synchronized(lock) { clock() < openUntil }

    internal fun recordOutcome(code: Int) {
        synchronized(lock) {
            // 429 (Too Many Requests) artik devreyi acmiyor; JikanRateLimitInterceptor
            // zaten bekletiyor. Devre sadece 5xx (sunucu coktuyse) hatalariyla acilir.
            if (code in UPSTREAM_FAILURE_CODES) {
                consecutiveFailures++
                if (consecutiveFailures >= FAILURE_THRESHOLD) {
                    openUntil = clock() + CIRCUIT_OPEN_MS
                    consecutiveFailures = 0
                }
            } else if (code == HTTP_OK || code == HTTP_NOT_FOUND) {
                // Basarili yanit veya kesin "yok" (404) devreyi kapatir.
                consecutiveFailures = 0
                openUntil = 0L
            }
        }
    }

    private fun unavailableResponse(chain: Interceptor.Chain): Response = Response.Builder()
        .request(chain.request())
        .protocol(Protocol.HTTP_1_1)
        .code(HTTP_SERVICE_UNAVAILABLE)
        .message("Jikan unavailable (circuit open)")
        .body("".toResponseBody(null))
        .build()

    internal companion object {
        private val UPSTREAM_FAILURE_CODES = listOf(
            500, // Internal Server Error
            502, // Bad Gateway
            503, // Service Unavailable
            504  // Gateway Timeout
        )

        const val HTTP_OK = 200
        const val HTTP_NOT_FOUND = 404
        const val HTTP_SERVICE_UNAVAILABLE = 503

        /**
         * Ust uste kac hata sonrasi devre acilir.
         *
         * 3-ten 8-e cikarildi: Sayfa yuklenirken ayni anda bircok kaynak (karakterler,
         * staff, oneriler vb.) istenir. Jikan kisa bir sure 504 verirse 3 hata cok
         * cabuk birikir ve tum uygulamayi 1 dakika kilitler. 8 daha toleransli.
         */
        const val FAILURE_THRESHOLD = 8

        /** Devre ne kadar acik kalsin (ms). 60s -> 20s. */
        const val CIRCUIT_OPEN_MS = 20_000L
    }
}
