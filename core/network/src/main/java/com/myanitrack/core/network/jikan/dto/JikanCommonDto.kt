package com.myanitrack.core.network.jikan.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Jikan tekil kaynak sarmalayicisi: `{ "data": { ... } }`. */
@Serializable
data class JikanResponse<T>(
    val data: T? = null,
)

/** Jikan sayfali liste sarmalayicisi: `{ "pagination": {...}, "data": [...] }`. */
@Serializable
data class JikanPagedResponse<T>(
    val data: List<T> = emptyList(),
    val pagination: JikanPaginationDto = JikanPaginationDto(),
)

@Serializable
data class JikanPaginationDto(
    @SerialName("last_visible_page") val lastVisiblePage: Int? = null,
    @SerialName("has_next_page") val hasNextPage: Boolean = false,
    @SerialName("current_page") val currentPage: Int? = null,
    val items: JikanPaginationItemsDto? = null,
)

@Serializable
data class JikanPaginationItemsDto(
    val count: Int = 0,
    val total: Int = 0,
    @SerialName("per_page") val perPage: Int = 0,
)

@Serializable
data class JikanImageSetDto(
    val jpg: JikanImageDto? = null,
    val webp: JikanImageDto? = null,
) {
    /** En buyuk kullanilabilir gorsel; webp yoksa jpg-ye duser. */
    val large: String? get() = (webp ?: jpg)?.largeImageUrl ?: (webp ?: jpg)?.imageUrl
    val medium: String? get() = (webp ?: jpg)?.imageUrl
}

@Serializable
data class JikanImageDto(
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("small_image_url") val smallImageUrl: String? = null,
    @SerialName("large_image_url") val largeImageUrl: String? = null,
)

/** Tur, studyo, yazar, lisansci gibi baglantili varliklar. */
@Serializable
data class JikanNamedDto(
    @SerialName("mal_id") val malId: Int = 0,
    val type: String? = null,
    val name: String = "",
    val url: String? = null,
)

@Serializable
data class JikanDateRangeDto(
    val from: String? = null,
    val to: String? = null,
)

@Serializable
data class JikanBroadcastDto(
    val day: String? = null,
    val time: String? = null,
    val timezone: String? = null,
    val string: String? = null,
)

@Serializable
data class JikanTrailerDto(
    @SerialName("youtube_id") val youtubeId: String? = null,
    val url: String? = null,
    @SerialName("embed_url") val embedUrl: String? = null,
    val images: JikanImageDto? = null,
)

@Serializable
data class JikanThemeSongsDto(
    val openings: List<String> = emptyList(),
    val endings: List<String> = emptyList(),
)

/** Iliskili yapim grubu: `{ "relation": "Sequel", "entry": [...] }`. */
@Serializable
data class JikanRelationDto(
    val relation: String = "",
    val entry: List<JikanNamedDto> = emptyList(),
)

/** Oneri, karakter ve arama sonuclarindaki kisa yapim gosterimi. */
@Serializable
data class JikanEntryDto(
    @SerialName("mal_id") val malId: Int = 0,
    val url: String? = null,
    val images: JikanImageSetDto? = null,
    val title: String = "",
)
