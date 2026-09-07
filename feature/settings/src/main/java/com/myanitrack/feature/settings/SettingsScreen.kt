package com.myanitrack.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myanitrack.core.model.ListViewMode
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.ThemeMode

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onThemeModeChange = viewModel::setThemeMode,
        onDynamicColorChange = viewModel::setDynamicColor,
        onDefaultMediaTypeChange = viewModel::setDefaultMediaType,
        onListViewModeChange = viewModel::setListViewMode,
        onHideNsfwChange = viewModel::setHideNsfw,
        onAiringNotificationsChange = viewModel::setAiringNotifications,
        onLogout = viewModel::logout,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onDefaultMediaTypeChange: (MediaType) -> Unit,
    onListViewModeChange: (ListViewMode) -> Unit,
    onHideNsfwChange: (Boolean) -> Unit,
    onAiringNotificationsChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmLogout by remember { mutableStateOf(false) }
    val prefs = uiState.preferences

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.settings_section_appearance))
            ThemeMode.entries.forEach { mode ->
                RadioRow(
                    label = themeLabel(mode),
                    selected = prefs.themeMode == mode,
                    onClick = { onThemeModeChange(mode) },
                )
            }
            SwitchRow(
                title = stringResource(R.string.settings_dynamic_color),
                subtitle = stringResource(R.string.settings_dynamic_color_summary),
                checked = prefs.useDynamicColor,
                onCheckedChange = onDynamicColorChange,
            )

            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_section_list))
            Text(
                text = stringResource(R.string.settings_default_tab),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            MediaType.entries.forEach { type ->
                RadioRow(
                    label = if (type.isAnime) {
                        stringResource(R.string.settings_media_anime)
                    } else {
                        stringResource(R.string.settings_media_manga)
                    },
                    selected = prefs.defaultMediaType == type,
                    onClick = { onDefaultMediaTypeChange(type) },
                )
            }
            Text(
                text = stringResource(R.string.settings_default_view),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            ListViewMode.entries.forEach { mode ->
                RadioRow(
                    label = viewModeLabel(mode),
                    selected = prefs.listViewMode == mode,
                    onClick = { onListViewModeChange(mode) },
                )
            }
            SwitchRow(
                title = stringResource(R.string.settings_hide_nsfw),
                subtitle = stringResource(R.string.settings_hide_nsfw_summary),
                checked = prefs.hideNsfw,
                onCheckedChange = onHideNsfwChange,
            )

            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_section_notifications))
            SwitchRow(
                title = stringResource(R.string.settings_airing_notifications),
                subtitle = stringResource(R.string.settings_airing_notifications_summary),
                checked = prefs.airingNotificationsEnabled,
                onCheckedChange = onAiringNotificationsChange,
            )

            HorizontalDivider()
            SectionHeader(stringResource(R.string.settings_section_account))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { confirmLogout = true }
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_logout),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                uiState.userName?.let { name ->
                    Text(
                        text = stringResource(R.string.settings_signed_in_as, name),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.settings_logout)) },
            text = { Text(stringResource(R.string.settings_logout_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLogout = false
                        onLogout()
                    },
                ) {
                    Text(stringResource(R.string.settings_logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) {
                    Text(stringResource(com.myanitrack.core.ui.R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun themeLabel(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    },
)

@Composable
private fun viewModeLabel(mode: ListViewMode): String = stringResource(
    when (mode) {
        ListViewMode.GRID -> R.string.settings_view_grid
        ListViewMode.COMPACT -> R.string.settings_view_compact
        ListViewMode.DETAILED_GRID -> R.string.settings_view_detailed
    },
)
