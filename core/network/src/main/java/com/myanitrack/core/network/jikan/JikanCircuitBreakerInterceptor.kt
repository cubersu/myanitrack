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
 *
 * ## Neden yeniden deneyiciden DISARIDA
 * Bu interceptor zincirde [JikanRetryInterceptor]-in disinda duruyor; boylece bir
 * MANTIKSAL cagri, kac kez yeniden denenmis olursa olsun, sayaci yalnizca bir kez
 * artirir. Ic tarafta olsaydi tek bir basarisiz istek sayaci ikiye katlar ve
 * devre, Jikan aslinda calisirken bile acilirdi (orn. yalnizca ikinci sayfanin
 * onyuklemesi basarisiz oldugunda).
 */
class JikanCircuitBreakerInterceptor(
    private val clock: () -> Long,
) : Interceptor {

    private var consecutiveFailures = 0
    private var openUntil = 0L
    private val lock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        if (isOpen()) return unavailableResponse(chain)

        val response = chain.proceed(chain.request())
        recordOutcome(response.code)
        return response
    }

    internal fun isOpen(): Boolean = synchronized(lock) { clock() < openUntil }

    /** Mantiksal cagrinin SONUCUNU kaydeder (yeniden denemeler dahil, tek kez). */
    internal fun recordOutcome(code: Int) {
        synchronized(lock) {
            if (code in UPSTREAM_FAILURE_CODES || code == HTTP_TOO_MANY_REQUESTS) {
                consecutiveFailures++
                if (consecutiveFailures >= FAILURE_THRESHOLD) {
                    openUntil = clock() + CIRCUIT_OPEN_MS
                    consecutiveFailures = 0
                }
            } else {
                consecutiveFailures = 0
                openUntil = 0L
            }
        }
    }

    /**
     * Devre acikken uretilen sentetik yanit.
     *
     * 503 seciliyor cunku repository katmani bunu zaten
     * [com.myanitrack.core.common.result.AppError.Server] ile esliyor ve bayat
     * onbellege dusuyor; yeni bir hata turu eklemeye gerek yok.
     */
    private fun unavailableResponse(chain: Interceptor.Chain): Response = Response.Builder()
        .request(chain.request())
        .protocol(Protocol.HTTP_1_1)
        .code(HTTP_SERVICE_UNAVAILABLE)
        .message("Jikan unavailable (circuit open)")
        .body("".toResponseBody(null))
        .build()

    internal companion object {
        /** Jikan MAL-a ulasamadiginda donen kodlar. */
        val UPSTREAM_FAILURE_CODES = setOf(500, 502, 503, 504)

        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HTTP_SERVICE_UNAVAILABLE = 503

        /** Kac ust uste BASARISIZ CAGRIDAN sonra devre acilsin. */
        const val FAILURE_THRESHOLD = 3

        /** Devre ne kadar acik kalsin. */
        const val CIRCUIT_OPEN_MS = 60_000L
    }
}
