package com.myanitrack.core.network.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Her MAL API istegine `Authorization: Bearer <token>` ekler ve token-in suresi
 * dolmak uzereyse istekten once yeniler.
 *
 * OkHttp interceptor zinciri senkron oldugu icin [runBlocking] kullaniliyor;
 * cagri zaten OkHttp-in kendi is parcaciginda calisiyor.
 */
@Singleton
class MalAuthInterceptor @Inject constructor(
    private val tokenRefresher: MalTokenRefresher,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenRefresher.currentAccessToken() }
        val request = chain.request().newBuilder()
            .apply { if (!token.isNullOrBlank()) header(HEADER_AUTHORIZATION, "Bearer $token") }
            .build()
        return chain.proceed(request)
    }

    internal companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
    }
}
