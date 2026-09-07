package com.myanitrack.core.datastore.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.myanitrack.core.model.ListSortOption
import com.myanitrack.core.model.ListViewMode
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.SortDirection
import com.myanitrack.core.model.ThemeMode
import com.myanitrack.core.model.UserPreferences
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Singleton
class UserPreferencesStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    val preferences: Flow<UserPreferences> = dataStore.data
        .catch { cause -> if (cause is IOException) emit(emptyPreferences()) else throw cause }
        .map { it.toUserPreferences() }

    suspend fun setThemeMode(mode: ThemeMode) = put(Keys.THEME_MODE, mode.name)

    suspend fun setDynamicColor(enabled: Boolean) = put(Keys.DYNAMIC_COLOR, enabled)

    suspend fun setDefaultMediaType(type: MediaType) = put(Keys.DEFAULT_MEDIA_TYPE, type.name)

    suspend fun setListViewMode(mode: ListViewMode) = put(Keys.LIST_VIEW_MODE, mode.name)

    suspend fun setListSort(option: ListSortOption, direction: SortDirection) {
        dataStore.edit {
            it[Keys.LIST_SORT_OPTION] = option.name
            it[Keys.LIST_SORT_DIRECTION] = direction.name
        }
    }

    suspend fun setHideNsfw(hide: Boolean) = put(Keys.HIDE_NSFW, hide)

    suspend fun setAiringNotifications(enabled: Boolean) = put(Keys.AIRING_NOTIFICATIONS, enabled)

    suspend fun setSyncOnlyOnWifi(enabled: Boolean) = put(Keys.SYNC_ONLY_WIFI, enabled)

    suspend fun setLastSync(epochSeconds: Long) = put(Keys.LAST_SYNC, epochSeconds)

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }

    private fun Preferences.toUserPreferences() = UserPreferences(
        themeMode = enumOrDefault(this[Keys.THEME_MODE], ThemeMode.SYSTEM),
        useDynamicColor = this[Keys.DYNAMIC_COLOR] ?: true,
        defaultMediaType = enumOrDefault(this[Keys.DEFAULT_MEDIA_TYPE], MediaType.ANIME),
        listViewMode = enumOrDefault(this[Keys.LIST_VIEW_MODE], ListViewMode.DETAILED_GRID),
        listSortOption = enumOrDefault(this[Keys.LIST_SORT_OPTION], ListSortOption.TITLE),
        listSortDirection = enumOrDefault(this[Keys.LIST_SORT_DIRECTION], SortDirection.ASCENDING),
        hideNsfw = this[Keys.HIDE_NSFW] ?: true,
        airingNotificationsEnabled = this[Keys.AIRING_NOTIFICATIONS] ?: true,
        syncOnlyOnWifi = this[Keys.SYNC_ONLY_WIFI] ?: false,
        lastSyncEpochSeconds = this[Keys.LAST_SYNC] ?: 0L,
    )

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, fallback: T): T =
        raw?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: fallback

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_MEDIA_TYPE = stringPreferencesKey("default_media_type")
        val LIST_VIEW_MODE = stringPreferencesKey("list_view_mode")
        val LIST_SORT_OPTION = stringPreferencesKey("list_sort_option")
        val LIST_SORT_DIRECTION = stringPreferencesKey("list_sort_direction")
        val HIDE_NSFW = booleanPreferencesKey("hide_nsfw")
        val AIRING_NOTIFICATIONS = booleanPreferencesKey("airing_notifications")
        val SYNC_ONLY_WIFI = booleanPreferencesKey("sync_only_wifi")
        val LAST_SYNC = longPreferencesKey("last_sync")
    }
}
