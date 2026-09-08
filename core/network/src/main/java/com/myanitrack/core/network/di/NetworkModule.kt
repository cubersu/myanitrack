package com.myanitrack.core.network.di

import com.myanitrack.core.network.BuildConfig
import com.myanitrack.core.network.auth.MalAuthInterceptor
import com.myanitrack.core.network.auth.MalAuthenticator
import com.myanitrack.core.network.jikan.JikanApiService
import com.myanitrack.core.network.jikan.JikanCircuitBreakerInterceptor
import com.myanitrack.core.network.jikan.JikanRateLimitInterceptor
import com.myanitrack.core.network.jikan.JikanRetryInterceptor
import com.myanitrack.core.network.mal.MalApiService
import com.myanitrack.core.network.mal.MalOAuthService
import com.myanitrack.core.network.rss.MalRssService
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
import retrofit2.converter.scalars.ScalarsConverterFactory

/** Kimlik dogrulama gerektirmeyen (OAuth token alisverisi) istemci. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class UnauthenticatedClient

/** Jikan v4 icin hiz sinirlayici + yeniden deneyici iceren istemci. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class JikanClient

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

    @Provides
    @Singleton
    fun providesJikanCircuitBreakerInterceptor(): JikanCircuitBreakerInterceptor =
        JikanCircuitBreakerInterceptor(clock = System::currentTimeMillis)

    @Provides
    @Singleton
    fun providesJikanRateLimitInterceptor(): JikanRateLimitInterceptor =
        JikanRateLimitInterceptor(
            clock = System::currentTimeMillis,
            sleeper = { millis -> Thread.sleep(millis) },
        )

    @Provides
    @Singleton
    fun providesJikanRetryInterceptor(): JikanRetryInterceptor =
        JikanRetryInterceptor(sleeper = { millis -> Thread.sleep(millis) })

    /**
     * Jikan istemcisi. Katman sirasi onemli:
     *
     * 1. **Devre kesici** (en disda): bir MANTIKSAL cagriyi bir kez degerlendirir.
     *    Yeniden denemelerin disinda oldugu icin tek bir basarisiz istek sayaci
     *    ikiye katlamaz; ayrica devre acikken yeniden deneme hic calismaz.
     * 2. **Yeniden deneme**: gecici hatalarda ustel geri cekilme.
     * 3. **Hiz siniri** (en icte): her DENEME araliklama kuralindan gecer, boylece
     *    backoff sirasinda bile Jikan-in limiti asilmaz.
     */
    @Provides
    @Singleton
    @JikanClient
    fun providesJikanOkHttpClient(
        @UnauthenticatedClient baseClient: OkHttpClient,
        circuitBreakerInterceptor: JikanCircuitBreakerInterceptor,
        retryInterceptor: JikanRetryInterceptor,
        rateLimitInterceptor: JikanRateLimitInterceptor,
    ): OkHttpClient = baseClient.newBuilder()
        .addInterceptor(circuitBreakerInterceptor)
        .addInterceptor(retryInterceptor)
        .addInterceptor(rateLimitInterceptor)
        .build()

    @Provides
    @Singleton
    fun providesJikanApiService(
        @JikanClient client: OkHttpClient,
        json: Json,
    ): JikanApiService = Retrofit.Builder()
        .baseUrl(BuildConfig.JIKAN_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
        .build()
        .create(JikanApiService::class.java)

    /**
     * MAL RSS istemcisi. Govde XML oldugu icin scalars donusturucusu kullaniliyor;
     * kimlik dogrulama gerektirmez, hiz siniri yoktur.
     */
    @Provides
    @Singleton
    fun providesMalRssService(
        @UnauthenticatedClient client: OkHttpClient,
    ): MalRssService = Retrofit.Builder()
        .baseUrl(BuildConfig.MAL_WEB_BASE_URL)
        .client(client)
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
        .create(MalRssService::class.java)

    private const val CONNECT_TIMEOUT_SECONDS = 20L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val JSON_MEDIA_TYPE = "application/json"
}
