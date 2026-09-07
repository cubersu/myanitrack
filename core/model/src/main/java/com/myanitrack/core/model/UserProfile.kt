package com.myanitrack.core.model

import java.time.Instant

/** MAL kullanici profili (API v2 /users/@me). */
data class UserProfile(
    val id: Int,
    val name: String,
    val pictureUrl: String? = null,
    val gender: String? = null,
    val birthday: String? = null,
    val location: String? = null,
    val joinedAt: Instant? = null,
    val animeStatistics: AnimeStatistics? = null,
)

/** Profil sayfasindaki anime istatistikleri. */
data class AnimeStatistics(
    val numItemsWatching: Int = 0,
    val numItemsCompleted: Int = 0,
    val numItemsOnHold: Int = 0,
    val numItemsDropped: Int = 0,
    val numItemsPlanToWatch: Int = 0,
    val numItems: Int = 0,
    val numDaysWatched: Double = 0.0,
    val numEpisodes: Int = 0,
    val numTimesRewatched: Int = 0,
    val meanScore: Double = 0.0,
)
