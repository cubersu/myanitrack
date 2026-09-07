package com.myanitrack.feature.mylist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myanitrack.core.designsystem.component.LoadingState
import com.myanitrack.core.designsystem.component.MessageState
import com.myanitrack.core.model.ListSortOption
import com.myanitrack.core.model.ListStatus
import com.myanitrack.core.model.ListStatusUpdate
import com.myanitrack.core.model.ListViewMode
import com.myanitrack.core.model.MediaListEntry
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.SortDirection
import com.myanitrack.core.ui.ObserveAsEvents
import com.myanitrack.core.ui.toUserMessage
import com.myanitrack.feature.mylist.component.CompactListItem
import com.myanitrack.feature.mylist.component.DetailedListItem
import com.myanitrack.feature.mylist.component.EditEntrySheet
import com.myanitrack.feature.mylist.component.GridListItem
import kotlinx.coroutines.launch

@Composable
fun MyListRoute(
    modifier: Modifier = Modifier,
    viewModel: MyListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        val message = when (event) {
            is MyListEvent.ShowError -> event.error.toUserMessage(context)
            is MyListEvent.EntryDeleted ->
                context.getString(R.string.mylist_entry_deleted, event.title)
        }
        scope.launch {
            // Ust uste gelen bildirimler kuyruga girmesin, sonuncusu gosterilsin.
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    MyListScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onMediaTypeChange = viewModel::selectMediaType,
        onStatusChange = viewModel::selectStatus,
        onQueryChange = viewModel::setQuery,
        onSearchActiveChange = viewModel::setSearchActive,
        onTagChange = viewModel::selectTag,
        onViewModeChange = viewModel::setViewMode,
        onSortChange = viewModel::setSort,
        onRefresh = viewModel::refresh,
        onIncrement = viewModel::incrementProgress,
        onEntryClick = viewModel::startEditing,
        onDismissEdit = viewModel::stopEditing,
        onSaveEdit = viewModel::saveEdit,
        onDeleteEntry = viewModel::deleteEntry,
        modifier = modifier,
    )
}

@Composable
internal fun MyListScreen(
    uiState: MyListUiState,
    snackbarHostState: SnackbarHostState,
    onMediaTypeChange: (MediaType) -> Unit,
    onStatusChange: (ListStatus?) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onTagChange: (String?) -> Unit,
    onViewModeChange: (ListViewMode) -> Unit,
    onSortChange: (ListSortOption, SortDirection) -> Unit,
    onRefresh: () -> Unit,
    onIncrement: (MediaListEntry) -> Unit,
    onEntryClick: (MediaListEntry) -> Unit,
    onDismissEdit: () -> Unit,
    onSaveEdit: (MediaListEntry, ListStatusUpdate) -> Unit,
    onDeleteEntry: (MediaListEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                MyListTopBar(
                    uiState = uiState,
                    onMediaTypeChange = onMediaTypeChange,
                    onQueryChange = onQueryChange,
                    onSearchActiveChange = onSearchActiveChange,
                    onViewModeChange = onViewModeChange,
                    onSortChange = onSortChange,
                )
                StatusTabs(
                    selected = uiState.filter.status,
                    counts = uiState.statusCounts,
                    isAnime = uiState.mediaType.isAnime,
                    onSelect = onStatusChange,
                )
                if (uiState.availableTags.isNotEmpty()) {
                    TagFilterRow(
                        tags = uiState.availableTags,
                        selected = uiState.filter.tag,
                        onSelect = onTagChange,
                    )
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
                uiState.isInitialLoading -> LoadingState()

                uiState.isEmpty -> MessageState(
                    title = stringResource(R.string.mylist_empty_title),
                    description = stringResource(R.string.mylist_empty_description),
                    icon = Icons.Outlined.Inbox,
                    actionLabel = stringResource(com.myanitrack.core.ui.R.string.action_refresh),
                    onAction = onRefresh,
                )

                else -> MyListContent(
                    entries = uiState.entries,
                    viewMode = uiState.viewMode,
                    onEntryClick = onEntryClick,
                    onIncrement = onIncrement,
                )
            }
        }
    }

    uiState.editingEntry?.let { entry ->
        EditEntrySheet(
            entry = entry,
            onDismiss = onDismissEdit,
            onSave = { update -> onSaveEdit(entry, update) },
            onDelete = { onDeleteEntry(entry) },
        )
    }
}

