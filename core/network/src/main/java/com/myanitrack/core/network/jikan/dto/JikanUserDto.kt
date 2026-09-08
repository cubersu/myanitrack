package com.myanitrack.core.network.jikan.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Jikan `/users/{username}/full` govdesi. */
@Serializable
data class JikanUserProfileDto(
    @SerialName("mal_id") val malId: Int? = null,
    val username: String = "",
    val url: String? = null,
    val images: JikanImageSetDto? = null,
    @SerialName("last_online") val lastOnline: String? = null,
    val gender: String? = null,
    val birthday: String? = null,
    val location: String? = null,
    val joined: String? = null,
    val statistics: JikanUserStatisticsDto? = null,
)

@Serializable
data class JikanUserStatisticsDto(
    val anime: JikanAnimeStatsDto? = null,
    val manga: JikanMangaStatsDto? = null,
)

@Serializable
data class JikanAnimeStatsDto(
    @SerialName("days_watched") val daysWatched: Double = 0.0,
    @SerialName("mean_score") val meanScore: Double = 0.0,
    val watching: Int = 0,
    val completed: Int = 0,
    @SerialName("on_hold") val onHold: Int = 0,
    val dropped: Int = 0,
    @SerialName("plan_to_watch") val planToWatch: Int = 0,
    @SerialName("total_entries") val totalEntries: Int = 0,
    val rewatched: Int = 0,
    @SerialName("episodes_watched") val episodesWatched: Int = 0,
)

@Serializable
data class JikanMangaStatsDto(
    @SerialName("days_read") val daysRead: Double = 0.0,
    @SerialName("mean_score") val meanScore: Double = 0.0,
    val reading: Int = 0,
    val completed: Int = 0,
    @SerialName("on_hold") val onHold: Int = 0,
    val dropped: Int = 0,
    @SerialName("plan_to_read") val planToRead: Int = 0,
    @SerialName("total_entries") val totalEntries: Int = 0,
    val reread: Int = 0,
    @SerialName("chapters_read") val chaptersRead: Int = 0,
    @SerialName("volumes_read") val volumesRead: Int = 0,
)

/** `/users/{username}/history` ogesi. */
@Serializable
data class JikanHistoryDto(
    val entry: JikanMalUrlDto = JikanMalUrlDto(),
    val increment: Int = 0,
    val date: String? = null,
)

/** Jikan-in "parsed URL" nesnesi: kimlik, tur, ad ve adres. */
@Serializable
data class JikanMalUrlDto(
    @SerialName("mal_id") val malId: Int = 0,
    val type: String? = null,
    val name: String = "",
    val url: String? = null,
)

/** `/users/{username}/friends` ogesi. */
@Serializable
data class JikanFriendDto(
    val user: JikanUserMetaDto = JikanUserMetaDto(),
    @SerialName("last_online") val lastOnline: String? = null,
    @SerialName("friends_since") val friendsSince: String? = null,
)
