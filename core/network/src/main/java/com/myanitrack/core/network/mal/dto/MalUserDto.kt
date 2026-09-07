package com.myanitrack.core.network.mal.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MalUserDto(
    val id: Int = 0,
    val name: String = "",
    val picture: String? = null,
    val gender: String? = null,
    val birthday: String? = null,
    val location: String? = null,
    @SerialName("joined_at") val joinedAt: String? = null,
    @SerialName("anime_statistics") val animeStatistics: MalAnimeStatisticsDto? = null,
)

@Serializable
data class MalAnimeStatisticsDto(
    @SerialName("num_items_watching") val numItemsWatching: Int = 0,
    @SerialName("num_items_completed") val numItemsCompleted: Int = 0,
    @SerialName("num_items_on_hold") val numItemsOnHold: Int = 0,
    @SerialName("num_items_dropped") val numItemsDropped: Int = 0,
    @SerialName("num_items_plan_to_watch") val numItemsPlanToWatch: Int = 0,
    @SerialName("num_items") val numItems: Int = 0,
    @SerialName("num_days_watched") val numDaysWatched: Double = 0.0,
    @SerialName("num_episodes") val numEpisodes: Int = 0,
    @SerialName("num_times_rewatched") val numTimesRewatched: Int = 0,
    @SerialName("mean_score") val meanScore: Double = 0.0,
)
