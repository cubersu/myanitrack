package com.myanitrack.core.network.jikan

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Gecici Jikan hatalarinda ustel geri cekilme ile yeniden dener.
 *
 * Jikan ucretsiz ve topluluk tarafindan isletiliyor; MAL-a ulasamadiginda 503/504
 * dondurmesi olagan. Limit asilirsa 429 gelir. Bunlarin tamami gecicidir, kullaniciya
 * hata gostermeden once birkac kez denemek dogru davranistir.
 *
 * 429 disindaki 4xx tekrar denenmez: istek zaten hatali, tekrarlamak anlamsiz.
 * Son denemede yanit oldugu gibi dondurulur ki repository katmani gercek durum
 * kodunu gorup [com.myanitrack.core.common.result.AppError] ile eslestirebilsin.
 */
// Not: @Inject constructor yerine NetworkModule icinde @Provides ile uretiliyor
// (bkz. JikanRateLimitInterceptor).
class JikanRetryInterceptor(
    private val sleeper: (Long) -> Unit,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var lastFailure: IOException? = null

        for (attempt in 0 until MAX_ATTEMPTS) {
            val isLastAttempt = attempt == MAX_ATTEMPTS - 1
            if (attempt > 0) sleeper(backoffMillis(attempt))

            try {
                val response = chain.proceed(chain.request())
                if (isLastAttempt || !response.isRetryable()) return response
                // Govde tuketilmezse baglanti havuza donmez.
                response.close()
                lastFailure = null
            } catch (io: IOException) {
                // Ag kesintisi de gecici olabilir; son denemede yukari firlatilir.
                if (isLastAttempt) throw io
                lastFailure = io
            }
        }

        throw lastFailure ?: IOException("Jikan request failed after $MAX_ATTEMPTS attempts")
    }

    private fun Response.isRetryable(): Boolean =
        code == HTTP_TOO_MANY_REQUESTS || code in RETRYABLE_SERVER_CODES

    /** 1s, 2s, 4s ... ust sinir 8s. */
    private fun backoffMillis(attempt: Int): Long =
        (BASE_DELAY_MS shl (attempt - 1)).coerceAtMost(MAX_DELAY_MS)

    internal companion object {
        /**
         * Toplam deneme sayisi.
         *
         * 3-ten 2-ye dusuruldu: Jikan 504 dondugunde sorun MAL-in erisilemez
         * olmasi: birkac saniye icinde tekrar denemek duzeltmiyor, yalnizca
         * dakikalik istek butcesini yakip ustune 429 aldiriyordu. Israrli
         * yeniden deneme yerine devre kesici devreye giriyor
         * (bkz. JikanRateLimitInterceptor).
         */
        const val MAX_ATTEMPTS = 2
        const val BASE_DELAY_MS = 1_000L
        const val MAX_DELAY_MS = 8_000L
        const val HTTP_TOO_MANY_REQUESTS = 429
        val RETRYABLE_SERVER_CODES = setOf(500, 502, 503, 504)
    }
}
