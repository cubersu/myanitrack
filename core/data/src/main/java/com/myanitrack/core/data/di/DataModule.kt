package com.myanitrack.core.data.di

import com.myanitrack.core.data.auth.AuthRepositoryImpl
import com.myanitrack.core.data.list.MediaListRepositoryImpl
import com.myanitrack.core.data.prefs.UserPreferencesRepositoryImpl
import com.myanitrack.core.data.user.UserRepositoryImpl
import com.myanitrack.core.domain.repository.AuthRepository
import com.myanitrack.core.domain.repository.MediaListRepository
import com.myanitrack.core.domain.repository.UserPreferencesRepository
import com.myanitrack.core.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindsAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindsMediaListRepository(impl: MediaListRepositoryImpl): MediaListRepository

    @Binds
    @Singleton
    abstract fun bindsUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindsUserPreferencesRepository(
        impl: UserPreferencesRepositoryImpl,
    ): UserPreferencesRepository
}
