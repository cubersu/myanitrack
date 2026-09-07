package com.myanitrack.core.network.di

import com.myanitrack.core.network.BuildConfig
import com.myanitrack.core.network.auth.MalAuthInterceptor
import com.myanitrack.core.network.auth.MalAuthenticator
import com.myanitrack.core.network.mal.MalApiService
import com.myanitrack.core.network.mal.MalOAuthService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Kimlik dogrulama gerektirmeyen (OAuth token alisverisi) istemci. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class UnauthenticatedClient

/** MAL API v2 icin token ekleyen istemci. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class MalAuthenticatedClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun providesLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        // Release derlemesinde govde loglanmaz; token sizmasini onlemek icin
        // Authorization basligi her durumda gizlenir.
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        redactHeader(MalAuthInterceptor.HEADER_AUTHORIZATION)
    }

    @Provides
    @Singleton
    @UnauthenticatedClient
    fun providesBaseOkHttpClient(
        logging: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
        .readTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))
        .writeTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))
        .retryOnConnectionFailure(true)
        .addInterceptor(logging)
        .build()

    @Provides
    @Singleton
    @MalAuthenticatedClient
    fun providesMalOkHttpClient(
        @UnauthenticatedClient baseClient: OkHttpClient,
        authInterceptor: MalAuthInterceptor,
        authenticator: MalAuthenticator,
    ): OkHttpClient = baseClient.newBuilder()
        .addInterceptor(authInterceptor)
        .authenticator(authenticator)
        .build()

    @Provides
    @Singleton
    fun providesMalApiService(
        @MalAuthenticatedClient client: OkHttpClient,
        json: Json,
    ): MalApiService = Retrofit.Builder()
        .baseUrl(BuildConfig.MAL_API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
        .build()
        .create(MalApiService::class.java)

    @Provides
    @Singleton
    fun providesMalOAuthService(
        @UnauthenticatedClient client: OkHttpClient,
        json: Json,
    ): MalOAuthService = Retrofit.Builder()
        .baseUrl(BuildConfig.MAL_OAUTH_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
        .build()
        .create(MalOAuthService::class.java)

    private const val CONNECT_TIMEOUT_SECONDS = 20L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val JSON_MEDIA_TYPE = "application/json"
}
