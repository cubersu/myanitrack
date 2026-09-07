package com.myanitrack.feature.browse

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.myanitrack.core.common.result.asAppError
import com.myanitrack.core.designsystem.component.LoadingState
import com.myanitrack.core.designsystem.component.MediaCover
import com.myanitrack.core.designsystem.component.MessageState
import com.myanitrack.core.model.MediaNode
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.RecommendationPair
import com.myanitrack.core.model.TopCategory
import com.myanitrack.core.ui.toUserMessage

private const val COVER_ASPECT_RATIO = 0.7f

@Composable
fun BrowseRoute(
    onOpenMedia: (MediaType, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BrowseScreen(
        uiState = uiState,
        topResults = viewModel.topResults.collectAsLazyPagingItems(),
        seasonResults = viewModel.seasonResults.collectAsLazyPagingItems(),
        searchResults = viewModel.searchResults.collectAsLazyPagingItems(),
        recommendations = viewModel.recommendations.collectAsLazyPagingItems(),
        onTabChange = viewModel::selectTab,
        onMediaTypeChange = viewModel::selectMediaType,
        onTopCategoryChange = viewModel::selectTopCategory,
        onQueryChange = viewModel::setQuery,
        onGenreChange = viewModel::selectGenre,
        onNextSeason = viewModel::nextSeason,
        onPreviousSeason = viewModel::previousSeason,
        onOpenMedia = onOpenMedia,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrowseScreen(
    uiState: BrowseUiState,
    topResults: LazyPagingItems<MediaNode>,
    seasonResults: LazyPagingItems<MediaNode>,
    searchResults: LazyPagingItems<MediaNode>,
    recommendations: LazyPagingItems<RecommendationPair>,
    onTabChange: (BrowseTab) -> Unit,
    onMediaTypeChange: (MediaType) -> Unit,
    onTopCategoryChange: (TopCategory) -> Unit,
    onQueryChange: (String) -> Unit,
    onGenreChange: (com.myanitrack.core.model.NamedRef?) -> Unit,
    onNextSeason: () -> Unit,
    onPreviousSeason: () -> Unit,
    onOpenMedia: (MediaType, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = BrowseTab.entries.filter { it != BrowseTab.SEASON || uiState.isSeasonTabAvailable }
    val selectedIndex = tabs.indexOf(uiState.tab).coerceAtLeast(0)

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MediaType.entries.forEach { type ->
                                FilterChip(
                                    selected = type == uiState.mediaType,
                                    onClick = { onMediaTypeChange(type) },
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
                    },
                )
                PrimaryTabRow(selectedTabIndex = selectedIndex) {
                    tabs.forEach { tab ->
                        Tab(
                            selected = tab == uiState.tab,
                            onClick = { onTabChange(tab) },
                            text = { Text(tabLabel(tab)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (uiState.tab) {
                BrowseTab.TOP -> {
                    TopCategoryRow(
                        categories = uiState.availableTopCategories,
                        selected = uiState.topCategory,
                        onSelect = onTopCategoryChange,
                    )
                    MediaGrid(items = topResults, onOpenMedia = onOpenMedia)
                }

                BrowseTab.SEASON -> {
                    SeasonSelector(
                        label = "${
                            uiState.season.name.name.lowercase().replaceFirstChar(Char::uppercase)
                        } ${uiState.season.year}",
                        onPrevious = onPreviousSeason,
                        onNext = onNextSeason,
                    )
                    MediaGrid(items = seasonResults, onOpenMedia = onOpenMedia)
                }

                BrowseTab.SEARCH -> {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = onQueryChange,
                        label = { Text(stringResource(R.string.browse_search_label)) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    if (uiState.genres.isNotEmpty()) {
                        GenreRow(
                            genres = uiState.genres,
                            selected = uiState.selectedGenre,
                            onSelect = onGenreChange,
                        )
                    }
                    MediaGrid(items = searchResults, onOpenMedia = onOpenMedia)
                }

                BrowseTab.RECOMMENDATIONS -> RecommendationList(
                    items = recommendations,
                    onOpenMedia = onOpenMedia,
                )
            }
        }
    }
}

@Composable
private fun TopCategoryRow(
    categories: List<TopCategory>,
    selected: TopCategory,
    onSelect: (TopCategory) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(categories, key = { it.name }) { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelect(category) },
                label = { Text(topCategoryLabel(category)) },
            )
        }
    }
}

@Composable
private fun GenreRow(
    genres: List<com.myanitrack.core.model.NamedRef>,
    selected: com.myanitrack.core.model.NamedRef?,
    onSelect: (com.myanitrack.core.model.NamedRef?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(genres, key = { it.id }) { genre ->
            FilterChip(
                selected = genre.id == selected?.id,
                onClick = { onSelect(genre) },
                label = { Text(genre.name) },
            )
        }
    }
}

@Composable
private fun SeasonSelector(label: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.browse_previous_season),
            )
        }
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onNext) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = stringResource(R.string.browse_next_season),
            )
        }
    }
}

