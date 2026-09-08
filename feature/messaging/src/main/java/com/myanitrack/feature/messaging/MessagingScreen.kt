package com.myanitrack.feature.messaging

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.myanitrack.core.ui.component.MalWebViewScreen

/**
 * MAL ozel mesajlari.
 *
 * KIRILGAN MODUL - forum modulüyle ayni gerekce: MAL-in mesajlasma API-si yok.
 * Secenek A (WebView) uygulandi, boylece MAL HTML-i degistiginde bozulmuyor.
 * Mesaj yazma da WebView icinde, MAL-in kendi arayuzuyle yapiliyor.
 */
@Composable
fun MessagingRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MalWebViewScreen(
        title = stringResource(R.string.messaging_title),
        url = MESSAGES_URL,
        onBack = onBack,
        modifier = modifier,
    )
}

private const val MESSAGES_URL = "https://myanimelist.net/mymessages.php"
