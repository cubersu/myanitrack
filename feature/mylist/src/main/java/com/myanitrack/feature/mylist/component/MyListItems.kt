package com.myanitrack.feature.mylist.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myanitrack.core.designsystem.component.MediaCover
import com.myanitrack.core.designsystem.component.ScoreBadge
import com.myanitrack.core.model.MediaListEntry
import com.myanitrack.feature.mylist.R

/** MAL kapak gorsellerinin standart en-boy orani. */
private const val COVER_ASPECT_RATIO = 0.7f

/**
 * Ilerleme metni: "12 / 24" ya da toplam bilinmiyorsa "12 / ?".
 */
@Composable
internal fun progressText(entry: MediaListEntry): String {
    val total = entry.total?.takeIf { it > 0 }?.toString()
        ?: stringResource(R.string.mylist_unknown_total)
    return "${entry.progress} / $total"
}

/** Yalnizca kapak + baslik; en yogun gorunum. */
@Composable
internal fun GridListItem(
    entry: MediaListEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Box {
            MediaCover(
                imageUrl = entry.node.picture.best,
                contentDescription = entry.node.title,
                fallbackText = entry.node.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(COVER_ASPECT_RATIO),
            )
            ScoreBadge(
                score = entry.listStatus.score,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
            )
        }
        Text(
            text = entry.node.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = progressText(entry),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Tek satirlik kompakt gorunum: kucuk kapak, baslik, ilerleme ve +1. */
@Composable
internal fun CompactListItem(
    entry: MediaListEntry,
    onClick: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MediaCover(
            imageUrl = entry.node.picture.medium ?: entry.node.picture.large,
            contentDescription = entry.node.title,
            cornerRadius = 4,
            modifier = Modifier
                .width(40.dp)
                .aspectRatio(COVER_ASPECT_RATIO),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = entry.node.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = progressText(entry),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ScoreBadge(score = entry.listStatus.score, modifier = Modifier.padding(end = 8.dp))
        IncrementButton(entry = entry, onIncrement = onIncrement)
    }
}

/** Kapak + puan + ilerleme cubugu + etiketler; varsayilan gorunum. */
@Composable
internal fun DetailedListItem(
    entry: MediaListEntry,
    onClick: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Row(modifier = Modifier.padding(8.dp)) {
            MediaCover(
                imageUrl = entry.node.picture.best,
                contentDescription = entry.node.title,
                fallbackText = entry.node.title,
                modifier = Modifier
                    .width(72.dp)
                    .aspectRatio(COVER_ASPECT_RATIO),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = entry.node.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScoreBadge(score = entry.listStatus.score)
                    Text(
                        text = progressText(entry),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                entry.progressFraction?.let { fraction ->
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (entry.listStatus.tags.isNotEmpty()) {
                    Text(
                        text = entry.listStatus.tags.joinToString(", "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IncrementButton(
                entry = entry,
                onIncrement = onIncrement,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
}

/** Son bolume ulasilmissa dugme devre disi kalir. */
@Composable
private fun IncrementButton(
    entry: MediaListEntry,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalIconButton(
        onClick = onIncrement,
        enabled = entry.canIncrement,
        modifier = modifier.size(36.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(),
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(R.string.mylist_increment_progress),
            modifier = Modifier.size(18.dp),
        )
    }
}

