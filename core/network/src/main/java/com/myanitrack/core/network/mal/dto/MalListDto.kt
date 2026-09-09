package com.myanitrack.core.network.mal.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MalPagedResponse<T>(
    val data: List<T> = emptyList(),
    val paging: MalPagingDto = MalPagingDto(),
)

@Serializable
data class MalPagingDto(
    val next: String? = null,
    val previous: String? = null,
)

@Serializable
data class MalListEntryDto(
    val node: MalNodeDto,
    @SerialName("list_status") val listStatus: MalListStatusDto? = null,
)

@Serializable
data class MalPictureDto(
    val medium: String? = null,
    val large: String? = null,
)

@Serializable
data class MalNamedDto(
    val id: Int = 0,
    val name: String = "",
)

@Serializable
data class MalAlternativeTitlesDto(
    val synonyms: List<String> = emptyList(),
    val en: String? = null,
    val ja: String? = null,
)

/**
 * MAL API v2 anime/manga dugumu. Anime ve manga alanlari tek DTO-da toplandi;
 * karsi tarafta olmayan alanlar null gelir.
 */
@Serializable
data class MalNodeDto(
    val id: Int,
    val title: String = "",
    @SerialName("main_picture") val mainPicture: MalPictureDto? = null,
    @SerialName("alternative_titles") val alternativeTitles: MalAlternativeTitlesDto? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val synopsis: String? = null,
    val mean: Double? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    val nsfw: String? = null,
    val genres: List<MalNamedDto> = emptyList(),
    @SerialName("media_type") val mediaType: String? = null,
    val status: String? = null,
    @SerialName("num_episodes") val numEpisodes: Int? = null,
    @SerialName("num_chapters") val numChapters: Int? = null,
    @SerialName("num_volumes") val numVolumes: Int? = null,
    val studios: List<MalNamedDto> = emptyList(),
    val broadcast: MalBroadcastDto? = null,
)

@Serializable
data class MalBroadcastDto(
    @SerialName("day_of_the_week") val dayOfWeek: String? = null,
    @SerialName("start_time") val startTime: String? = null,
)

/** `my_list_status` alani. Anime ve manga varyantlarinin birlesimi. */
@Serializable
data class MalListStatusDto(
    val status: String? = null,
    val score: Int = 0,
    @SerialName("num_episodes_watched") val numEpisodesWatched: Int = 0,
    @SerialName("num_chapters_read") val numChaptersRead: Int = 0,
    @SerialName("num_volumes_read") val numVolumesRead: Int = 0,
    @SerialName("is_rewatching") val isRewatching: Boolean = false,
    @SerialName("is_rereading") val isRereading: Boolean = false,
    @SerialName("num_times_rewatched") val numTimesRewatched: Int = 0,
    @SerialName("num_times_reread") val numTimesReread: Int = 0,
    @SerialName("rewatch_value") val rewatchValue: Int = 0,
    @SerialName("reread_value") val rereadValue: Int = 0,
    val priority: Int = 0,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("finish_date") val finishDate: String? = null,
    val tags: List<String> = emptyList(),
    val comments: String = "",
    @SerialName("updated_at") val updatedAt: String? = null,
)
