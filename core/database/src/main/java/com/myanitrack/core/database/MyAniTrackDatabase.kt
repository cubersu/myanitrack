package com.myanitrack.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.myanitrack.core.database.converter.RoomConverters
import com.myanitrack.core.database.dao.MediaListDao
import com.myanitrack.core.database.entity.MediaListEntryEntity

@Database(
    entities = [MediaListEntryEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class MyAniTrackDatabase : RoomDatabase() {

    abstract fun mediaListDao(): MediaListDao

    companion object {
        const val NAME = "myanitrack.db"
    }
}
