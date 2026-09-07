package com.myanitrack.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.myanitrack.core.common.di.ApplicationScope
import com.myanitrack.core.common.di.AppDispatcher
import com.myanitrack.core.common.di.Dispatcher
import com.myanitrack.core.datastore.auth.AuthSessionSerializer
import com.myanitrack.core.datastore.auth.StoredAuthSession
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun providesJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun providesPreferencesDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(AppDispatcher.IO) ioDispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope + ioDispatcher,
        produceFile = { context.preferencesDataStoreFile("user_preferences") },
    )

    @Provides
    @Singleton
    fun providesAuthDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(AppDispatcher.IO) ioDispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
        serializer: AuthSessionSerializer,
    ): DataStore<StoredAuthSession> = DataStoreFactory.create(
        serializer = serializer,
        // Sifreli dosya okunamazsa (orn. Keystore anahtari silindi) kullaniciyi
        // cikis yapmis kabul et; cokme yerine yeniden giris istenir.
        corruptionHandler = ReplaceFileCorruptionHandler { StoredAuthSession.EMPTY },
        scope = scope + ioDispatcher,
        produceFile = { context.dataStoreFile("auth_session.pb") },
    )
}

private fun Context.preferencesDataStoreFile(name: String) =
    dataStoreFile("$name.preferences_pb")

private fun Context.dataStoreFile(fileName: String) =
    java.io.File(applicationContext.filesDir, "datastore/$fileName")
