package com.myanitrack.core.network.jikan.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Jikan `/anime/{id}/full` ve `/manga/{id}/full` govdesi.
 *
 * Anime ve manga alanlari tek DTO-da birlestirildi; karsi tarafta olmayan alanlar
 * null gelir (orn. manga icin `episodes`, anime icin `chapters`).
 */
@Serializable
data class JikanMediaDto(
    @SerialName("mal_id") val malId: Int = 0,
    val url: String? = null,
    val images: JikanImageSetDto? = null,
    val trailer: JikanTrailerDto? = null,
    val title: String = "",
    @SerialName("title_english") val titleEnglish: String? = null,
    @SerialName("title_japanese") val titleJapanese: String? = null,
    @SerialName("title_synonyms") val titleSynonyms: List<String> = emptyList(),
    val type: String? = null,
    val source: String? = null,
    val episodes: Int? = null,
    val chapters: Int? = null,
    val volumes: Int? = null,
    val status: String? = null,
    val airing: Boolean = false,
    val publishing: Boolean = false,
    val aired: JikanDateRangeDto? = null,
    val published: JikanDateRangeDto? = null,
    val duration: String? = null,
    val rating: String? = null,
    val score: Double? = null,
    @SerialName("scored_by") val scoredBy: Int? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    val members: Int? = null,
    val favorites: Int? = null,
    val synopsis: String? = null,
    val background: String? = null,
    val season: String? = null,
    val year: Int? = null,
    val broadcast: JikanBroadcastDto? = null,
    val producers: List<JikanNamedDto> = emptyList(),
    val studios: List<JikanNamedDto> = emptyList(),
    val authors: List<JikanNamedDto> = emptyList(),
    val serializations: List<JikanNamedDto> = emptyList(),
    val genres: List<JikanNamedDto> = emptyList(),
    @SerialName("explicit_genres") val explicitGenres: List<JikanNamedDto> = emptyList(),
    val themes: List<JikanNamedDto> = emptyList(),
    val demographics: List<JikanNamedDto> = emptyList(),
    val relations: List<JikanRelationDto> = emptyList(),
    val theme: JikanThemeSongsDto? = null,
)

@Serializable
data class JikanCharacterEntryDto(
    val character: JikanPersonRefDto = JikanPersonRefDto(),
    val role: String? = null,
    val favorites: Int = 0,
    @SerialName("voice_actors") val voiceActors: List<JikanVoiceActorDto> = emptyList(),
)

@Serializable
data class JikanPersonRefDto(
    @SerialName("mal_id") val malId: Int = 0,
    val url: String? = null,
    val images: JikanImageSetDto? = null,
    val name: String = "",
)

@Serializable
data class JikanVoiceActorDto(
    val person: JikanPersonRefDto = JikanPersonRefDto(),
    val language: String? = null,
)

@Serializable
data class JikanStaffEntryDto(
    val person: JikanPersonRefDto = JikanPersonRefDto(),
    val positions: List<String> = emptyList(),
)

@Serializable
data class JikanReviewDto(
    @SerialName("mal_id") val malId: Int = 0,
    val url: String? = null,
    val type: String? = null,
    val reactions: JikanReactionsDto? = null,
    val date: String? = null,
    val review: String = "",
    val score: Int? = null,
    val tags: List<String> = emptyList(),
    @SerialName("is_spoiler") val isSpoiler: Boolean = false,
    @SerialName("is_preliminary") val isPreliminary: Boolean = false,
    @SerialName("episodes_watched") val episodesWatched: Int? = null,
    val user: JikanUserMetaDto? = null,
)

@Serializable
data class JikanReactionsDto(
    val overall: Int = 0,
    val nice: Int = 0,
    @SerialName("love_it") val loveIt: Int = 0,
    val funny: Int = 0,
    val confusing: Int = 0,
    val informative: Int = 0,
    @SerialName("well_written") val wellWritten: Int = 0,
    val creative: Int = 0,
)

@Serializable
data class JikanUserMetaDto(
    val username: String = "",
    val url: String? = null,
    val images: JikanImageSetDto? = null,
)

@Serializable
data class JikanRecommendationDto(
    val entry: JikanEntryDto = JikanEntryDto(),
    val url: String? = null,
    val votes: Int = 0,
)

@Serializable
data class JikanVideosDto(
    val promo: List<JikanPromoDto> = emptyList(),
)

@Serializable
data class JikanPromoDto(
    val title: String? = null,
    val trailer: JikanTrailerDto? = null,
)

@Serializable
data class JikanGenreDto(
    @SerialName("mal_id") val malId: Int = 0,
    val name: String = "",
    val url: String? = null,
    val count: Int = 0,
)

/**
 * Genel oneri akisi (`/recommendations/{type}`): her kayit birbirine onerilen
 * IKI yapim icerir. `mal_id` burada "5114-9253" gibi tireli bir dizedir.
 */
@Serializable
data class JikanRecommendationEntriesDto(
    @SerialName("mal_id") val malId: String = "",
    val entry: List<JikanEntryDto> = emptyList(),
    val content: String = "",
)
