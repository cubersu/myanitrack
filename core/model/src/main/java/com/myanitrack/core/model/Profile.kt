package com.myanitrack.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * Bir MAL kullanicisinin profil sayfasi.
 *
 * Kaynak Jikan `/users/{username}/full`. MAL API v2-nin `/users/{user_name}` ucu
 * YALNIZCA `@me` kabul ediyor; baskasinin profiline bakmak icin tek secenek Jikan.
 * Bu yuzden kendi profilimiz de ayni yoldan okunuyor - iki farkli kod yolu
 * tutmaktansa tek yol daha az sürpriz üretiyor.
 */
data class UserProfileDetails(
    val userName: String,
    val malId: Int? = null,
    val imageUrl: String? = null,
    val gender: String? = null,
    val birthday: LocalDate? = null,
    val location: String? = null,
    val joinedAt: Instant? = null,
    val lastOnlineAt: Instant? = null,
    val animeStats: UserMediaStatistics? = null,
    val mangaStats: UserMediaStatistics? = null,
) {
    val profileUrl: String get() = "https://myanimelist.net/profile/$userName"
}

/**
 * Anime ve manga istatistikleri ayni sekle sahip; alan adlari farkli
 * ("days_watched"/"days_read" gibi) ama anlamlari ortak oldugu icin tek model.
 */
data class UserMediaStatistics(
    val mediaType: MediaType,
    val daysSpent: Double = 0.0,
    val meanScore: Double = 0.0,
    val inProgress: Int = 0,
    val completed: Int = 0,
    val onHold: Int = 0,
    val dropped: Int = 0,
    val planned: Int = 0,
    val totalEntries: Int = 0,
    val repeated: Int = 0,
    /** Anime icin izlenen bolum, manga icin okunan bolum. */
    val unitsConsumed: Int = 0,
    /** Yalnizca manga: okunan cilt sayisi. */
    val volumesRead: Int = 0,
) {
    /** Durum dagilimi cubugu icin; toplam sifirsa bos liste doner. */
    val statusBreakdown: List<Pair<ListStatus, Int>>
        get() = listOf(
            ListStatus.WATCHING to inProgress,
            ListStatus.COMPLETED to completed,
            ListStatus.ON_HOLD to onHold,
            ListStatus.DROPPED to dropped,
            ListStatus.PLAN_TO_WATCH to planned,
        ).filter { it.second > 0 }
}

/** Geçmiş kaydı: kullanicinin bir yapimda kac birim ilerledigi. */
data class HistoryEntry(
    val malId: Int,
    val title: String,
    val mediaType: MediaType,
    val increment: Int,
    val date: Instant?,
)

/** Arkadas listesi ogesi. */
data class Friend(
    val userName: String,
    val imageUrl: String? = null,
    val lastOnlineAt: Instant? = null,
    val friendsSince: Instant? = null,
)

/**
 * Arkadas akisindaki tek guncelleme.
 *
 * Kaynak MAL-in kullanici RSS beslemesi (`rss.php?type=rw|rm&u=<kullanici>`);
 * orijinal MALClient-taki "friends feed" tam olarak bunu okuyordu.
 */
data class FeedUpdate(
    val userName: String,
    val malId: Int,
    val title: String,
    val mediaType: MediaType,
    /** Ham durum metni, orn. "Watching - 623 of ? episodes". */
    val statusText: String,
    val publishedAt: Instant?,
    val url: String,
) {
    val key: String get() = "$userName:$malId:${publishedAt?.epochSecond ?: 0}"
}
