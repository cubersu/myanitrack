package com.myanitrack

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myanitrack.core.common.auth.AuthRedirectBus
import com.myanitrack.core.common.deeplink.DeepLinkBus
import com.myanitrack.core.common.deeplink.DeepLinkParser
import com.myanitrack.core.common.deeplink.DeepLinkTarget
import com.myanitrack.core.designsystem.theme.MyAniTrackTheme
import com.myanitrack.core.model.AuthState
import com.myanitrack.core.model.ThemeMode
import com.myanitrack.feature.auth.LoginRoute
import com.myanitrack.notification.AiringNotifier
import com.myanitrack.ui.MyAniTrackApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var authRedirectBus: AuthRedirectBus

    @Inject
    lateinit var deepLinkBus: DeepLinkBus

    @Inject
    lateinit var airingNotifier: AiringNotifier

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Oturum durumu diskten okunana kadar acilis ekrani kalsin; boylece
        // kullanici giris ekraninin bir an gorunup kaybolmasini yasamaz.
        var uiState: MainUiState = MainUiState()
        splashScreen.setKeepOnScreenCondition { !uiState.isReady }

        handleIntent(intent)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            uiState = state

            val darkTheme = when (state.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            MyAniTrackTheme(darkTheme = darkTheme, useDynamicColor = state.useDynamicColor) {
                var guest by rememberSaveable { mutableStateOf(getPreferences(MODE_PRIVATE).getBoolean("guest", false)) }
                when (state.authState) {
                    AuthState.Loading -> Unit // Acilis ekrani gosteriliyor.

                    AuthState.LoggedOut ->
                        // Giris tamamlaninca authState akisi ekrani kendiliginden degistirir.
                        if (guest) {
                            MyAniTrackApp(isGuest = true)
                        } else {
                            LoginRoute(onLoggedIn = {}, onContinueAsGuest = {
                                getPreferences(MODE_PRIVATE).edit().putBoolean("guest", true).apply()
                                guest = true
                            })
                        }

                    is AuthState.LoggedIn -> {
                        NotificationPermissionRequest()
                        MyAniTrackApp()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /**
     * Android 13+ bildirim izni. Kullanici reddederse uygulama normal calismaya
     * devam eder; yalnizca yayin hatirlatmalari gosterilmez.
     */
    @androidx.compose.runtime.Composable
    private fun NotificationPermissionRequest() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { /* Sonuc onemli degil: izin yoksa bildirim atlanir. */ }

        LaunchedEffect(Unit) {
            if (!airingNotifier.canPostNotifications()) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * Gelen baglantilari isler.
     *
     * - `myanitrack://auth?code=...` - OAuth donusu (giris ekranina)
     * - `myanitrack://anime/<id>`, `myanitrack://profile/<kullanici>` vb.
     * - `https://myanimelist.net/...` - baska uygulamalardan paylasilan MAL adresleri
     *
     * Ayristirma [DeepLinkParser] icinde; burada yalnizca OAuth ozel durumu var.
     */
    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return

        if (data.scheme == APP_SCHEME && data.host == AUTH_HOST) {
            authRedirectBus.publish(data.toString())
            return
        }

        DeepLinkParser.parse(data.toString())?.let(deepLinkBus::publish)
    }

    private companion object {
        const val APP_SCHEME = "myanitrack"
        const val AUTH_HOST = "auth"
    }
}
