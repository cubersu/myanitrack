package com.myanitrack.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import java.time.Instant
import java.time.LocalDate

/**
 * Kullanicinin anime/manga listesindeki tek kaydin yerel kopyasi.
 *
 * Anahtar (mediaType, malId) ciftidir: ayni id hem anime hem manga tarafinda
 * bulunabilir. Liste ekrani her zaman bu tablodan okur; ag yalnizca tabloyu
 * tazeler (cache-then-network). Cevrimdisi goruntuleme bu sayede bedava gelir.
 */
@Entity(
    tableName = "media_list_entries",
    primaryKeys = ["mediaType", "malId"],
    indices = [
        Index(value = ["mediaType", "listStatus"]),
        Index(value = ["mediaType", "title"]),
    ],
)
data class MediaListEntryEntity(
    val mediaType: String,
    val malId: Int,

    // --- node ---
    val title: String,
    val englishTitle: String?,
    val japaneseTitle: String?,
    val pictureMedium: String?,
    val pictureLarge: String?,
    val subType: String,
    val airingStatus: String,
    val meanScore: Double?,
    val rank: Int?,
    val popularity: Int?,
    val numEpisodes: Int?,
    val numChapters: Int?,
    val numVolumes: Int?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val synopsis: String?,
    val genres: List<String>,
    val studios: List<String>,
    val nsfw: Boolean,

    // --- my_list_status ---
    val listStatus: String,
    val score: Int,
    val numEpisodesWatched: Int,
    val numChaptersRead: Int,
    val numVolumesRead: Int,
    val isRepeating: Boolean,
    val numTimesRepeated: Int,
    val repeatValue: Int,
    val priority: Int,
    val userStartDate: LocalDate?,
    val userFinishDate: LocalDate?,
    val tags: List<String>,
    val comments: String,
    val updatedAt: Instant?,

    /**
     * Yerelde degisip henuz MAL-e gonderilemeyen kayit. Cevrimdisi duzenlemede
     * true olur, senkronizasyon basarili olunca temizlenir.
     */
    val pendingSync: Boolean = false,
)
