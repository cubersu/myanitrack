package com.myanitrack.feature.news

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.myanitrack.core.model.NewsArticle
import com.myanitrack.core.ui.toUserMessage
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NewsRoute(
    modifier: Modifier = Modifier,
    viewModel: NewsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    NewsScreen(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onOpenArticle = { article ->
            // Haber govdesi RSS-te tam gelmiyor; makale MAL sitesinde aciliyor.
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, article.url.toUri()))
            }
        },
        modifier = modifier,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun NewsScreen(
    uiState: NewsUiState,
    onRefresh: () -> Unit,
    onOpenArticle: (NewsArticle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.news_title)) }) },
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

                uiState.articles.isEmpty() -> MessageState(
                    title = stringResource(R.string.news_empty_title),
                    description = uiState.error?.toUserMessage()
                        ?: stringResource(R.string.news_empty_description),
                    icon = Icons.Outlined.Newspaper,
                    actionLabel = stringResource(com.myanitrack.core.ui.R.string.action_refresh),
                    onAction = onRefresh,
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(uiState.articles, key = { it.id }) { article ->
                        NewsRow(article = article, onClick = { onOpenArticle(article) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsRow(article: NewsArticle, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        if (article.imageUrl != null) {
            MediaCover(
                imageUrl = article.imageUrl,
                contentDescription = article.title,
                cornerRadius = 6,
                modifier = Modifier
                    .width(96.dp)
                    .height(72.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (article.imageUrl != null) 12.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = article.excerpt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            article.publishedAt?.let { published ->
                Text(
                    text = published.atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
