package com.myanitrack.feature.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myanitrack.core.designsystem.component.LoadingState
import com.myanitrack.core.designsystem.component.MediaCover
import com.myanitrack.core.designsystem.component.MessageState
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.ScheduleEntry
import com.myanitrack.core.ui.toUserMessage
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

private const val COVER_ASPECT_RATIO = 0.7f

/** Geri sayimin canli kalmasi icin yenileme araligi. */
private const val COUNTDOWN_TICK_MS = 60_000L

@Composable
fun CalendarRoute(
    onOpenMedia: (MediaType, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CalendarScreen(
        uiState = uiState,
        onDaySelect = viewModel::selectDay,
        onToggleOnlyMyList = viewModel::toggleOnlyMyList,
        onRefresh = viewModel::refresh,
        onOpenMedia = onOpenMedia,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarScreen(
    uiState: CalendarUiState,
    onDaySelect: (DayOfWeek) -> Unit,
    onToggleOnlyMyList: () -> Unit,
    onRefresh: () -> Unit,
    onOpenMedia: (MediaType, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Dakikada bir tetiklenen sayac; geri sayim metinlerini tazeler.
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(COUNTDOWN_TICK_MS)
            now = Instant.now()
        }
    }

    val days = DayOfWeek.entries
    val selectedIndex = days.indexOf(uiState.selectedDay).coerceAtLeast(0)

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.calendar_title)) },
                    actions = {
                        FilterChip(
                            selected = uiState.onlyMyList,
                            onClick = onToggleOnlyMyList,
                            label = { Text(stringResource(R.string.calendar_only_my_list)) },
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    },
                )
                PrimaryScrollableTabRow(selectedTabIndex = selectedIndex, edgePadding = 8.dp) {
                    days.forEach { day ->
                        Tab(
                            selected = day == uiState.selectedDay,
                            onClick = { onDaySelect(day) },
                            text = { Text(day.shortLabel()) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                uiState.isLoading -> LoadingState()

                uiState.isEmpty -> MessageState(
                    title = stringResource(
                        if (uiState.onlyMyList) {
                            R.string.calendar_empty_my_list_title
                        } else {
                            R.string.calendar_empty_title
                        },
                    ),
                    description = uiState.error?.toUserMessage()
                        ?: stringResource(R.string.calendar_empty_description),
                    icon = Icons.Outlined.EventBusy,
                    actionLabel = stringResource(com.myanitrack.core.ui.R.string.action_refresh),
                    onAction = onRefresh,
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(uiState.visibleEntries, key = { it.id }) { entry ->
                        ScheduleRow(
                            entry = entry,
                            now = now,
                            onClick = { onOpenMedia(entry.node.mediaType, entry.node.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleRow(
    entry: ScheduleEntry,
    now: Instant,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MediaCover(
            imageUrl = entry.node.picture.medium ?: entry.node.picture.large,
            contentDescription = entry.node.title,
            fallbackText = entry.node.title,
            cornerRadius = 6,
            modifier = Modifier
                .width(52.dp)
                .aspectRatio(COVER_ASPECT_RATIO),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = entry.node.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.localAiringLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (entry.isInMyList) {
                Text(
                    text = stringResource(R.string.calendar_in_my_list),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text = entry.countdownLabel(now),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Yayin saatini kullanicinin kendi saat diliminde gosterir. */
@Composable
private fun ScheduleEntry.localAiringLabel(): String {
    val airing = nextAiringAt ?: return broadcast.rawText
        ?: stringResource(R.string.calendar_time_unknown)
    val local = airing.atZone(ZoneId.systemDefault())
    return local.format(DateTimeFormatter.ofPattern("EEE HH:mm", Locale.getDefault()))
}

@Composable
private fun ScheduleEntry.countdownLabel(now: Instant): String {
    val remaining = timeUntilAiring(now) ?: return "-"
    return when {
        remaining.toHours() >= 24 ->
            stringResource(R.string.calendar_in_days, remaining.toDays().toInt())

        remaining.toMinutes() >= 60 ->
            stringResource(R.string.calendar_in_hours, remaining.toHours().toInt())

        remaining > Duration.ZERO ->
            stringResource(R.string.calendar_in_minutes, remaining.toMinutes().toInt())

        else -> stringResource(R.string.calendar_airing_now)
    }
}

private fun DayOfWeek.shortLabel(): String =
    getDisplayName(TextStyle.SHORT, Locale.getDefault())
