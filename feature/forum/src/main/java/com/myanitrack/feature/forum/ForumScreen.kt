package com.myanitrack.feature.forum

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.myanitrack.core.ui.component.MalWebViewScreen

/**
 * MAL forumu.
 *
 * KIRILGAN MODUL - ama kirilganligi bilincli olarak en aza indirildi:
 * MAL-in forum API-si olmadigi icin secenekler (A) WebView ya da (B) kimlik
 * dogrulanmis HTML scraping idi. Secenek A uygulandi; MAL sayfa yapisini
 * degistirdiginde bu modul bozulmaz, cunku sayfayi biz ayristirmiyoruz.
 *
 * Modul yalnizca bir URL biliyor; uygulamanin geri kalaniyla baglantisi yok.
 * Forum tumden kaldirilsa bile derleme etkilenmez.
 */
@Composable
fun ForumRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MalWebViewScreen(
        title = stringResource(R.string.forum_title),
        url = FORUM_URL,
        onBack = onBack,
        modifier = modifier,
    )
}

private const val FORUM_URL = "https://myanimelist.net/forum/"
