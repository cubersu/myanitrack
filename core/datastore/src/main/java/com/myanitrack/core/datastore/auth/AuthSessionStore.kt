package com.myanitrack.core.datastore.auth

import androidx.datastore.core.DataStore
import com.myanitrack.core.datastore.security.CryptoManager
import com.myanitrack.core.model.AuthSession
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** OAuth oturumunun tek okuma/yazma noktasi. */
@Singleton
class AuthSessionStore @Inject constructor(
    private val dataStore: DataStore<StoredAuthSession>,
    private val cryptoManager: CryptoManager,
) {

    val session: Flow<AuthSession?> = dataStore.data
        .catch { cause -> if (cause is IOException) emit(StoredAuthSession.EMPTY) else throw cause }
        .map { it.toDomain() }

    suspend fun current(): AuthSession? = session.first()

    suspend fun save(session: AuthSession) {
        dataStore.updateData {
            StoredAuthSession(
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                expiresAtEpochSeconds = session.expiresAt.epochSecond,
                userName = session.userName,
            )
        }
    }

    suspend fun updateUserName(userName: String) {
        dataStore.updateData { it.copy(userName = userName) }
    }

    suspend fun clear() {
        dataStore.updateData { StoredAuthSession.EMPTY }
        cryptoManager.clearKey()
    }

    private fun StoredAuthSession.toDomain(): AuthSession? = when {
        isEmpty -> null
        else -> AuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = Instant.ofEpochSecond(expiresAtEpochSeconds),
            userName = userName,
        )
    }
}
