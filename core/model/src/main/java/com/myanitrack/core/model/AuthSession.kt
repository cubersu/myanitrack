package com.myanitrack.core.model

import java.time.Instant

/** Diskte saklanan OAuth2 oturumu. */
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Instant,
    val userName: String? = null,
) {
    fun isExpired(now: Instant = Instant.now(), slack: Long = REFRESH_SLACK_SECONDS): Boolean =
        now.plusSeconds(slack).isAfter(expiresAt)

    companion object {
        /** Token suresi dolmadan 5 dakika once yenilemeye basla. */
        const val REFRESH_SLACK_SECONDS: Long = 300
    }
}

/** Uygulamanin oturum durumu. */
sealed interface AuthState {
    data object Loading : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val userName: String?) : AuthState
}
