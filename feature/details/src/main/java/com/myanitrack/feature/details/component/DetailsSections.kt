package com.myanitrack.feature.details.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myanitrack.core.designsystem.component.MediaCover
import com.myanitrack.core.model.CharacterSummary
import com.myanitrack.core.model.MediaRecommendation
import com.myanitrack.core.model.MediaReview
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.PromoVideo
import com.myanitrack.core.model.RelatedGroup
import com.myanitrack.core.model.StaffSummary
import com.myanitrack.feature.details.R

private const val COVER_ASPECT_RATIO = 0.7f

@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** Uzun ozetler icin "devamini oku" davranisi. */
@Composable
internal fun ExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 5,
) {
    var expanded by remember(text) { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = { expanded = !expanded }) {
            Text(
                stringResource(
                    if (expanded) R.string.details_show_less else R.string.details_show_more,
                ),
            )
        }
    }
}

/** Etiket / deger ciftlerinden olusan bilgi izgarasi. */
@Composable
internal fun InfoGrid(rows: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        rows.forEach { (label, value) ->
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(120.dp),
                )
                Text(text = value, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
internal fun ChipRow(
    items: List<String>,
    modifier: Modifier = Modifier,
    onClick: ((Int) -> Unit)? = null,
) {
    FlowRow(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEachIndexed { index, label ->
            SuggestionChip(
                onClick = { onClick?.invoke(index) },
                label = { Text(label) },
                enabled = onClick != null,
            )
        }
    }
}

@Composable
internal fun CharacterRow(characters: List<CharacterSummary>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(characters, key = { it.id }) { character ->
            Column(
                modifier = Modifier.width(84.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MediaCover(
                    imageUrl = character.imageUrl,
                    contentDescription = character.name,
                    fallbackText = character.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(COVER_ASPECT_RATIO),
                )
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
                character.voiceActorName?.let { actor ->
                    Text(
                        text = actor,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
internal fun StaffRow(staff: List<StaffSummary>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(staff, key = { it.id }) { person ->
            Column(
                modifier = Modifier.width(84.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MediaCover(
                    imageUrl = person.imageUrl,
                    contentDescription = person.name,
                    fallbackText = person.name,
                    cornerRadius = 42,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                )
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = person.positions.firstOrNull().orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun MediaRow(
    recommendations: List<MediaRecommendation>,
    onOpenMedia: (MediaType, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(recommendations, key = { it.node.id }) { recommendation ->
            val node = recommendation.node
            Column(
                modifier = Modifier
                    .width(104.dp)
                    .clickable { onOpenMedia(node.mediaType, node.id) },
            ) {
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
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
internal fun RelatedSection(
    groups: List<RelatedGroup>,
    onOpenMedia: (MediaType, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        groups.forEach { group ->
            Text(
                text = group.relation,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                group.entries.forEach { entry ->
                    AssistChip(
                        onClick = { onOpenMedia(entry.mediaType, entry.id) },
                        label = { Text(entry.name) },
                    )
                }
            }
        }
    }
}

/**
 * Tanitim videolari. Gomulu oynatici yerine kapak + oynat dugmesi gosterip
 * YouTube uygulamasina/tarayiciya devrediyoruz: WebView tabanli gomulu oynatici
 * hem agir hem de MAL-in verdigi nocookie embed adresleri sik sik bozuluyor.
 */
@Composable
internal fun PromoRow(
    videos: List<PromoVideo>,
    onPlay: (PromoVideo) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(videos, key = { it.youtubeId ?: it.title.orEmpty() }) { video ->
            Column(
                modifier = Modifier
                    .width(200.dp)
                    .clickable { onPlay(video) },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    MediaCover(
                        imageUrl = video.thumbnailUrl,
                        contentDescription = video.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                    )
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.details_play_trailer),
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                            .padding(6.dp),
                    )
                }
                Text(
                    text = video.title.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Spoiler iceren review-lar dokunulana kadar gizli kalir. */
@Composable
internal fun ReviewCard(review: MediaReview, modifier: Modifier = Modifier) {
    var revealed by remember(review.id) { mutableStateOf(!review.isSpoiler) }

    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MediaCover(
                imageUrl = review.userImageUrl,
                contentDescription = review.userName,
                cornerRadius = 20,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
            )
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(text = review.userName, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = listOfNotNull(
                        review.dateText,
                        review.score?.let { stringResource(R.string.details_review_score, it) },
                    ).joinToString("  -  "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        if (revealed) {
            ExpandableText(text = review.text, collapsedMaxLines = 6)
        } else {
            TextButton(onClick = { revealed = true }) {
                Text(stringResource(R.string.details_reveal_spoiler))
            }
        }

        if (review.isPreliminary) {
            Text(
                text = stringResource(R.string.details_preliminary_review),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