@Composable
private fun MyListContent(
    entries: List<MediaListEntry>,
    viewMode: ListViewMode,
    onEntryClick: (MediaListEntry) -> Unit,
    onIncrement: (MediaListEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (viewMode) {
        ListViewMode.GRID -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 104.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            items(entries, key = { it.id }) { entry ->
                GridListItem(entry = entry, onClick = { onEntryClick(entry) })
            }
        }

        ListViewMode.COMPACT -> LazyColumn(modifier = modifier.fillMaxSize()) {
            items(entries, key = { it.id }) { entry ->
                CompactListItem(
                    entry = entry,
                    onClick = { onEntryClick(entry) },
                    onIncrement = { onIncrement(entry) },
                )
            }
        }

        ListViewMode.DETAILED_GRID -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 320.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            items(entries, key = { it.id }) { entry ->
                DetailedListItem(
                    entry = entry,
                    onClick = { onEntryClick(entry) },
                    onIncrement = { onIncrement(entry) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyListTopBar(
    uiState: MyListUiState,
    onMediaTypeChange: (MediaType) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onViewModeChange: (ListViewMode) -> Unit,
    onSortChange: (ListSortOption, SortDirection) -> Unit,
) {
    var viewMenuOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            if (uiState.isSearchActive) {
                TextField(
                    value = uiState.filter.query,
                    onValueChange = onQueryChange,
                    placeholder = { Text(stringResource(R.string.mylist_search_placeholder)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                MediaTypeToggle(selected = uiState.mediaType, onSelect = onMediaTypeChange)
            }
        },
        actions = {
            IconButton(onClick = { onSearchActiveChange(!uiState.isSearchActive) }) {
                Icon(
                    imageVector = if (uiState.isSearchActive) {
                        Icons.Outlined.Close
                    } else {
                        Icons.Outlined.Search
                    },
                    contentDescription = stringResource(R.string.mylist_search),
                )
            }

            Box {
                IconButton(onClick = { viewMenuOpen = true }) {
                    Icon(
                        Icons.Outlined.ViewAgenda,
                        contentDescription = stringResource(R.string.mylist_view_mode),
                    )
                }
                DropdownMenu(expanded = viewMenuOpen, onDismissRequest = { viewMenuOpen = false }) {
                    ListViewMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(viewModeLabel(mode)) },
                            onClick = {
                                onViewModeChange(mode)
                                viewMenuOpen = false
                            },
                            trailingIcon = {
                                if (mode == uiState.viewMode) {
                                    Icon(Icons.Outlined.Check, contentDescription = null)
                                }
                            },
                        )
                    }
                }
            }

            Box {
                IconButton(onClick = { sortMenuOpen = true }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = stringResource(R.string.mylist_sort),
                    )
                }
                DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                    ListSortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(sortLabel(option)) },
                            onClick = {
                                onSortChange(option, uiState.filter.nextDirectionFor(option))
                                sortMenuOpen = false
                            },
                            trailingIcon = {
                                if (option == uiState.filter.sortBy) {
                                    Icon(Icons.Outlined.Check, contentDescription = null)
                                }
                            },
                        )
                    }
                }
            }
        },
    )
}

/** Ayni olcute tekrar basmak siralama yonunu tersine cevirir. */
private fun com.myanitrack.core.model.ListFilter.nextDirectionFor(
    option: ListSortOption,
): SortDirection = when {
    option != sortBy -> SortDirection.ASCENDING
    sortDirection == SortDirection.ASCENDING -> SortDirection.DESCENDING
    else -> SortDirection.ASCENDING
}

@Composable
private fun MediaTypeToggle(
    selected: MediaType,
    onSelect: (MediaType) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MediaType.entries.forEach { type ->
            FilterChip(
                selected = type == selected,
                onClick = { onSelect(type) },
                label = {
                    Text(
                        stringResource(
                            if (type.isAnime) R.string.media_type_anime
                            else R.string.media_type_manga,
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun StatusTabs(
    selected: ListStatus?,
    counts: Map<ListStatus, Int>,
    isAnime: Boolean,
    onSelect: (ListStatus?) -> Unit,
) {
    val statuses = ListStatus.entries
    // "Tumu" sekmesi listenin sonunda; secili durum yoksa o secilidir.
    val selectedIndex = statuses.indexOf(selected).takeIf { it >= 0 } ?: statuses.size

    PrimaryScrollableTabRow(selectedTabIndex = selectedIndex, edgePadding = 8.dp) {
        statuses.forEach { status ->
            Tab(
                selected = status == selected,
                onClick = { onSelect(status) },
                text = {
                    Text(
                        text = "${statusTabLabel(status, isAnime)} (${counts[status] ?: 0})",
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
        Tab(
            selected = selected == null,
            onClick = { onSelect(null) },
            text = { Text(stringResource(R.string.status_all)) },
        )
    }
}

@Composable
private fun TagFilterRow(
    tags: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(tags, key = { it }) { tag ->
            FilterChip(
                selected = tag == selected,
                onClick = { onSelect(if (tag == selected) null else tag) },
                label = { Text(tag) },
            )
        }
    }
}

@Composable
private fun viewModeLabel(mode: ListViewMode): String = stringResource(
    when (mode) {
        ListViewMode.GRID -> R.string.view_mode_grid
        ListViewMode.COMPACT -> R.string.view_mode_compact
        ListViewMode.DETAILED_GRID -> R.string.view_mode_detailed
    },
)

@Composable
private fun sortLabel(option: ListSortOption): String = stringResource(
    when (option) {
        ListSortOption.TITLE -> R.string.sort_title
        ListSortOption.SCORE -> R.string.sort_score
        ListSortOption.PROGRESS -> R.string.sort_progress
        ListSortOption.LAST_UPDATED -> R.string.sort_last_updated
        ListSortOption.START_DATE -> R.string.sort_start_date
        ListSortOption.MEAN_SCORE -> R.string.sort_mean_score
        ListSortOption.POPULARITY -> R.string.sort_popularity
    },
)

@Composable
private fun statusTabLabel(status: ListStatus, isAnime: Boolean): String = stringResource(
    when (status) {
        ListStatus.WATCHING -> if (isAnime) R.string.status_watching else R.string.status_reading
        ListStatus.COMPLETED -> R.string.status_completed
        ListStatus.ON_HOLD -> R.string.status_on_hold
        ListStatus.DROPPED -> R.string.status_dropped
        ListStatus.PLAN_TO_WATCH ->
            if (isAnime) R.string.status_plan_to_watch else R.string.status_plan_to_read
    },
)
