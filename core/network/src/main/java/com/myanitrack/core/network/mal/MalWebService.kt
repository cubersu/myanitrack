package com.myanitrack.core.network.mal

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Public pages only. This service must never receive MAL OAuth credentials. */
interface MalWebService {
    @GET("character/{id}")
    suspend fun getCharacter(@Path("id") id: Int): String

    @GET("people/{id}")
    suspend fun getPerson(@Path("id") id: Int): String

    @GET("anime/{id}/_/episode")
    suspend fun getEpisodes(@Path("id") animeId: Int, @Query("offset") offset: Int = 0): String
}
