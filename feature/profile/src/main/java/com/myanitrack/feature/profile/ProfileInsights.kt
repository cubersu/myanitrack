package com.myanitrack.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.myanitrack.core.model.MediaListEntry
import com.myanitrack.core.model.ListStatus

internal fun scoreDistribution(entries: List<MediaListEntry>): Map<Int, Int> = entries
    .map { it.listStatus.score }.filter { it in 1..10 }.groupingBy { it }.eachCount()

internal fun watchedGenres(entries: List<MediaListEntry>): List<Pair<String, Int>> = entries
    .filter { it.progress > 0 || it.listStatus.status == ListStatus.COMPLETED }
    .flatMap { it.node.genres.distinct() }.groupingBy { it }.eachCount()
    .toList().sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first }).take(5)

@Composable
internal fun ScoreInsights(entries: List<MediaListEntry>) {
    val scores = remember(entries) { scoreDistribution(entries) }
    InsightCard(stringResource(R.string.profile_score_distribution), stringResource(R.string.profile_score_hint, scores.values.sum(), entries.size - scores.values.sum())) {
        val largest = scores.values.maxOrNull()?.coerceAtLeast(1) ?: 1
        (10 downTo 1).forEach { score ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(score.toString(), Modifier.width(24.dp), style = MaterialTheme.typography.labelMedium)
                LinearProgressIndicator(progress = { (scores[score] ?: 0).toFloat() / largest }, modifier = Modifier.weight(1f).height(6.dp), trackColor = MaterialTheme.colorScheme.surfaceContainerHigh, drawStopIndicator = {})
                Text((scores[score] ?: 0).toString(), Modifier.width(32.dp), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
internal fun GenreInsights(entries: List<MediaListEntry>) {
    val genres = remember(entries) { watchedGenres(entries) }
    InsightCard(stringResource(R.string.profile_genres), stringResource(R.string.profile_genres_hint)) {
        if (genres.isEmpty()) Text(stringResource(R.string.profile_insights_empty), style = MaterialTheme.typography.bodyMedium)
        val largest = genres.firstOrNull()?.second ?: 1
        genres.forEach { (genre, count) ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(localizedGenre(genre), style = MaterialTheme.typography.bodyMedium)
                    Text(count.toString(), style = MaterialTheme.typography.labelLarge)
                }
                LinearProgressIndicator(progress = { count.toFloat() / largest }, modifier = Modifier.fillMaxWidth().height(6.dp), trackColor = MaterialTheme.colorScheme.surfaceContainerHigh, drawStopIndicator = {})
            }
        }
    }
}

@Composable
private fun localizedGenre(genre: String): String = when (genre) {
    "Fantasy" -> stringResource(R.string.genre_fantasy)
    "Action" -> stringResource(R.string.genre_action)
    "Adventure" -> stringResource(R.string.genre_adventure)
    "Comedy" -> stringResource(R.string.genre_comedy)
    "Romance" -> stringResource(R.string.genre_romance)
    "Drama" -> stringResource(R.string.genre_drama)
    "Mystery" -> stringResource(R.string.genre_mystery)
    "Sci-Fi" -> stringResource(R.string.genre_scifi)
    else -> genre
}

@Composable
private fun InsightCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}
