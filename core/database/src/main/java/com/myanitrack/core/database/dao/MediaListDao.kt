package com.myanitrack.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.myanitrack.core.database.entity.MediaListEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaListDao {

    @Query("SELECT * FROM media_list_entries WHERE mediaType = :mediaType")
    fun observeAll(mediaType: String): Flow<List<MediaListEntryEntity>>

    @Query("SELECT * FROM media_list_entries WHERE mediaType = :mediaType AND listStatus = :status")
    fun observeByStatus(mediaType: String, status: String): Flow<List<MediaListEntryEntity>>

    @Query("SELECT * FROM media_list_entries WHERE mediaType = :mediaType AND malId = :malId")
    fun observeEntry(mediaType: String, malId: Int): Flow<MediaListEntryEntity?>

    @Query("SELECT * FROM media_list_entries WHERE mediaType = :mediaType AND malId = :malId")
    suspend fun getEntry(mediaType: String, malId: Int): MediaListEntryEntity?

    @Query("SELECT COUNT(*) FROM media_list_entries WHERE mediaType = :mediaType")
    suspend fun count(mediaType: String): Int

    @Query("SELECT * FROM media_list_entries WHERE pendingSync = 1")
    suspend fun getPendingSync(): List<MediaListEntryEntity>

    @Upsert
    suspend fun upsertAll(entries: List<MediaListEntryEntity>)

    @Upsert
    suspend fun upsert(entry: MediaListEntryEntity)

    @Query("DELETE FROM media_list_entries WHERE mediaType = :mediaType AND malId = :malId")
    suspend fun delete(mediaType: String, malId: Int)

    @Query("DELETE FROM media_list_entries WHERE mediaType = :mediaType")
    suspend fun deleteAll(mediaType: String)

    @Query("DELETE FROM media_list_entries")
    suspend fun clear()

    /**
     * Tam senkronizasyon sonucu: sunucuda artik olmayan kayitlari da temizler.
     * Tek islem icinde yapilir; UI ara adimda bos liste gormez.
     */
    @Transaction
    suspend fun replaceAll(mediaType: String, entries: List<MediaListEntryEntity>) {
        deleteAll(mediaType)
        upsertAll(entries)
    }
}
