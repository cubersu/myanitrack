package com.myanitrack.feature.auth

import android.content.ActivityNotFoundException
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myanitrack.core.ui.ObserveAsEvents
import com.myanitrack.core.ui.toUserMessage

@Composable
fun LoginRoute(
    onLoggedIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val toolbarColor = MaterialTheme.colorScheme.surface.toArgb()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is LoginEvent.OpenAuthorizeUrl -> {
                val intent = CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .setDefaultColorSchemeParams(
                        androidx.browser.customtabs.CustomTabColorSchemeParams.Builder()
                            .setToolbarColor(toolbarColor)
                            .build(),
                    )
                    .build()
                try {
                    intent.launchUrl(context, event.url.toUri())
                } catch (_: ActivityNotFoundException) {
                    // Cihazda tarayici yoksa akis baslatilamaz.
                    viewModel.onAuthorizationAbandoned()
                }
            }

            LoginEvent.LoginSucceeded -> onLoggedIn()
        }
    }

    val errorMessage = uiState.error?.toUserMessage()
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.dismissError()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LoginScreen(
            uiState = uiState,
            onSignInClick = viewModel::startLogin,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

@Composable
internal fun LoginScreen(
    uiState: LoginUiState,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp).then(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))

        com.myanitrack.core.ui.component.LegalLinks()

        if (!uiState.isClientConfigured) {
            MissingClientIdCard()
            Spacer(Modifier.height(24.dp))
        }

        if (uiState.isExchangingToken) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.login_completing),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Button(
                onClick = onSignInClick,
                enabled = uiState.isClientConfigured && !uiState.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Login, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.login_action))
            }
        }
    }
}

/**
 * MAL_CLIENT_ID tanimli degilse giris hic denenmemeli; kullaniciya ne yapmasi
 * gerektigi acikca soylenir.
 */
@Composable
private fun MissingClientIdCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.Warning, contentDescription = null)
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.login_missing_client_id),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}
