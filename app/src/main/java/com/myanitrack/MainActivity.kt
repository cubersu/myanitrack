package com.myanitrack

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myanitrack.core.common.auth.AuthRedirectBus
import com.myanitrack.core.designsystem.theme.MyAniTrackTheme
import com.myanitrack.core.model.AuthState
import com.myanitrack.core.model.ThemeMode
import com.myanitrack.feature.auth.LoginRoute
import com.myanitrack.ui.MyAniTrackApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var authRedirectBus: AuthRedirectBus

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Oturum durumu diskten okunana kadar acilis ekrani kalsin; boylece
        // kullanici giris ekraninin bir an gorunup kaybolmasini yasamaz.
        var uiState: MainUiState = MainUiState()
        splashScreen.setKeepOnScreenCondition { !uiState.isReady }

        handleAuthRedirect(intent)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            uiState = state

            val darkTheme = when (state.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            MyAniTrackTheme(darkTheme = darkTheme, useDynamicColor = state.useDynamicColor) {
                when (state.authState) {
                    AuthState.Loading -> Unit // Acilis ekrani gosteriliyor.
                    AuthState.LoggedOut -> LoginRoute(onLoggedIn = { /* authState akisi ekrani degistirir */ })
                    is AuthState.LoggedIn -> MyAniTrackApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthRedirect(intent)
    }

    /**
     * Tarayicidan donen `myanitrack://auth?code=...` adresini giris ekranina iletir.
     * Ileride eklenecek `myanitrack://anime/<id>` gibi derin baglantilar da burada
     * ayristirilacak.
     */
    private fun handleAuthRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == AUTH_SCHEME && data.host == AUTH_HOST) {
            authRedirectBus.publish(data.toString())
        }
    }

    private companion object {
        const val AUTH_SCHEME = "myanitrack"
        const val AUTH_HOST = "auth"
    }
}
