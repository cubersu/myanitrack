package com.myanitrack.core.network.jikan.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class JikanPersonDto(
    val name: String = "",
    val images: JikanImageSetDto? = null,
    val about: String? = null,
    @SerialName("name_kanji") val nameKanji: String? = null,
    @SerialName("given_name") val givenName: String? = null,
    @SerialName("family_name") val familyName: String? = null,
    val favorites: Int = 0,
)
