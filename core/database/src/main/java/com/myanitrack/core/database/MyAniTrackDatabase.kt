package com.myanitrack.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.myanitrack.core.database.converter.RoomConverters
import com.myanitrack.core.database.dao.MediaListDao
import com.myanitrack.core.database.dao.RemoteCacheDao
import com.myanitrack.core.database.entity.MediaListEntryEntity
import com.myanitrack.core.database.entity.RemoteCacheEntity

@Database(
    entities = [MediaListEntryEntity::class, RemoteCacheEntity::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class MyAniTrackDatabase : RoomDatabase() {

    abstract fun mediaListDao(): MediaListDao

    abstract fun remoteCacheDao(): RemoteCacheDao

    companion object {
        const val NAME = "myanitrack.db"
    }
}
