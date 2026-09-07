package com.myanitrack.core.network.auth

import com.myanitrack.core.datastore.auth.AuthSessionStore
import com.myanitrack.core.model.AuthSession
import com.myanitrack.core.network.mal.MalOAuthService
import java.time.Instant
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Access token-in gecerliligini koruyan tek nokta.
 *
 * [mutex] sayesinde es zamanli birden fazla istek 401 alsa bile yenileme
 * yalnizca bir kez yapilir; digerleri yeni token-i bekleyip kullanir.
 */
@Singleton
class MalTokenRefresher @Inject constructor(
    private val oauthServiceProvider: Provider<MalOAuthService>,
    private val sessionStore: AuthSessionStore,
) {

    private val mutex = Mutex()

    /** Gecerli access token; gerekiyorsa sessizce yeniler. Oturum yoksa null. */
    suspend fun currentAccessToken(): String? {
        val session = sessionStore.current() ?: return null
        if (!session.isExpired()) return session.accessToken
        return mutex.withLock { refreshLocked(session.refreshToken)?.accessToken }
    }

    /**
     * 401 sonrasi zorunlu yenileme.
     *
     * [usedToken] istegin basarisiz olan token-i. Kilidi aldigimizda depodaki
     * token degismisse baska bir cagri zaten yenilemis demektir; tekrar
     * yenilemeden yeni token-i doneriz.
     */
    suspend fun refreshAfterUnauthorized(usedToken: String?): String? = mutex.withLock {
        val session = sessionStore.current() ?: return@withLock null
        if (usedToken != null && session.accessToken != usedToken) return@withLock session.accessToken
        refreshLocked(session.refreshToken)?.accessToken
    }

    /** Yenileme de basarisiz olursa oturum temizlenir; kullanici yeniden giris yapar. */
    private suspend fun refreshLocked(refreshToken: String): AuthSession? {
        if (refreshToken.isBlank()) {
            sessionStore.clear()
            return null
        }
        val existingUserName = sessionStore.current()?.userName
        return runCatching {
            oauthServiceProvider.get().refreshToken(
                clientId = MalAuthConfig.clientId,
                refreshToken = refreshToken,
            )
        }.map { token ->
            AuthSession(
                accessToken = token.accessToken,
                refreshToken = token.refreshToken.ifBlank { refreshToken },
                expiresAt = Instant.now().plusSeconds(token.expiresIn),
                userName = existingUserName,
            ).also { sessionStore.save(it) }
        }.getOrElse {
            sessionStore.clear()
            null
        }
    }
}