/**
 * Paging listelerinin ortak izgarasi. Yukleme ve hata durumlari
 * [LazyPagingItems.loadState] uzerinden ele alinir.
 */
@Composable
private fun MediaGrid(
    items: LazyPagingItems<MediaNode>,
    onOpenMedia: (MediaType, Int) -> Unit,
) {
    val refreshState = items.loadState.refresh
    when {
        refreshState is LoadState.Loading && items.itemCount == 0 -> LoadingState()

        refreshState is LoadState.Error && items.itemCount == 0 -> MessageState(
            title = stringResource(R.string.browse_error_title),
            description = refreshState.error.asAppError().toUserMessage(),
            actionLabel = stringResource(com.myanitrack.core.ui.R.string.action_retry),
            onAction = items::retry,
        )

        items.itemCount == 0 -> MessageState(
            title = stringResource(R.string.browse_empty_title),
            description = stringResource(R.string.browse_empty_description),
        )

        else -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(count = items.itemCount, key = items.itemKey { it.id }) { index ->
                items[index]?.let { node ->
                    MediaGridItem(node = node, onClick = { onOpenMedia(node.mediaType, node.id) })
                }
            }
            if (items.loadState.append is LoadState.Loading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaGridItem(node: MediaNode, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        MediaCover(
            imageUrl = node.picture.best,
            contentDescription = node.title,
            fallbackText = node.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(COVER_ASPECT_RATIO),
        )
        Text(
            text = node.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        node.meanScore?.let { score ->
            Text(
                text = stringResource(R.string.browse_score, score),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "X-i begendiysen Y-yi dene" ikilileri. */
@Composable
private fun RecommendationList(
    items: LazyPagingItems<RecommendationPair>,
    onOpenMedia: (MediaType, Int) -> Unit,
) {
    val refreshState = items.loadState.refresh
    when {
        refreshState is LoadState.Loading && items.itemCount == 0 -> LoadingState()

        refreshState is LoadState.Error && items.itemCount == 0 -> MessageState(
            title = stringResource(R.string.browse_error_title),
            description = refreshState.error.asAppError().toUserMessage(),
            actionLabel = stringResource(com.myanitrack.core.ui.R.string.action_retry),
            onAction = items::retry,
        )

        else -> LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(count = items.itemCount, key = items.itemKey { it.key }) { index ->
                items[index]?.let { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RecommendationSide(
                            node = pair.liked,
                            captionRes = R.string.browse_if_you_liked,
                            onClick = { onOpenMedia(pair.liked.mediaType, pair.liked.id) },
                        )
                        RecommendationSide(
                            node = pair.suggested,
                            captionRes = R.string.browse_then_try,
                            onClick = { onOpenMedia(pair.suggested.mediaType, pair.suggested.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationSide(node: MediaNode, captionRes: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = stringResource(captionRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        MediaCover(
            imageUrl = node.picture.best,
            contentDescription = node.title,
            fallbackText = node.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(COVER_ASPECT_RATIO),
        )
        Text(
            text = node.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun tabLabel(tab: BrowseTab): String = stringResource(
    when (tab) {
        BrowseTab.TOP -> R.string.browse_tab_top
        BrowseTab.SEASON -> R.string.browse_tab_season
        BrowseTab.SEARCH -> R.string.browse_tab_search
        BrowseTab.RECOMMENDATIONS -> R.string.browse_tab_recommendations
    },
)

@Composable
private fun topCategoryLabel(category: TopCategory): String = stringResource(
    when (category) {
        TopCategory.ALL -> R.string.browse_top_all
        TopCategory.AIRING -> R.string.browse_top_airing
        TopCategory.PUBLISHING -> R.string.browse_top_publishing
        TopCategory.UPCOMING -> R.string.browse_top_upcoming
        TopCategory.BY_POPULARITY -> R.string.browse_top_popularity
        TopCategory.FAVORITE -> R.string.browse_top_favorite
    },
)
