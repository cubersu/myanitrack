package com.myanitrack.core.network.jikan

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Jikan trafiginin tek gecis kapisi: hiz siniri + devre kesici.
 *
 * ## Neden "saniyede 3" degil de "her 360 ms-de bir"
 * Jikan dokumani "3 istek/saniye" diyor, ama sunucu tarafinda bu nginx `limit_req`
 * ile uygulaniyor: kova saniyede 3 jeton hizinda DOLAR, yani istekler ~333 ms
 * araliklarla kabul edilir. Ayni anda gonderilen 3 istekte birincisi gecer,
 * digerleri 429 alir. Bu yuzden istekler ARALIKLI gonderiliyor.
 *
 * ## Devre kesici
 * Jikan MAL-a ulasamadiginda 504 doner. Bu, bizim duzeltebilecegimiz bir sey
 * degil; israrla istek gondermek yalnizca dakika butcesini yakar ve ustune 429
 * aldirir. Ust uste [FAILURE_THRESHOLD] hatadan sonra devre acilir ve
 * [CIRCUIT_OPEN_MS] boyunca istekler AGA HIC CIKMADAN 503 ile doner; repository
 * katmani bayat onbellege duser, kullanici bosuna beklemez.
 *
 * Tek bir basarili yanit devreyi kapatir.
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

    /** Ust uste gelen sunucu hatasi sayisi; devre kesiciyi besler. */
    private var consecutiveFailures = 0

    /** Devre acikken bu ana kadar istek DENENMEZ (beklemeden hata doner). */
    private var circuitOpenUntil = 0L

    private val lock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        // Devre acikken beklemeden hata don: Jikan zaten yanit veremiyor,
        // kuyrukta beklemek kullaniciyi bosuna oyalar.
        if (isCircuitOpen()) return unavailableResponse(chain)

        awaitSlot()
        val response = chain.proceed(chain.request())

        recordResponse(response.code)
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
    internal fun awaitSlot() {
        while (true) {
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

    internal fun isCircuitOpen(): Boolean = synchronized(lock) { clock() < circuitOpenUntil }

    /** Yanit koduna gore devre kesiciyi gunceller. */
    internal fun recordResponse(code: Int) {
        synchronized(lock) {
            if (code in UPSTREAM_FAILURE_CODES || code == HTTP_TOO_MANY_REQUESTS) {
                consecutiveFailures++
                if (consecutiveFailures >= FAILURE_THRESHOLD) {
                    circuitOpenUntil = clock() + CIRCUIT_OPEN_MS
                    consecutiveFailures = 0
                }
            } else {
                // Tek bir basarili yanit servisin dondugunu gosterir.
                consecutiveFailures = 0
                circuitOpenUntil = 0L
            }
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
        /**
         * Ardisik istekler arasindaki en kisa sure.
         * 3 istek/sn siniri icin teorik alt sinir ~334 ms; saat sapmalarina karsi
         * biraz pay birakiliyor.
         */
        const val MIN_INTERVAL_MS = 360L

        const val MAX_PER_MINUTE = 60
        const val MINUTE_WINDOW_MS = 60_000L
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HTTP_SERVICE_UNAVAILABLE = 503

        /** `Retry-After` yoksa kullanilacak duraklama. */
        const val DEFAULT_SERVER_BACKOFF_MS = 2_000L

        /** Sunucu cok kisa bir sure onerse bile en az bu kadar bekle. */
        const val MIN_SERVER_BACKOFF_MS = 1_000L

        /** Tek seferde uzun uyumak yerine bolerek uyu; iptal tepkisi hizli kalsin. */
        const val MAX_SINGLE_SLEEP_MS = 1_000L

        /** Jikan MAL-a ulasamadiginda donen kodlar. */
        val UPSTREAM_FAILURE_CODES = setOf(500, 502, 503, 504)

        /** Kac ust uste hatadan sonra devre acilsin. */
        const val FAILURE_THRESHOLD = 3

        /** Devre ne kadar acik kalsin. */
        const val CIRCUIT_OPEN_MS = 60_000L
    }
}
