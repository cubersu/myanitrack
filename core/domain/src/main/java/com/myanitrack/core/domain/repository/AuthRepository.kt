package com.myanitrack.core.domain.repository

import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.domain.model.AuthRequest
import com.myanitrack.core.model.AuthState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    val authState: Flow<AuthState>

    /** local.properties icinde MAL_CLIENT_ID tanimli mi. */
    val isClientConfigured: Boolean

    /** Tarayicida acilacak yetkilendirme istegini uretir. */
    fun createAuthRequest(): AuthRequest

    /**
     * Tarayicidan donen `myanitrack://auth?code=...&state=...` adresini isler,
     * token alir ve oturumu kaydeder.
     */
    suspend fun completeLogin(callbackUri: String, request: AuthRequest): AppResult<Unit>

    suspend fun logout()
}
