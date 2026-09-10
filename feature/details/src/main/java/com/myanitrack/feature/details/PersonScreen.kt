package com.myanitrack.feature.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myanitrack.core.designsystem.component.LoadingState
import com.myanitrack.core.designsystem.component.MediaCover
import com.myanitrack.core.designsystem.component.MessageState
import com.myanitrack.core.ui.toUserMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonScreen(onBack: () -> Unit, viewModel: PersonViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        TopAppBar(title = { Text(state.person?.name ?: stringResource(R.string.person_title)) }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.details_back)) }
        })
    }) { padding ->
        val person = state.person
        when {
            state.loading -> LoadingState(modifier = Modifier.fillMaxSize().padding(padding))
            person == null -> MessageState(
                title = stringResource(R.string.person_unavailable),
                description = state.error?.toUserMessage(),
                actionLabel = stringResource(com.myanitrack.core.ui.R.string.action_retry),
                onAction = viewModel::refresh,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        MediaCover(imageUrl = person.imageUrl, contentDescription = person.name, fallbackText = person.name, modifier = Modifier.width(120.dp).aspectRatio(0.7f))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(person.name, style = MaterialTheme.typography.headlineSmall)
                            person.japaneseName?.let { Text(it) }
                            Text(stringResource(R.string.person_favorites, person.favorites), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                item {
                    SelectionContainer { Text(person.about?.takeIf(String::isNotBlank) ?: stringResource(R.string.person_no_bio), style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }
}
