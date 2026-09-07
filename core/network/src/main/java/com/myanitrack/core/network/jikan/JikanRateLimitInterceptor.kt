package com.myanitrack.core.network.jikan

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Jikan-in yayimladigi istek limitini ISTEMCI TARAFINDA zorlar: 3 istek/saniye
 * ve 60 istek/dakika.
 *
 * 429 yedigimizde geri cekilmek yerine limiti hic asmamayi tercih ediyoruz;
 * boylece hem Jikan-in ucretsiz altyapisina saygili davranmis oluyoruz hem de
 * kullanici gereksiz gecikme yasamiyor.
 *
 * Calisma bicimi: son gonderilen isteklerin zaman damgalari kayan bir pencerede
 * tutulur. Yeni istek her iki pencereye de sigmiyorsa, sigacagi ana kadar
 * cagiran is parcacigi uyutulur. OkHttp interceptor-lari zaten arka plan is
 * parcaciklarinda calistigi icin bloklama guvenlidir.
 */
// Not: @Inject constructor yerine NetworkModule icinde @Provides ile uretiliyor.
// Hilt varsayilan degerli constructor parametrelerini kabul etmiyor; saat ve uyku
// islevlerini disaridan verilebilir birakmak ise testler icin gerekli.
class JikanRateLimitInterceptor(
    private val clock: () -> Long,
    private val sleeper: (Long) -> Unit,
) : Interceptor {

    /** Gonderim zamanlari, eskiden yeniye. Yalnizca [lock] altinda erisilir. */
    private val recentRequests = ArrayDeque<Long>()
    private val lock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        awaitSlot()
        return chain.proceed(chain.request())
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

    /** Iki pencereden hangisi daha uzun beklemeyi gerektiriyorsa o kazanir. */
    private fun requiredDelay(now: Long): Long {
        val perSecondDelay = nthNewestTimestamp(MAX_PER_SECOND)
            ?.let { it + SECOND_WINDOW_MS - now }
            ?: 0L
        val perMinuteDelay = nthNewestTimestamp(MAX_PER_MINUTE)
            ?.let { it + MINUTE_WINDOW_MS - now }
            ?: 0L
        return maxOf(perSecondDelay, perMinuteDelay)
    }

    /**
     * Pencere doldugunda serbest kalacak ilk yuvanin zaman damgasi.
     * Henuz [limit] kadar istek yapilmadiysa null (bekleme gerekmez).
     */
    private fun nthNewestTimestamp(limit: Int): Long? =
        if (recentRequests.size < limit) null else recentRequests.elementAt(recentRequests.size - limit)

    private fun purgeOlderThan(threshold: Long) {
        while (recentRequests.isNotEmpty() && recentRequests.first() <= threshold) {
            recentRequests.removeFirst()
        }
    }

    internal companion object {
        const val MAX_PER_SECOND = 3
        const val MAX_PER_MINUTE = 60
        const val SECOND_WINDOW_MS = 1_000L
        const val MINUTE_WINDOW_MS = 60_000L

        /** Tek seferde uzun uyumak yerine bolerek uyu; iptal tepkisi hizli kalsin. */
        const val MAX_SINGLE_SLEEP_MS = 1_000L
    }
}
