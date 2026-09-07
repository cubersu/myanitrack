package com.myanitrack.core.datastore.auth

import androidx.datastore.core.Serializer
import com.myanitrack.core.datastore.security.CryptoManager
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Diske yazilan sifreli oturum kaydi. */
@Serializable
data class StoredAuthSession(
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresAtEpochSeconds: Long = 0L,
    val userName: String? = null,
) {
    val isEmpty: Boolean get() = accessToken.isEmpty()

    companion object {
        val EMPTY = StoredAuthSession()
    }
}

/**
 * DataStore serializer-i: JSON-a cevir, [CryptoManager] ile sifrele.
 *
 * Cozumleme hatasinda istisna firlatmak yerine bos oturum donuyoruz; boylece
 * anahtar donduginde ya da dosya bozuldugunda uygulama acilista cokmuyor,
 * kullanici sadece yeniden giris yapiyor.
 */
class AuthSessionSerializer @Inject constructor(
    private val cryptoManager: CryptoManager,
    private val json: Json,
) : Serializer<StoredAuthSession> {

    override val defaultValue: StoredAuthSession = StoredAuthSession.EMPTY

    override suspend fun readFrom(input: InputStream): StoredAuthSession =
        runCatching {
            val decrypted = cryptoManager.decryptFrom(input) ?: return StoredAuthSession.EMPTY
            json.decodeFromString<StoredAuthSession>(decrypted.decodeToString())
        }.getOrDefault(StoredAuthSession.EMPTY)

    override suspend fun writeTo(t: StoredAuthSession, output: OutputStream) {
        cryptoManager.encryptTo(json.encodeToString(t).encodeToByteArray(), output)
    }
}
