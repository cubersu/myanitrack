package com.myanitrack.core.database.di

import android.content.Context
import androidx.room.Room
import com.myanitrack.core.database.MyAniTrackDatabase
import com.myanitrack.core.database.dao.MediaListDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providesDatabase(@ApplicationContext context: Context): MyAniTrackDatabase =
        Room.databaseBuilder(context, MyAniTrackDatabase::class.java, MyAniTrackDatabase.NAME)
            // Bu tablo yalnizca onbellek. Sema degisince veri kaybi sorun degil;
            // bir sonraki senkronizasyonda MAL-den yeniden dolar.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun providesMediaListDao(database: MyAniTrackDatabase): MediaListDao = database.mediaListDao()
}
