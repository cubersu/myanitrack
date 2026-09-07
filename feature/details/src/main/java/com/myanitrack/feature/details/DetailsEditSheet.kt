package com.myanitrack.feature.details

import androidx.compose.runtime.Composable
import com.myanitrack.core.model.ListStatusUpdate
import com.myanitrack.core.model.MediaListEntry
import com.myanitrack.core.ui.component.ListEntryEditSheet

/**
 * Detay sayfasindaki duzenleme yuzeyi. Liste ekraniyla ayni bileseni kullanir
 * ([ListEntryEditSheet]); boylece iki ekranda alanlar ve davranis birebir ayni kalir.
 */
@Composable
internal fun DetailsEditSheet(
    entry: MediaListEntry,
    onDismiss: () -> Unit,
    onSave: (ListStatusUpdate) -> Unit,
    onDelete: () -> Unit,
) {
    ListEntryEditSheet(
        entry = entry,
        onDismiss = onDismiss,
        onSave = onSave,
        onDelete = onDelete,
    )
}
