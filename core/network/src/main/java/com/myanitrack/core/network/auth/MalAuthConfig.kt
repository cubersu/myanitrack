package com.myanitrack.core.network.auth

import android.util.Base64
import com.myanitrack.core.network.BuildConfig
import java.security.SecureRandom

/**
 * MAL OAuth2 + PKCE yapilandirmasi.
 *
 * Client ID kaynak koda gomulmez; local.properties -> BuildConfig uzerinden gelir.
 * MAL yerel uygulamalar icin client secret istemez.
 */
object MalAuthConfig {

    val clientId: String = BuildConfig.MAL_CLIENT_ID
    val redirectUri: String = BuildConfig.OAUTH_REDIRECT_URI
    val authorizeUrl: String = BuildConfig.MAL_OAUTH_BASE_URL + "authorize"

    val isConfigured: Boolean get() = clientId.isNotBlank()

    /**
     * PKCE dogrulayicisi. MAL yalnizca `plain` yontemini destekledigi icin
     * code_challenge = code_verifier olur; bu yuzden dogrulayici yeterince
     * uzun ve rastgele olmali (RFC 7636: 43-128 karakter).
     */
    fun generateCodeVerifier(): String {
        val bytes = ByteArray(CODE_VERIFIER_BYTES)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
        )
    }

    fun generateState(): String = generateCodeVerifier().take(STATE_LENGTH)

    /** Kullanicinin tarayicida acacagi yetkilendirme adresi. */
    fun buildAuthorizeUrl(codeVerifier: String, state: String): String = buildString {
        append(authorizeUrl)
        append("?response_type=code")
        append("&client_id=").append(clientId)
        append("&code_challenge=").append(codeVerifier)
        append("&code_challenge_method=plain")
        append("&state=").append(state)
        append("&redirect_uri=").append(redirectUri)
    }

    private const val CODE_VERIFIER_BYTES = 64
    private const val STATE_LENGTH = 32
}
