package com.myanitrack.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.myanitrack.core.ui.component.MalWebViewScreen

/**
 * Profil yorumlari (okuma ve yazma).
 *
 * KIRILGAN ALAN: MAL-in profil yorumlari icin API-si yok. Native bir arayuz
 * kimlik dogrulanmis scraping gerektirirdi; forum ve mesajlarla ayni gerekceyle
 * WebView (Secenek A) tercih edildi. Yorum yazma da MAL-in kendi formuyla,
 * WebView icinde yapiliyor.
 *
 * Adres kullanici adini degil MAL sayisal kimligini istiyor; bu kimlik Jikan
 * profilinden geliyor. Kimlik yoksa bu ekrana hic gecilmiyor.
 */
@Composable
fun ProfileCommentsRoute(
    malUserId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MalWebViewScreen(
        title = stringResource(R.string.profile_comments_title),
        url = "https://myanimelist.net/comments.php?id=$malUserId",
        onBack = onBack,
        modifier = modifier,
    )
}
