package com.myanitrack.core.network.mal

import com.myanitrack.core.network.mal.dto.MalListEntryDto
import com.myanitrack.core.network.mal.dto.MalListStatusDto
import com.myanitrack.core.network.mal.dto.MalNodeDto
import com.myanitrack.core.network.mal.dto.MalPagedResponse
import com.myanitrack.core.network.mal.dto.MalUserDto
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * MAL API v2 - resmi endpoint-ler.
 *
 * Uygulamadaki TUM yazma islemleri (liste ekleme/guncelleme/silme) yalnizca
 * buradan gecer; Jikan ve RSS kaynaklari salt-okunurdur.
 */
interface MalApiService {

    @GET("users/@me")
    suspend fun getMyUser(
        @Query("fields") fields: String = MalFields.USER,
    ): MalUserDto

    @GET("users/{userName}")
    suspend fun getUser(
        @Path("userName") userName: String,
        @Query("fields") fields: String = MalFields.USER,
    ): MalUserDto

    @GET("users/@me/animelist")
    suspend fun getMyAnimeList(
        @Query("fields") fields: String = MalFields.ANIME_LIST,
        @Query("limit") limit: Int = DEFAULT_PAGE_SIZE,
        @Query("offset") offset: Int = 0,
        @Query("nsfw") nsfw: Boolean = true,
        @Query("status") status: String? = null,
        @Query("sort") sort: String? = null,
    ): MalPagedResponse<MalListEntryDto>

    @GET("users/@me/mangalist")
    suspend fun getMyMangaList(
        @Query("fields") fields: String = MalFields.MANGA_LIST,
        @Query("limit") limit: Int = DEFAULT_PAGE_SIZE,
        @Query("offset") offset: Int = 0,
        @Query("nsfw") nsfw: Boolean = true,
        @Query("status") status: String? = null,
        @Query("sort") sort: String? = null,
    ): MalPagedResponse<MalListEntryDto>

    /** `paging.next` mutlak URL doner; sayfalamayi bu sekilde takip ediyoruz. */
    @GET
    suspend fun getListPage(@Url url: String): MalPagedResponse<MalListEntryDto>

    @GET("anime/ranking")
    suspend fun getAnimeRanking(
        @Query("ranking_type") rankingType: String = "all",
        @Query("limit") limit: Int = DEFAULT_PAGE_SIZE,
        @Query("offset") offset: Int = 0,
        @Query("fields") fields: String = MalFields.ANIME_LIST,
    ): MalPagedResponse<MalListEntryDto>

    @GET("manga/ranking")
    suspend fun getMangaRanking(
        @Query("ranking_type") rankingType: String = "all",
        @Query("limit") limit: Int = DEFAULT_PAGE_SIZE,
        @Query("offset") offset: Int = 0,
        @Query("fields") fields: String = MalFields.MANGA_LIST,
    ): MalPagedResponse<MalListEntryDto>

    @GET("anime/season/{year}/{season}")
    suspend fun getSeasonAnime(
        @Path("year") year: Int,
        @Path("season") season: String,
        @Query("limit") limit: Int = SEARCH_PAGE_SIZE,
        @Query("offset") offset: Int = 0,
        @Query("nsfw") nsfw: Boolean = false,
        @Query("sort") sort: String = "anime_score",
        @Query("fields") fields: String = MalFields.ANIME_LIST,
    ): MalPagedResponse<MalListEntryDto>

    @GET("anime/{id}")
    suspend fun getAnime(
        @Path("id") id: Int,
        @Query("fields") fields: String = MalFields.ANIME_LIST,
    ): MalNodeDto

    @GET("manga/{id}")
    suspend fun getManga(
        @Path("id") id: Int,
        @Query("fields") fields: String = MalFields.MANGA_LIST,
    ): MalNodeDto

    @GET("anime")
    suspend fun searchAnime(
        @Query("q") query: String,
        @Query("fields") fields: String = MalFields.ANIME_LIST,
        @Query("limit") limit: Int = SEARCH_PAGE_SIZE,
        @Query("offset") offset: Int = 0,
        @Query("nsfw") nsfw: Boolean = true,
    ): MalPagedResponse<MalListEntryDto>

    @GET("manga")
    suspend fun searchManga(
        @Query("q") query: String,
        @Query("fields") fields: String = MalFields.MANGA_LIST,
        @Query("limit") limit: Int = SEARCH_PAGE_SIZE,
        @Query("offset") offset: Int = 0,
        @Query("nsfw") nsfw: Boolean = true,
    ): MalPagedResponse<MalListEntryDto>

    @FormUrlEncoded
    @PATCH("anime/{id}/my_list_status")
    suspend fun updateAnimeListStatus(
        @Path("id") id: Int,
        @Field("status") status: String? = null,
        @Field("score") score: Int? = null,
        @Field("num_watched_episodes") numWatchedEpisodes: Int? = null,
        @Field("is_rewatching") isRewatching: Boolean? = null,
        @Field("num_times_rewatched") numTimesRewatched: Int? = null,
        @Field("rewatch_value") rewatchValue: Int? = null,
        @Field("priority") priority: Int? = null,
        @Field("start_date") startDate: String? = null,
        @Field("finish_date") finishDate: String? = null,
        @Field("tags") tags: String? = null,
        @Field("comments") comments: String? = null,
    ): MalListStatusDto

    @FormUrlEncoded
    @PATCH("manga/{id}/my_list_status")
    suspend fun updateMangaListStatus(
        @Path("id") id: Int,
        @Field("status") status: String? = null,
        @Field("score") score: Int? = null,
        @Field("num_chapters_read") numChaptersRead: Int? = null,
        @Field("num_volumes_read") numVolumesRead: Int? = null,
        @Field("is_rereading") isRereading: Boolean? = null,
        @Field("num_times_reread") numTimesReread: Int? = null,
        @Field("reread_value") rereadValue: Int? = null,
        @Field("priority") priority: Int? = null,
        @Field("start_date") startDate: String? = null,
        @Field("finish_date") finishDate: String? = null,
        @Field("tags") tags: String? = null,
        @Field("comments") comments: String? = null,
    ): MalListStatusDto

    @DELETE("anime/{id}/my_list_status")
    suspend fun deleteAnimeListStatus(@Path("id") id: Int)

    @DELETE("manga/{id}/my_list_status")
    suspend fun deleteMangaListStatus(@Path("id") id: Int)

    companion object {
        /** MAL liste endpoint-lerinde ust sinir 1000. */
        const val DEFAULT_PAGE_SIZE = 1000
        const val SEARCH_PAGE_SIZE = 50
    }
}
