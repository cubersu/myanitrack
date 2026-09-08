package com.myanitrack.core.ui.component

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.myanitrack.core.designsystem.component.MessageState
import com.myanitrack.core.ui.R

/**
 * MAL sitesini uygulama icinde gosteren paylasilan WebView ekrani.
 *
 * ## Neden WebView (Secenek A)
 * Forum ve ozel mesajlasmanin MAL tarafinda API-si yok. Native bir arayuz ancak
 * kimlik dogrulanmis HTML scraping ile yapilabilirdi - orijinal MALClient-in
 * cokme sebebiyle ayni kirilganlik. WebView, MAL sayfa yapisini degistirdiginde
 * bile calismaya devam eder.
 *
 * ## Onemli: oturum devri neden yok
 * Uygulama MAL-a **OAuth2 + PKCE** ile baglaniyor; elimizde `api.myanimelist.net`
 * icin gecerli bir bearer token var, `myanimelist.net` icin bir oturum cerezi YOK.
 * MAL-in API kimlik dogrulamasi ile web oturumu ayri sistemler; birini digerine
 * cevirmenin desteklenen bir yolu yok. Bu yuzden WebView kendi cerez kavanozunu
 * kullanir ve kullanici forum/mesajlar icin MAL-a bir kez de burada giris yapar.
 * Cerezler kalici oldugu icin bu islem tekrarlanmaz.
 *
 * (Orijinal MALClient cerez tabanli giris yaptigi icin cerezleri devredebiliyordu;
 * biz bilincli olarak daha stabil olan resmi OAuth2 akisini sectik.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MalWebViewScreen(
    title: String,
    url: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var progress by remember { mutableStateOf(0) }
    var hasError by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    // Ayri giris gerektigi bilgisi kullanici kapatana kadar gorunur.
    var hintVisible by rememberSaveable { mutableStateOf(true) }

    // Cihazin geri hareketi once WebView gecmisinde ilerlesin; yigin bittiginde
    // ekrandan cikilsin. Aksi halde forumda uc sayfa derine inip geri basmak
    // kullaniciyi dogrudan uygulamaya atardi.
    BackHandler(enabled = canGoBack) { webView?.goBack() }

    DisposableEffect(Unit) {
        onDispose {
            // Cerezleri diske yaz; aksi halde surec olurse oturum kaybolabilir.
            CookieManager.getInstance().flush()
            webView?.destroy()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = { if (canGoBack) webView?.goBack() else onBack() }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.webview_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { webView?.reload() }) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.action_refresh),
                        )
                    }
                    IconButton(
                        onClick = {
                            val current = webView?.url ?: url
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, current.toUri()))
                            }
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = stringResource(R.string.webview_open_in_browser),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (hintVisible) {
                WebLoginHint(onDismiss = { hintVisible = false })
            }

            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (hasError) {
                    MessageState(
                        title = stringResource(R.string.webview_error_title),
                        description = stringResource(R.string.webview_error_description),
                        actionLabel = stringResource(R.string.action_retry),
                        onAction = {
                            hasError = false
                            webView?.reload()
                        },
                    )
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { viewContext ->
                            createMalWebView(
                                context = viewContext,
                                onProgress = { progress = it },
                                onHistoryChanged = { canGoBack = it },
                                onError = { hasError = true },
                            ).also {
                                webView = it
                                it.loadUrl(url)
                            }
                        },
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createMalWebView(
    context: android.content.Context,
    onProgress: (Int) -> Unit,
    onHistoryChanged: (Boolean) -> Unit,
    onError: () -> Unit,
): WebView = WebView(context).apply {
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    )

    with(settings) {
        // MAL sitesi JavaScript olmadan calismiyor.
        javaScriptEnabled = true
        domStorageEnabled = true
        // Giris formu ve sayfa duzeni icin masaustu degil mobil gorunum.
        useWideViewPort = true
        loadWithOverviewMode = true
        builtInZoomControls = true
        displayZoomControls = false
        // Yerel dosya erisimi kapali: WebView yalnizca uzak icerik gostermeli.
        allowFileAccess = false
        allowContentAccess = false
    }

    // Koyu temada sayfayi da karart (destekleyen WebView surumlerinde).
    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
    }

    with(CookieManager.getInstance()) {
        setAcceptCookie(true)
        // MAL girisi baska alan adlarina yonlenebiliyor; ucuncu taraf cerezleri
        // acik olmazsa oturum kurulamiyor.
        setAcceptThirdPartyCookies(this@apply, true)
    }

    webChromeClient = object : android.webkit.WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            onProgress(newProgress)
        }
    }

    webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            onHistoryChanged(view?.canGoBack() == true)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            onHistoryChanged(view?.canGoBack() == true)
            CookieManager.getInstance().flush()
        }

        /**
         * MAL disindaki adresler sistem tarayicisinda acilir; kullanici
         * uygulamanin icinde rastgele sitelerde dolasmaz.
         */
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?,
        ): Boolean {
            val target = request?.url ?: return false
            if (!shouldOpenExternally(target.host)) return false
            return runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, target))
            }.isSuccess
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?,
        ) {
            // Yalnizca ana belgenin hatasi ekrani bozmali; sayfadaki tekil
            // gorsel/istek hatalari yok sayilir.
            if (request?.isForMainFrame == true) onError()
        }
    }
}

/**
 * Verilen alan adi WebView icinde mi kalmali, sistem tarayicisinda mi acilmali.
 *
 * MAL ve alt alan adlari (cdn, www...) iceride kalir; digerleri disari cikar.
 * Ayri ve saf bir fonksiyon oldugu icin birim testi yazilabiliyor - WebView-in
 * kendisi JVM testlerinde calistirilamaz.
 *
 * Dikkat: basit `endsWith` yeterli degil; "notmyanimelist.net" de eslesirdi.
 */
internal fun shouldOpenExternally(host: String?): Boolean {
    val normalized = host?.lowercase()?.removePrefix("www.") ?: return false
    return normalized != MAL_HOST && !normalized.endsWith(".$MAL_HOST")
}

private const val MAL_HOST = "myanimelist.net"

/**
 * Kullaniciya forum/mesajlar icin MAL sitesinde ayrica giris yapmasi gerektigini
 * anlatan serit.
 *
 * Bu, gecici bir eksiklik degil mimari bir sonuc: OAuth2 token-i web oturumu
 * uretmiyor (ayrintili aciklama icin [MalWebViewScreen] belgelendirmesine bak).
 * Bu yuzden mesaj bir hata gibi degil, bilgi olarak sunuluyor.
 */
@Composable
private fun WebLoginHint(onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.webview_sign_in_hint),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.action_cancel),
                )
            }
        }
    }
}
