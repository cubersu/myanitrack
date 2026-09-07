package com.myanitrack.core.domain.model

/**
 * Baslatilan OAuth2 + PKCE akisinin durumu.
 *
 * [codeVerifier] ve [state] geri donus URL-i dogrulanirken gerekir; bu yuzden
 * tarayici acilmadan once saklanmalidir.
 */
data class AuthRequest(
    val authorizeUrl: String,
    val codeVerifier: String,
    val state: String,
)
