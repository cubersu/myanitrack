package com.myanitrack.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.myanitrack.core.database.entity.RemoteCacheEntity

@Dao
interface RemoteCacheDao {

    @Query("SELECT * FROM remote_cache WHERE cacheKey = :key")
    suspend fun get(key: String): RemoteCacheEntity?

    @Upsert
    suspend fun put(entry: RemoteCacheEntity)

    @Query("DELETE FROM remote_cache WHERE cacheKey = :key")
    suspend fun delete(key: String)

    /** Ayarlardaki "onbellegi temizle" ve suresi gecmis kayitlarin toplanmasi icin. */
    @Query("DELETE FROM remote_cache WHERE fetchedAtEpochSeconds < :threshold")
    suspend fun deleteOlderThan(threshold: Long)

    @Query("DELETE FROM remote_cache")
    suspend fun clear()

    @Query("SELECT COALESCE(SUM(LENGTH(payload)), 0) FROM remote_cache")
    suspend fun approximateSizeBytes(): Long
}
