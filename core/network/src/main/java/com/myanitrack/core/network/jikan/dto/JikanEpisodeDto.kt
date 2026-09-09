package com.myanitrack.core.network.jikan.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JikanEpisodeDto(
    @SerialName("mal_id") val number: Int = 0,
    val aired: String? = null,
)
