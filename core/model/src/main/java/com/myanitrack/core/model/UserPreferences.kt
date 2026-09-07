package com.myanitrack.core.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Ayarlar ekranindan yonetilen, cihazda kalici kullanici tercihleri. */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val defaultMediaType: MediaType = MediaType.ANIME,
    val listViewMode: ListViewMode = ListViewMode.DETAILED_GRID,
    val listSortOption: ListSortOption = ListSortOption.TITLE,
    val listSortDirection: SortDirection = SortDirection.ASCENDING,
    val hideNsfw: Boolean = true,
    val airingNotificationsEnabled: Boolean = true,
    val syncOnlyOnWifi: Boolean = false,
    val lastSyncEpochSeconds: Long = 0L,
)
