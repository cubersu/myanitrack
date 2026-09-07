package com.myanitrack.core.network.rss

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * MAL-in resmi RSS beslemeleri.
 *
 * Haber akisi icin Jikan yerine bunu kullaniyoruz: Jikan-in yalnizca yapim bazli
 * haber ucu var (`/anime/{id}/news`), genel akis yok. RSS ise resmi, herkese acik,
 * anahtarsiz ve hiz sinirina takilmiyor.
 *
 * Govde ham XML olarak alinip [RssParser] ile ayristirilir; kucuk ve sabit bir
 * bicim oldugu icin XML kutuphanesi eklemeye deger bulunmadi.
 */
interface MalRssService {

    @GET("rss.php?type=news")
    suspend fun getNewsFeed(): String

    /**
     * Kullanicinin liste guncellemeleri akisi (Faz 4 - arkadas akisi).
     * `type`: "rw" (anime) ya da "rm" (manga).
     */
    @GET("rss.php")
    suspend fun getUserFeed(
        @Query("type") type: String,
        @Query("u") userName: String,
    ): String
}
