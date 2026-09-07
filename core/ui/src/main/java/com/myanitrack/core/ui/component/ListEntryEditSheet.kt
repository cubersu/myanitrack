package com.myanitrack.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.myanitrack.core.model.ListStatus
import com.myanitrack.core.model.ListStatusUpdate
import com.myanitrack.core.model.MediaListEntry
import com.myanitrack.core.model.MyListStatus
import com.myanitrack.core.model.scoreLabel
import com.myanitrack.core.ui.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Liste kaydinin tam duzenleme yuzeyi.
 *
 * Kaydederken yalnizca degisen alanlar [ListStatusUpdate] icine konur; boylece
 * MAL-e gereksiz alan gonderilmez ve baska bir istemciden yapilan degisiklikler
 * ezilmez.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListEntryEditSheet(
    entry: MediaListEntry,
    onDismiss: () -> Unit,
    onSave: (ListStatusUpdate) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val original = entry.listStatus
    val isAnime = entry.mediaType.isAnime

    var status by rememberSaveable(entry.id) { mutableStateOf(original.status) }
    var score by rememberSaveable(entry.id) { mutableStateOf(original.score) }
    var progress by rememberSaveable(entry.id) { mutableStateOf(entry.progress.toString()) }
    var volumes by rememberSaveable(entry.id) { mutableStateOf(original.numVolumesRead.toString()) }
    var isRepeating by rememberSaveable(entry.id) { mutableStateOf(original.isRepeating) }
    var timesRepeated by rememberSaveable(entry.id) {
        mutableStateOf(original.numTimesRepeated.toString())
    }
    var tags by rememberSaveable(entry.id) { mutableStateOf(original.tags.joinToString(", ")) }
    var comments by rememberSaveable(entry.id) { mutableStateOf(original.comments) }
    var startDate by remember(entry.id) { mutableStateOf(original.startDate) }
    var finishDate by remember(entry.id) { mutableStateOf(original.finishDate) }
    var datePickerTarget by remember(entry.id) { mutableStateOf<DateTarget?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = entry.node.title, style = MaterialTheme.typography.titleMedium)

            SectionLabel(stringResource(R.string.entry_field_status))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ListStatus.entries.forEach { option ->
                    FilterChip(
                        selected = status == option,
                        onClick = { status = option },
                        label = { Text(statusLabel(option, isAnime)) },
                    )
                }
            }

            SectionLabel(
                stringResource(R.string.entry_field_score) +
                    if (score > 0) "  -  ${score.scoreLabel()}" else "",
            )
            Slider(
                value = score.toFloat(),
                onValueChange = { score = it.toInt() },
                valueRange = MyListStatus.SCORE_NOT_RATED.toFloat()..MyListStatus.SCORE_MAX.toFloat(),
                steps = MyListStatus.SCORE_MAX - 1,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = progress,
                    onValueChange = { progress = it.filter(Char::isDigit) },
                    label = {
                        Text(
                            stringResource(
                                if (isAnime) R.string.entry_field_episodes
                                else R.string.entry_field_chapters,
                            ),
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                if (!isAnime) {
                    OutlinedTextField(
                        value = volumes,
                        onValueChange = { volumes = it.filter(Char::isDigit) },
                        label = { Text(stringResource(R.string.entry_field_volumes)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(
                        if (isAnime) R.string.entry_field_rewatching
                        else R.string.entry_field_rereading,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(checked = isRepeating, onCheckedChange = { isRepeating = it })
            }

            OutlinedTextField(
                value = timesRepeated,
                onValueChange = { timesRepeated = it.filter(Char::isDigit) },
                label = {
                    Text(
                        stringResource(
                            if (isAnime) R.string.entry_field_times_rewatched
                            else R.string.entry_field_times_reread,
                        ),
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            DateRow(
                label = stringResource(R.string.entry_field_start_date),
                date = startDate,
                onPick = { datePickerTarget = DateTarget.START },
                onClear = { startDate = null },
            )
            DateRow(
                label = stringResource(R.string.entry_field_finish_date),
                date = finishDate,
                onPick = { datePickerTarget = DateTarget.FINISH },
                onClear = { finishDate = null },
            )

            HorizontalDivider()

            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text(stringResource(R.string.entry_field_tags)) },
                supportingText = { Text(stringResource(R.string.entry_field_tags_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = comments,
                onValueChange = { comments = it },
                label = { Text(stringResource(R.string.entry_field_comments)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Spacer(Modifier.height(0.dp))
                    Text(
                        text = stringResource(R.string.action_delete),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Button(
                    onClick = {
                        onSave(
                            buildUpdate(
                                original = original,
                                originalProgress = entry.progress,
                                status = status,
                                score = score,
                                progress = progress.toIntOrNull(),
                                volumes = volumes.toIntOrNull(),
                                isAnime = isAnime,
                                isRepeating = isRepeating,
                                timesRepeated = timesRepeated.toIntOrNull(),
                                startDate = startDate,
                                finishDate = finishDate,
                                tags = tags,
                                comments = comments,
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }

    datePickerTarget?.let { target ->
        val initial = if (target == DateTarget.START) startDate else finishDate
        EntryDatePickerDialog(
            initialDate = initial,
            onDismiss = { datePickerTarget = null },
            onConfirm = { picked ->
                if (target == DateTarget.START) startDate = picked else finishDate = picked
                datePickerTarget = null
            },
        )
    }
}

private enum class DateTarget { START, FINISH }

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DateRow(
    label: String,
    date: LocalDate?,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = date?.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    ?: stringResource(R.string.entry_date_not_set),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row {
            if (date != null) {
                TextButton(onClick = onClear) {
                    Text(stringResource(R.string.entry_date_clear))
                }
            }
            TextButton(onClick = onPick) {
                Text(stringResource(R.string.entry_date_pick))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDatePickerDialog(
    initialDate: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            ?.atStartOfDay(ZoneOffset.UTC)
            ?.toInstant()
            ?.toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onConfirm(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                        )
                    }
                },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun statusLabel(status: ListStatus, isAnime: Boolean): String = stringResource(
    when (status) {
        ListStatus.WATCHING ->
            if (isAnime) R.string.status_watching else R.string.status_reading

        ListStatus.COMPLETED -> R.string.status_completed
        ListStatus.ON_HOLD -> R.string.status_on_hold
        ListStatus.DROPPED -> R.string.status_dropped
        ListStatus.PLAN_TO_WATCH ->
            if (isAnime) R.string.status_plan_to_watch else R.string.status_plan_to_read
    },
)

/** Yalnizca gercekten degisen alanlari iceren guncelleme uretir. */
private fun buildUpdate(
    original: MyListStatus,
    originalProgress: Int,
    status: ListStatus,
    score: Int,
    progress: Int?,
    volumes: Int?,
    isAnime: Boolean,
    isRepeating: Boolean,
    timesRepeated: Int?,
    startDate: LocalDate?,
    finishDate: LocalDate?,
    tags: String,
    comments: String,
): ListStatusUpdate {
    val parsedTags = tags.split(",").map(String::trim).filter(String::isNotEmpty)
    return ListStatusUpdate(
        status = status.takeIf { it != original.status },
        score = score.takeIf { it != original.score },
        progress = progress?.takeIf { it != originalProgress },
        volumesRead = volumes?.takeIf { !isAnime && it != original.numVolumesRead },
        isRepeating = isRepeating.takeIf { it != original.isRepeating },
        numTimesRepeated = timesRepeated?.takeIf { it != original.numTimesRepeated },
        startDate = startDate?.takeIf { it != original.startDate },
        finishDate = finishDate?.takeIf { it != original.finishDate },
        clearStartDate = original.startDate != null && startDate == null,
        clearFinishDate = original.finishDate != null && finishDate == null,
        tags = parsedTags.takeIf { it != original.tags },
        comments = comments.takeIf { it != original.comments },
    )
}
