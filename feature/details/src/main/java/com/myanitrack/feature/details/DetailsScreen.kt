package com.myanitrack.feature.details

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.myanitrack.core.designsystem.component.LoadingState
import com.myanitrack.core.designsystem.component.MediaCover
import com.myanitrack.core.designsystem.component.MessageState
import com.myanitrack.core.model.ListStatus
import com.myanitrack.core.model.MediaDetails
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.ui.ObserveAsEvents
import com.myanitrack.core.ui.toUserMessage
import com.myanitrack.feature.details.component.CharacterRow
import com.myanitrack.feature.details.component.ChipRow
import com.myanitrack.feature.details.component.ExpandableText
import com.myanitrack.feature.details.component.InfoGrid
import com.myanitrack.feature.details.component.MediaRow
import com.myanitrack.feature.details.component.PromoRow
import com.myanitrack.feature.details.component.RelatedSection
import com.myanitrack.feature.details.component.ReviewCard
import com.myanitrack.feature.details.component.SectionTitle
import com.myanitrack.feature.details.component.StaffRow
import kotlinx.coroutines.launch

private const val COVER_ASPECT_RATIO = 0.7f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsRoute(
    onBack: () -> Unit,
    onOpenMedia: (MediaType, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reviews = viewModel.reviews.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        val message = when (event) {
            is DetailsEvent.ShowError -> event.error.toUserMessage(context)
            is DetailsEvent.ShowMessage ->
                context.getString(R.string.details_added_to_list, event.titleAdded)
        }
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    val openUrl: (String) -> Unit = { url ->
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.details?.title.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.details_back),
                        )
                    }
                },
                actions = {
                    uiState.details?.let { details ->
                        IconButton(onClick = { openUrl(details.malUrl) }) {
                            Icon(
                                Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = stringResource(R.string.details_open_on_mal),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        val details = uiState.details
        when {
            uiState.isLoading && details == null -> LoadingState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )

            details == null -> MessageState(
                title = stringResource(R.string.details_unavailable_title),
                description = uiState.error?.toUserMessage(),
                actionLabel = stringResource(com.myanitrack.core.ui.R.string.action_retry),
                onAction = viewModel::refresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )

            else -> DetailsContent(
                uiState = uiState,
                details = details,
                reviewsCount = reviews.itemCount,
                reviewAt = { index -> reviews[index] },
                reviewKey = reviews.itemKey { it.id },
                onOpenMedia = onOpenMedia,
                onAddToList = viewModel::addToList,
                onEditClick = viewModel::startEditing,
                onPlayVideo = { video -> video.watchUrl?.let(openUrl) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }

    val entry = uiState.listEntry
    if (uiState.isEditing && entry != null) {
        DetailsEditSheet(
            entry = entry,
            onDismiss = viewModel::stopEditing,
            onSave = viewModel::saveEdit,
            onDelete = viewModel::removeFromList,
        )
    }
}

@Composable
private fun DetailsContent(
    uiState: DetailsUiState,
    details: MediaDetails,
    reviewsCount: Int,
    reviewAt: (Int) -> com.myanitrack.core.model.MediaReview?,
    reviewKey: (Int) -> Any,
    onOpenMedia: (MediaType, Int) -> Unit,
    onAddToList: (ListStatus) -> Unit,
    onEditClick: () -> Unit,
    onPlayVideo: (com.myanitrack.core.model.PromoVideo) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item(key = "header") {
            DetailsHeader(
                details = details,
                uiState = uiState,
                onAddToList = onAddToList,
                onEditClick = onEditClick,
            )
        }

        if (details.genres.isNotEmpty() || details.themes.isNotEmpty()) {
            item(key = "genres") {
                SectionTitle(stringResource(R.string.details_section_genres))
                ChipRow((details.genres + details.themes + details.demographics).map { it.name })
            }
        }

        details.synopsis?.takeIf { it.isNotBlank() }?.let { synopsis ->
            item(key = "synopsis") {
                SectionTitle(stringResource(R.string.details_section_synopsis))
                ExpandableText(
                    text = synopsis,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        item(key = "info") {
            SectionTitle(stringResource(R.string.details_section_information))
            InfoGrid(rows = details.infoRows())
        }

        if (uiState.videos.isNotEmpty()) {
            item(key = "videos") {
                SectionTitle(stringResource(R.string.details_section_videos))
                PromoRow(videos = uiState.videos, onPlay = onPlayVideo)
            }
        }

        if (uiState.characters.isNotEmpty()) {
            item(key = "characters") {
                SectionTitle(stringResource(R.string.details_section_characters))
                CharacterRow(characters = uiState.characters.take(MAX_ROW_ITEMS))
            }
        }

        if (uiState.staff.isNotEmpty()) {
            item(key = "staff") {
                SectionTitle(stringResource(R.string.details_section_staff))
                StaffRow(staff = uiState.staff.take(MAX_ROW_ITEMS))
            }
        }

        if (details.related.isNotEmpty()) {
            item(key = "related") {
                SectionTitle(stringResource(R.string.details_section_related))
                RelatedSection(groups = details.related, onOpenMedia = onOpenMedia)
            }
        }

        if (uiState.recommendations.isNotEmpty()) {
            item(key = "recommendations") {
                SectionTitle(stringResource(R.string.details_section_recommendations))
                MediaRow(
                    recommendations = uiState.recommendations.take(MAX_ROW_ITEMS),
                    onOpenMedia = onOpenMedia,
                )
            }
        }

        if (details.openingThemes.isNotEmpty() || details.endingThemes.isNotEmpty()) {
            item(key = "themes") {
                SectionTitle(stringResource(R.string.details_section_theme_songs))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    (details.openingThemes + details.endingThemes).forEach { song ->
                        Text(
                            text = song,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        }

        if (reviewsCount > 0) {
            item(key = "reviews-title") {
                HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
                SectionTitle(stringResource(R.string.details_section_reviews))
            }
            items(count = reviewsCount, key = reviewKey) { index ->
                reviewAt(index)?.let { ReviewCard(review = it) }
            }
        }

        item(key = "bottom-spacer") { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun DetailsHeader(
    details: MediaDetails,
    uiState: DetailsUiState,
    onAddToList: (ListStatus) -> Unit,
    onEditClick: () -> Unit,
) {
    var addMenuOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row {
            MediaCover(
                imageUrl = details.picture.best,
                contentDescription = details.title,
                fallbackText = details.title,
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(COVER_ASPECT_RATIO),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = details.title, style = MaterialTheme.typography.titleLarge)
                details.englishTitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                details.statistics.score?.let { score ->
                    Text(
                        text = stringResource(
                            R.string.details_score_with_users,
                            score,
                            details.statistics.scoredBy ?: 0,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                details.statistics.rank?.let { rank ->
                    Text(
                        text = stringResource(R.string.details_rank, rank),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (uiState.isInList) {
            Button(onClick = onEditClick, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
                Text(
                    text = stringResource(R.string.details_edit_entry),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { addMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text(
                        text = stringResource(R.string.details_add_to_list),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                DropdownMenu(expanded = addMenuOpen, onDismissRequest = { addMenuOpen = false }) {
                    ListStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(statusLabel(status, details.mediaType.isAnime)) },
                            onClick = {
                                addMenuOpen = false
                                onAddToList(status)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun statusLabel(status: ListStatus, isAnime: Boolean): String = stringResource(
    when (status) {
        ListStatus.WATCHING -> if (isAnime) R.string.details_status_watching else R.string.details_status_reading
        ListStatus.COMPLETED -> R.string.details_status_completed
        ListStatus.ON_HOLD -> R.string.details_status_on_hold
        ListStatus.DROPPED -> R.string.details_status_dropped
        ListStatus.PLAN_TO_WATCH ->
            if (isAnime) R.string.details_status_plan_to_watch else R.string.details_status_plan_to_read
    },
)

@Composable
private fun MediaDetails.infoRows(): List<Pair<String, String>> = buildList {
    add(stringResource(R.string.details_info_type) to subType.name.replace('_', ' '))
    airingStatus.takeIf { it.name != "UNKNOWN" }?.let {
        add(stringResource(R.string.details_info_status) to it.name.replace('_', ' '))
    }
    totalUnits?.let {
        add(
            stringResource(
                if (mediaType.isAnime) R.string.details_info_episodes
                else R.string.details_info_chapters,
            ) to it.toString(),
        )
    }
    numVolumes?.let { add(stringResource(R.string.details_info_volumes) to it.toString()) }
    durationText?.let { add(stringResource(R.string.details_info_duration) to it) }
    startDate?.let {
        add(stringResource(R.string.details_info_aired) to "$it${endDate?.let { end -> " - $end" }.orEmpty()}")
    }
    season?.let {
        add(
            stringResource(R.string.details_info_season) to
                "${it.name.name.lowercase().replaceFirstChar(Char::uppercase)} ${it.year}",
        )
    }
    broadcast?.let { add(stringResource(R.string.details_info_broadcast) to it) }
    source?.let { add(stringResource(R.string.details_info_source) to it) }
    rating?.let { add(stringResource(R.string.details_info_rating) to it) }
    studios.takeIf { it.isNotEmpty() }?.let {
        add(stringResource(R.string.details_info_studios) to it.joinToString(", ") { s -> s.name })
    }
    authors.takeIf { it.isNotEmpty() }?.let {
        add(stringResource(R.string.details_info_authors) to it.joinToString(", ") { a -> a.name })
    }
    statistics.members?.let { add(stringResource(R.string.details_info_members) to it.toString()) }
    statistics.favorites?.let {
        add(stringResource(R.string.details_info_favorites) to it.toString())
    }
}

private const val MAX_ROW_ITEMS = 20
