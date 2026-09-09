package com.myanitrack.core.network.jikan

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Retries rate limits and connection failures once, respecting cancellation.
 * Upstream 5xx responses return immediately to allow cache/MAL fallback.
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
            if (chain.call().isCanceled()) throw IOException("Canceled")
            val isLastAttempt = attempt == MAX_ATTEMPTS - 1
            if (attempt > 0) sleeper(backoffMillis(attempt))
            if (chain.call().isCanceled()) throw IOException("Canceled")

            try {
                val response = chain.proceed(chain.request())
                if (isLastAttempt || !response.isRetryable()) return response
                // Govde tuketilmezse baglanti havuza donmez.
                response.close()
                lastFailure = null
            } catch (io: IOException) {
                // Ag kesintisi de gecici olabilir; son denemede yukari firlatilir.
                if (isLastAttempt || chain.call().isCanceled()) throw io
                lastFailure = io
            }
        }

        throw lastFailure ?: IOException("Jikan request failed after $MAX_ATTEMPTS attempts")
    }

    private fun Response.isRetryable(): Boolean =
        // A Jikan upstream failure is not repaired by immediately scraping MAL again.
        // Return it promptly so repositories can use cached data or MAL directly.
        code == HTTP_TOO_MANY_REQUESTS

    /** 1s, 2s, 4s ... ust sinir 8s. */
    private fun backoffMillis(attempt: Int): Long =
        (BASE_DELAY_MS shl (attempt - 1)).coerceAtMost(MAX_DELAY_MS)

    internal companion object {
        /** Initial request plus at most one retry. */
        const val MAX_ATTEMPTS = 2
        const val BASE_DELAY_MS = 1_000L
        const val MAX_DELAY_MS = 8_000L
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
