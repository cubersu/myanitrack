package com.myanitrack.core.network.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * 401 alan istekleri token-i yenileyip bir kez tekrar dener.
 *
 * Yenileme basarisiz olursa null doneriz; istek 401 olarak yukari cikar ve
 * repository katmani bunu [com.myanitrack.core.common.result.AppError.Unauthorized]
 * yapip kullaniciyi giris ekranina yonlendirir.
 */
@Singleton
class MalAuthenticator @Inject constructor(
    private val tokenRefresher: MalTokenRefresher,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Sonsuz donguyu engelle: bu istek zaten bir kez yenilenmis olarak denendiyse birak.
        if (response.priorResponseCount() >= MAX_RETRY) return null

        val usedToken = response.request
            .header(MalAuthInterceptor.HEADER_AUTHORIZATION)
            ?.removePrefix("Bearer ")
            ?.trim()

        val newToken = runBlocking { tokenRefresher.refreshAfterUnauthorized(usedToken) }
            ?.takeIf { it.isNotBlank() && it != usedToken }
            ?: return null

        return response.request.newBuilder()
            .header(MalAuthInterceptor.HEADER_AUTHORIZATION, "Bearer $newToken")
            .build()
    }

    private fun Response.priorResponseCount(): Int {
        var count = 0
        var prior = priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_RETRY = 1
    }
}
