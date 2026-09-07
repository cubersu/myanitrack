package com.myanitrack.core.network.mal

import com.myanitrack.core.network.mal.dto.MalTokenDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * MAL OAuth2 token endpoint-i.
 *
 * MAL yerel uygulamalar icin PKCE kullanir ve client secret istemez;
 * `code_challenge_method` olarak SADECE `plain` destekler.
 */
interface MalOAuthService {

    @FormUrlEncoded
    @POST("token")
    suspend fun exchangeCode(
        @Field("client_id") clientId: String,
        @Field("code") code: String,
        @Field("code_verifier") codeVerifier: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("grant_type") grantType: String = "authorization_code",
    ): MalTokenDto

    @FormUrlEncoded
    @POST("token")
    suspend fun refreshToken(
        @Field("client_id") clientId: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token",
    ): MalTokenDto
}
