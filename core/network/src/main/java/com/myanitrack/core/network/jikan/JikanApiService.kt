package com.myanitrack.core.network.jikan

import com.myanitrack.core.network.jikan.dto.JikanCharacterEntryDto
import com.myanitrack.core.network.jikan.dto.JikanGenreDto
import com.myanitrack.core.network.jikan.dto.JikanMediaDto
import com.myanitrack.core.network.jikan.dto.JikanPagedResponse
import com.myanitrack.core.network.jikan.dto.JikanRecommendationDto
import com.myanitrack.core.network.jikan.dto.JikanRecommendationEntriesDto
import com.myanitrack.core.network.jikan.dto.JikanResponse
import com.myanitrack.core.network.jikan.dto.JikanReviewDto
import com.myanitrack.core.network.jikan.dto.JikanFriendDto
import com.myanitrack.core.network.jikan.dto.JikanHistoryDto
import com.myanitrack.core.network.jikan.dto.JikanStaffEntryDto
import com.myanitrack.core.network.jikan.dto.JikanUserProfileDto
import com.myanitrack.core.network.jikan.dto.JikanVideosDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Jikan API v4 - topluluk tarafindan isletilen, anahtarsiz, SALT OKUNUR MAL kaynagi.
 *
 * Uygulamadaki hicbir yazma islemi buradan gecmez; Jikan yalnizca MAL API v2-nin
 * vermedigi zenginlestirme verisini saglar (karakter, review, oneri, top listeler,
 * sezonluk, tur/studyo gezinmesi).
 *
 * Istek limiti 3/sn ve 60/dk-dir; [com.myanitrack.core.network.jikan.JikanRateLimitInterceptor]
 * bunu istemci tarafinda zorlar, [JikanRetryInterceptor] ise 429/5xx durumlarinda
 * ustel bekleyerek yeniden dener.
 */
interface JikanApiService {

    @GET("anime/{id}/full")
    suspend fun getAnimeFull(@Path("id") id: Int): JikanResponse<JikanMediaDto>

    @GET("manga/{id}/full")
    suspend fun getMangaFull(@Path("id") id: Int): JikanResponse<JikanMediaDto>

    @GET("anime/{id}/characters")
    suspend fun getAnimeCharacters(@Path("id") id: Int): JikanResponse<List<JikanCharacterEntryDto>>

    @GET("manga/{id}/characters")
    suspend fun getMangaCharacters(@Path("id") id: Int): JikanResponse<List<JikanCharacterEntryDto>>

    @GET("anime/{id}/staff")
    suspend fun getAnimeStaff(@Path("id") id: Int): JikanResponse<List<JikanStaffEntryDto>>

    @GET("anime/{id}/reviews")
    suspend fun getAnimeReviews(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("preliminary") preliminary: Boolean = true,
        @Query("spoilers") spoilers: Boolean = true,
    ): JikanPagedResponse<JikanReviewDto>

    @GET("manga/{id}/reviews")
    suspend fun getMangaReviews(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("preliminary") preliminary: Boolean = true,
        @Query("spoilers") spoilers: Boolean = true,
    ): JikanPagedResponse<JikanReviewDto>

    @GET("anime/{id}/recommendations")
    suspend fun getAnimeRecommendations(
        @Path("id") id: Int,
    ): JikanResponse<List<JikanRecommendationDto>>

    @GET("manga/{id}/recommendations")
    suspend fun getMangaRecommendations(
        @Path("id") id: Int,
    ): JikanResponse<List<JikanRecommendationDto>>

    @GET("anime/{id}/videos")
    suspend fun getAnimeVideos(@Path("id") id: Int): JikanResponse<JikanVideosDto>

    /**
     * Haftalik yayin takvimi. `filter` gun adi ("monday" ... "sunday").
     * Yanit, `broadcast` alani dolu anime kayitlaridir.
     */
    @GET("schedules")
    suspend fun getSchedule(
        @Query("filter") day: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = PAGE_SIZE,
        @Query("sfw") safeForWork: Boolean = true,
        @Query("kids") kids: Boolean = false,
    ): JikanPagedResponse<JikanMediaDto>

    // --- Kesfet ---

    @GET("top/anime")
    suspend fun getTopAnime(
        @Query("filter") filter: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = PAGE_SIZE,
        @Query("sfw") safeForWork: Boolean = true,
    ): JikanPagedResponse<JikanMediaDto>

    @GET("top/manga")
    suspend fun getTopManga(
        @Query("filter") filter: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = PAGE_SIZE,
        @Query("sfw") safeForWork: Boolean = true,
    ): JikanPagedResponse<JikanMediaDto>

    @GET("seasons/{year}/{season}")
    suspend fun getSeason(
        @Path("year") year: Int,
        @Path("season") season: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = PAGE_SIZE,
        @Query("sfw") safeForWork: Boolean = true,
    ): JikanPagedResponse<JikanMediaDto>

    @GET("anime")
    suspend fun searchAnime(
        @Query("q") query: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = PAGE_SIZE,
        @Query("genres") genres: Int? = null,
        @Query("producers") producers: Int? = null,
        @Query("order_by") orderBy: String? = null,
        @Query("sort") sort: String? = null,
        @Query("sfw") safeForWork: Boolean = true,
    ): JikanPagedResponse<JikanMediaDto>

    @GET("manga")
    suspend fun searchManga(
        @Query("q") query: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = PAGE_SIZE,
        @Query("genres") genres: Int? = null,
        @Query("order_by") orderBy: String? = null,
        @Query("sort") sort: String? = null,
        @Query("sfw") safeForWork: Boolean = true,
    ): JikanPagedResponse<JikanMediaDto>

    @GET("genres/anime")
    suspend fun getAnimeGenres(): JikanResponse<List<JikanGenreDto>>

    @GET("genres/manga")
    suspend fun getMangaGenres(): JikanResponse<List<JikanGenreDto>>

    @GET("recommendations/anime")
    suspend fun getGlobalAnimeRecommendations(
        @Query("page") page: Int = 1,
    ): JikanPagedResponse<JikanRecommendationEntriesDto>

    @GET("recommendations/manga")
    suspend fun getGlobalMangaRecommendations(
        @Query("page") page: Int = 1,
    ): JikanPagedResponse<JikanRecommendationEntriesDto>

    // --- Profil ---

    /**
     * Kullanici profili + istatistikler.
     *
     * MAL API v2-nin `/users/{user_name}` ucu yalnizca `@me` kabul ettigi icin
     * baskasinin profiline bakmanin tek yolu bu.
     */
    @GET("users/{username}/full")
    suspend fun getUserProfile(@Path("username") username: String): JikanResponse<JikanUserProfileDto>

    @GET("users/{username}/history")
    suspend fun getUserHistory(
        @Path("username") username: String,
        @Query("type") type: String? = null,
    ): JikanResponse<List<JikanHistoryDto>>

    @GET("users/{username}/friends")
    suspend fun getUserFriends(
        @Path("username") username: String,
        @Query("page") page: Int = 1,
    ): JikanPagedResponse<JikanFriendDto>

    companion object {
        /** Jikan sayfa basina en fazla 25 kayit doner. */
        const val PAGE_SIZE = 25
    }
}
