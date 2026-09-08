package com.myanitrack.feature.profile

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myanitrack.core.designsystem.component.LoadingState
import com.myanitrack.core.designsystem.component.MediaCover
import com.myanitrack.core.designsystem.component.MessageState
import com.myanitrack.core.model.FeedUpdate
import com.myanitrack.core.model.Friend
import com.myanitrack.core.model.HistoryEntry
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.UserMediaStatistics
import com.myanitrack.core.model.UserProfileDetails
import com.myanitrack.core.ui.toUserMessage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRoute(
    onOpenMedia: (MediaType, Int) -> Unit,
    onOpenUser: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenForum: (() -> Unit)? = null,
    onOpenMessages: (() -> Unit)? = null,
    onOpenComments: ((Int) -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = uiState.userName.ifBlank {
                                stringResource(R.string.profile_title)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = stringResource(R.string.profile_back),
                                )
                            }
                        }
                    },
                    actions = {
                        uiState.profile?.let { profile ->
                            IconButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, profile.profileUrl.toUri()),
                                        )
                                    }
                                },
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = stringResource(
                                        R.string.profile_open_on_mal,
                                    ),
                                )
                            }
                        }

                        ProfileOverflowMenu(
                            expanded = menuOpen,
                            onExpandedChange = { menuOpen = it },
                            malUserId = uiState.profile?.malId,
                            onOpenForum = onOpenForum,
                            onOpenMessages = onOpenMessages,
                            onOpenComments = onOpenComments,
                            onOpenSettings = onOpenSettings,
                        )
                    },
                )
                if (uiState.hasUser) {
                    val tabs = ProfileTab.entries
                    PrimaryTabRow(selectedTabIndex = tabs.indexOf(uiState.tab)) {
                        tabs.forEach { tab ->
                            Tab(
                                selected = tab == uiState.tab,
                                onClick = { viewModel.selectTab(tab) },
                                text = { Text(tabLabel(tab)) },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)

        when {
            !uiState.hasUser && !uiState.isLoading -> MessageState(
                title = stringResource(R.string.profile_signed_out_title),
                description = stringResource(R.string.profile_signed_out_description),
                modifier = contentModifier,
            )

            uiState.isLoading -> LoadingState(modifier = contentModifier)

            uiState.profile == null -> MessageState(
                title = stringResource(R.string.profile_unavailable_title),
                description = uiState.error?.toUserMessage(),
                actionLabel = stringResource(com.myanitrack.core.ui.R.string.action_retry),
                onAction = viewModel::refresh,
                modifier = contentModifier,
            )

            else -> PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = contentModifier,
            ) {
                when (uiState.tab) {
                    ProfileTab.OVERVIEW -> OverviewTab(profile = uiState.profile!!)

                    ProfileTab.HISTORY -> HistoryTab(
                        entries = uiState.history,
                        filter = uiState.historyFilter,
                        onFilterChange = viewModel::setHistoryFilter,
                        onOpenMedia = onOpenMedia,
                    )

                    ProfileTab.FRIENDS -> FriendsTab(
                        friends = uiState.friends,
                        onOpenUser = onOpenUser,
                    )

                    ProfileTab.FEED -> FeedTab(
                        updates = uiState.feed,
                        isLoading = uiState.isFeedLoading,
                        isOwnProfile = uiState.isOwnProfile,
                        onOpenMedia = onOpenMedia,
                        onOpenUser = onOpenUser,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(profile: UserProfileDetails) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "header") {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                MediaCover(
                    imageUrl = profile.imageUrl,
                    contentDescription = profile.userName,
                    fallbackText = profile.userName.take(2).uppercase(),
                    cornerRadius = 40,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                )
                Column(
                    modifier = Modifier.padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(text = profile.userName, style = MaterialTheme.typography.titleLarge)
                    profile.location?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    profile.joinedAt?.let {
                        Text(
                            text = stringResource(R.string.profile_joined, it.formatDate()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    profile.lastOnlineAt?.let {
                        Text(
                            text = stringResource(R.string.profile_last_online, it.formatDate()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        profile.animeStats?.let { stats ->
            item(key = "anime-stats") {
                StatisticsSection(
                    title = stringResource(R.string.profile_stats_anime),
                    stats = stats,
                )
            }
        }
        profile.mangaStats?.let { stats ->
            item(key = "manga-stats") {
                StatisticsSection(
                    title = stringResource(R.string.profile_stats_manga),
                    stats = stats,
                )
            }
        }
        item(key = "spacer") { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun StatisticsSection(title: String, stats: UserMediaStatistics) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        // Durum dagilimi: her durum toplam icindeki payi kadar genislikte.
        val total = stats.statusBreakdown.sumOf { it.second }
        if (total > 0) {
            Row(modifier = Modifier.fillMaxWidth()) {
                stats.statusBreakdown.forEach { (_, count) ->
                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier
                            .weight(count.toFloat())
                            .padding(end = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCell(stringResource(R.string.profile_stat_days), stats.daysSpent.formatScore())
            StatCell(
                stringResource(R.string.profile_stat_mean_score),
                stats.meanScore.formatScore(),
            )
            StatCell(
                stringResource(R.string.profile_stat_total),
                stats.totalEntries.toString(),
            )
            StatCell(
                stringResource(
                    if (stats.mediaType.isAnime) {
                        R.string.profile_stat_episodes
                    } else {
                        R.string.profile_stat_chapters
                    },
                ),
                stats.unitsConsumed.toString(),
            )
            if (stats.mediaType.isManga) {
                StatCell(
                    stringResource(R.string.profile_stat_volumes),
                    stats.volumesRead.toString(),
                )
            }
            StatCell(
                stringResource(
                    if (stats.mediaType.isAnime) {
                        R.string.profile_stat_rewatched
                    } else {
                        R.string.profile_stat_reread
                    },
                ),
                stats.repeated.toString(),
            )
        }
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = value, style = MaterialTheme.typography.titleSmall)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistoryTab(
    entries: List<HistoryEntry>,
    filter: MediaType?,
    onFilterChange: (MediaType?) -> Unit,
    onOpenMedia: (MediaType, Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = filter == null,
                onClick = { onFilterChange(null) },
                label = { Text(stringResource(R.string.profile_history_all)) },
            )
            MediaType.entries.forEach { type ->
                FilterChip(
                    selected = filter == type,
                    onClick = { onFilterChange(type) },
                    label = {
                        Text(
                            stringResource(
                                if (type.isAnime) {
                                    com.myanitrack.core.ui.R.string.media_type_anime
                                } else {
                                    com.myanitrack.core.ui.R.string.media_type_manga
                                },
                            ),
                        )
                    },
                )
            }
        }

        if (entries.isEmpty()) {
            MessageState(title = stringResource(R.string.profile_history_empty))
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(entries, key = { "${it.mediaType}-${it.malId}-${it.date?.epochSecond}" }) {
                    HistoryRow(entry = it, onClick = { onOpenMedia(it.mediaType, it.malId) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            entry.date?.let {
                Text(
                    text = it.formatDateTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = stringResource(R.string.profile_history_increment, entry.increment),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun FriendsTab(friends: List<Friend>, onOpenUser: (String) -> Unit) {
    if (friends.isEmpty()) {
        MessageState(title = stringResource(R.string.profile_friends_empty))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(friends, key = { it.userName }) { friend ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenUser(friend.userName) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MediaCover(
                    imageUrl = friend.imageUrl,
                    contentDescription = friend.userName,
                    fallbackText = friend.userName.take(2).uppercase(),
                    cornerRadius = 24,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(text = friend.userName, style = MaterialTheme.typography.bodyLarge)
                    friend.lastOnlineAt?.let {
                        Text(
                            text = stringResource(R.string.profile_last_online, it.formatDate()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun FeedTab(
    updates: List<FeedUpdate>,
    isLoading: Boolean,
    isOwnProfile: Boolean,
    onOpenMedia: (MediaType, Int) -> Unit,
    onOpenUser: (String) -> Unit,
) {
    when {
        isLoading -> LoadingState()

        updates.isEmpty() -> MessageState(
            title = stringResource(R.string.profile_feed_empty),
            description = stringResource(
                if (isOwnProfile) {
                    R.string.profile_feed_own_hint
                } else {
                    R.string.profile_feed_other_hint
                },
            ),
        )

        else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(updates, key = { it.key }) { update ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenMedia(update.mediaType, update.malId) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    if (isOwnProfile) {
                        Text(
                            text = update.userName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onOpenUser(update.userName) },
                        )
                    }
                    Text(
                        text = update.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = update.statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    update.publishedAt?.let {
                        Text(
                            text = it.formatDateTime(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun tabLabel(tab: ProfileTab): String = stringResource(
    when (tab) {
        ProfileTab.OVERVIEW -> R.string.profile_tab_overview
        ProfileTab.HISTORY -> R.string.profile_tab_history
        ProfileTab.FRIENDS -> R.string.profile_tab_friends
        ProfileTab.FEED -> R.string.profile_tab_feed
    },
)

private fun Instant.formatDate(): String =
    atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))

private fun Instant.formatDateTime(): String =
    atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", Locale.getDefault()))

/** Ondalik degerleri iki basamakla, yerel bicimde gosterir. */
private fun Double.formatScore(): String = String.format(Locale.getDefault(), "%.1f", this)

/**
 * Hesap ve topluluk eylemleri.
 *
 * Alt gezinme cubugu Material 3-un onerdigi 5 sekmede dolu oldugu icin forum,
 * mesajlar, profil yorumlari ve ayarlar buraya toplandi. Hepsi hesap baglamli
 * oldugundan profil ekrani dogal ev.
 *
 * Yorumlar yalnizca MAL sayisal kimligi bilindiginde acilabilir (adres kullanici
 * adini degil kimligi istiyor).
 */
@Composable
private fun ProfileOverflowMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    malUserId: Int?,
    onOpenForum: (() -> Unit)?,
    onOpenMessages: (() -> Unit)?,
    onOpenComments: ((Int) -> Unit)?,
    onOpenSettings: (() -> Unit)?,
) {
    val hasAnyAction = onOpenForum != null || onOpenMessages != null ||
        onOpenSettings != null || (onOpenComments != null && malUserId != null)
    if (!hasAnyAction) return

    IconButton(onClick = { onExpandedChange(true) }) {
        Icon(
            Icons.Outlined.MoreVert,
            contentDescription = stringResource(R.string.profile_more),
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
        onOpenForum?.let { action ->
            DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_menu_forum)) },
                onClick = {
                    onExpandedChange(false)
                    action()
                },
            )
        }
        onOpenMessages?.let { action ->
            DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_menu_messages)) },
                onClick = {
                    onExpandedChange(false)
                    action()
                },
            )
        }
        if (onOpenComments != null && malUserId != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_menu_comments)) },
                onClick = {
                    onExpandedChange(false)
                    onOpenComments(malUserId)
                },
            )
        }
        onOpenSettings?.let { action ->
            DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_settings)) },
                onClick = {
                    onExpandedChange(false)
                    action()
                },
            )
        }
    }
}
