package com.myanitrack.core.data.auth

import android.net.Uri
import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.datastore.auth.AuthSessionStore
import com.myanitrack.core.domain.model.AuthRequest
import com.myanitrack.core.domain.repository.AuthRepository
import com.myanitrack.core.model.AuthSession
import com.myanitrack.core.model.AuthState
import com.myanitrack.core.network.auth.MalAuthConfig
import com.myanitrack.core.network.mal.MalApiService
import com.myanitrack.core.network.mal.MalOAuthService
import com.myanitrack.core.network.util.safeApiCall
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val oauthService: MalOAuthService,
    private val apiService: MalApiService,
    private val sessionStore: AuthSessionStore,
) : AuthRepository {

    override val authState: Flow<AuthState> = sessionStore.session.map { session ->
        if (session == null) AuthState.LoggedOut else AuthState.LoggedIn(session.userName)
    }

    override val isClientConfigured: Boolean get() = MalAuthConfig.isConfigured

    override fun createAuthRequest(): AuthRequest {
        val verifier = MalAuthConfig.generateCodeVerifier()
        val state = MalAuthConfig.generateState()
        return AuthRequest(
            authorizeUrl = MalAuthConfig.buildAuthorizeUrl(verifier, state),
            codeVerifier = verifier,
            state = state,
        )
    }

    override suspend fun completeLogin(
        callbackUri: String,
        request: AuthRequest,
    ): AppResult<Unit> {
        val uri = runCatching { Uri.parse(callbackUri) }.getOrNull()
            ?: return AppResult.Failure(AppError.Unknown())

        uri.getQueryParameter("error")?.let { error ->
            // Kullanici izni reddettiginde MAL error=access_denied ile geri doner.
            return AppResult.Failure(AppError.FeatureUnavailable("oauth:$error"))
        }

        val returnedState = uri.getQueryParameter("state")
        if (returnedState != request.state) {
            // CSRF korumasi: state eslesmiyorsa yaniti kabul etme.
            return AppResult.Failure(AppError.Forbidden)
        }

        val code = uri.getQueryParameter("code")
            ?: return AppResult.Failure(AppError.Unknown())

        return safeApiCall {
            oauthService.exchangeCode(
                clientId = MalAuthConfig.clientId,
                code = code,
                codeVerifier = request.codeVerifier,
                redirectUri = MalAuthConfig.redirectUri,
            )
        }.let { result ->
            when (result) {
                is AppResult.Failure -> result
                is AppResult.Success -> {
                    sessionStore.save(
                        AuthSession(
                            accessToken = result.data.accessToken,
                            refreshToken = result.data.refreshToken,
                            expiresAt = Instant.now().plusSeconds(result.data.expiresIn),
                        ),
                    )
                    // Kullanici adini hemen cekiyoruz; basarisiz olsa bile giris gecerlidir.
                    safeApiCall { apiService.getMyUser() }
                        .let { it as? AppResult.Success }
                        ?.data
                        ?.name
                        ?.let { sessionStore.updateUserName(it) }
                    AppResult.Success(Unit)
                }
            }
        }
    }

    override suspend fun logout() {
        sessionStore.clear()
    }
}
