package com.myanitrack.core.network.jikan

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Jikan istek limitini istemci tarafinda zorlar.
 *
 * ## Neden "saniyede 3" degil de "her 360 ms-de bir"
 * Jikan dokumani "3 istek/saniye" diyor, ama sunucu tarafinda bu nginx `limit_req`
 * ile uygulaniyor: kova saniyede 3 jeton hizinda DOLAR, yani istekler ~333 ms
 * araliklarla kabul edilir. Ayni anda gonderilen 3 istekte birincisi gecer,
 * digerleri 429 alir. Bu yuzden istekler ARALIKLI gonderiliyor.
 *
 * Devre kesici ayri bir katmanda: bkz. [JikanCircuitBreakerInterceptor].
 *
 * ## Sunucudan gelen geri bildirim
 * 429 gelirse `Retry-After` kadar - yoksa varsayilan sure kadar - tum Jikan
 * trafigi duraklatilir; yeniden deneme katmani limiti daha da zorlamak yerine bekler.
 *
 * Bloklama guvenli: OkHttp interceptor-lari zaten arka plan is parcaciklarinda calisir.
 */
class JikanRateLimitInterceptor(
    private val clock: () -> Long,
    private val sleeper: (Long) -> Unit,
) : Interceptor {

    /** Gonderim zamanlari, eskiden yeniye. Yalnizca [lock] altinda erisilir. */
    private val recentRequests = ArrayDeque<Long>()

    /** Sunucu 429 dediginde bu ana kadar hic istek gonderilmez. */
    private var cooldownUntil = 0L

    private val lock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        awaitSlot { chain.call().isCanceled() }
        val response = chain.proceed(chain.request())

        if (response.code == HTTP_TOO_MANY_REQUESTS) {
            applyServerBackoff(response.retryAfterMillis())
        }
        return response
    }

    /**
     * Bir gonderim yuvasi acilana kadar bekler ve yuvayi kaydeder.
     *
     * `internal`: OkHttp 5-in `Interceptor.Chain` arayuzu testte taklit edilemeyecek
     * kadar genis oldugu icin birim testleri bu islevi dogrudan surer.
     */
    internal fun awaitSlot(isCanceled: () -> Boolean = { false }) {
        while (true) {
            if (isCanceled()) throw IOException("Canceled")
            val waitMillis = synchronized(lock) {
                val now = clock()
                purgeOlderThan(now - MINUTE_WINDOW_MS)

                val delay = requiredDelay(now)
                if (delay <= 0L) {
                    recentRequests.addLast(now)
                    return
                }
                delay
            }
            sleeper(waitMillis.coerceAtMost(MAX_SINGLE_SLEEP_MS))
        }
    }

    /** Sunucu 429 dedi: en az [millis] boyunca hicbir istek gonderme. */
    internal fun applyServerBackoff(millis: Long) {
        synchronized(lock) {
            val until = clock() + millis.coerceAtLeast(MIN_SERVER_BACKOFF_MS)
            if (until > cooldownUntil) cooldownUntil = until
        }
    }

    /** Uc kisittan en uzun beklemeyi gerektiren kazanir. */
    private fun requiredDelay(now: Long): Long {
        val cooldownDelay = cooldownUntil - now

        // Ardisik istekler arasinda en az MIN_INTERVAL_MS bosluk.
        val spacingDelay = recentRequests.lastOrNull()
            ?.let { it + MIN_INTERVAL_MS - now }
            ?: 0L

        // Dakikada en fazla MAX_PER_MINUTE istek.
        val perMinuteDelay = nthNewestTimestamp(MAX_PER_MINUTE)
            ?.let { it + MINUTE_WINDOW_MS - now }
            ?: 0L

        return maxOf(cooldownDelay, spacingDelay, perMinuteDelay)
    }

    /**
     * Pencere doldugunda serbest kalacak ilk yuvanin zaman damgasi.
     * Henuz [limit] kadar istek yapilmadiysa null (bekleme gerekmez).
     */
    private fun nthNewestTimestamp(limit: Int): Long? =
        if (recentRequests.size < limit) {
            null
        } else {
            recentRequests.elementAt(recentRequests.size - limit)
        }

    private fun purgeOlderThan(threshold: Long) {
        while (recentRequests.isNotEmpty() && recentRequests.first() <= threshold) {
            recentRequests.removeFirst()
        }
    }

    private fun Response.retryAfterMillis(): Long =
        header("Retry-After")?.toLongOrNull()?.times(1_000L) ?: DEFAULT_SERVER_BACKOFF_MS


    internal companion object {
        /**
         * Ardisik istekler arasindaki en kisa sure.
         * Jikan 3 req/sec diyor ama sunucu tarafindaki Nginx sinirlari bazen
         * daha katı olabiliyor. 500ms (2 req/sec) cok daha guvenli bir sınır.
         */
        const val MIN_INTERVAL_MS = 500L

        const val MAX_PER_MINUTE = 60
        const val MINUTE_WINDOW_MS = 60_000L
        const val HTTP_TOO_MANY_REQUESTS = 429

        /** `Retry-After` yoksa kullanilacak duraklama. */
        const val DEFAULT_SERVER_BACKOFF_MS = 2_000L

        /** Sunucu cok kisa bir sure onerse bile en az bu kadar bekle. */
        const val MIN_SERVER_BACKOFF_MS = 1_000L

        /** Tek seferde uzun uyumak yerine bolerek uyu; iptal tepkisi hizli kalsin. */
        const val MAX_SINGLE_SLEEP_MS = 1_000L
    }
}
