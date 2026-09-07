package com.myanitrack.core.data.prefs

import com.myanitrack.core.datastore.prefs.UserPreferencesStore
import com.myanitrack.core.domain.repository.UserPreferencesRepository
import com.myanitrack.core.model.ListSortOption
import com.myanitrack.core.model.ListViewMode
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.SortDirection
import com.myanitrack.core.model.ThemeMode
import com.myanitrack.core.model.UserPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val store: UserPreferencesStore,
) : UserPreferencesRepository {

    override val preferences: Flow<UserPreferences> = store.preferences

    override suspend fun setThemeMode(mode: ThemeMode) = store.setThemeMode(mode)

    override suspend fun setDynamicColor(enabled: Boolean) = store.setDynamicColor(enabled)

    override suspend fun setDefaultMediaType(type: MediaType) = store.setDefaultMediaType(type)

    override suspend fun setListViewMode(mode: ListViewMode) = store.setListViewMode(mode)

    override suspend fun setListSort(option: ListSortOption, direction: SortDirection) =
        store.setListSort(option, direction)

    override suspend fun setHideNsfw(hide: Boolean) = store.setHideNsfw(hide)

    override suspend fun setAiringNotifications(enabled: Boolean) =
        store.setAiringNotifications(enabled)

    override suspend fun setSyncOnlyOnWifi(enabled: Boolean) = store.setSyncOnlyOnWifi(enabled)
}
