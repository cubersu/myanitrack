package com.myanitrack.core.domain.repository

import com.myanitrack.core.model.ListSortOption
import com.myanitrack.core.model.ListViewMode
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.SortDirection
import com.myanitrack.core.model.ThemeMode
import com.myanitrack.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/** Ayarlar ekraninin ve liste gorunumunun kalici tercihleri. */
interface UserPreferencesRepository {

    val preferences: Flow<UserPreferences>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setDynamicColor(enabled: Boolean)

    suspend fun setDefaultMediaType(type: MediaType)

    suspend fun setListViewMode(mode: ListViewMode)

    suspend fun setListSort(option: ListSortOption, direction: SortDirection)

    suspend fun setHideNsfw(hide: Boolean)

    suspend fun setAiringNotifications(enabled: Boolean)

    suspend fun setSyncOnlyOnWifi(enabled: Boolean)
}
