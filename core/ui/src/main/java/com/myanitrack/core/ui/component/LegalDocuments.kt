package com.myanitrack.core.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.myanitrack.core.ui.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LegalLinks(modifier: Modifier = Modifier) {
    var document by remember { mutableStateOf<String?>(null) }
    Column(modifier) {
        TextButton(onClick = { document = "privacy" }) { Text(stringResource(R.string.legal_privacy)) }
        TextButton(onClick = { document = "credits" }) { Text(stringResource(R.string.legal_credits)) }
        TextButton(onClick = { document = "licenses" }) { Text(stringResource(R.string.legal_licenses)) }
    }
    document?.let { LegalDocument(it, onClose = { document = null }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegalDocument(document: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val language = context.resources.configuration.locales[0].language
    val text by produceState("", document, language) {
        value = withContext(Dispatchers.IO) {
            val suffix = if (document == "licenses") "" else if (language == "tr") "-tr" else "-en"
            context.assets.open("legal/$document$suffix.txt").bufferedReader().use { it.readText() }
        }
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BackHandler(onBack = onClose)
        Scaffold(topBar = {
            TopAppBar(title = { Text(stringResource(when (document) {
                "privacy" -> R.string.legal_privacy
                "credits" -> R.string.legal_credits
                else -> R.string.legal_licenses
            })) }, navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_cancel)) } })
        }) { padding ->
            SelectionContainer {
                Text(text, modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
