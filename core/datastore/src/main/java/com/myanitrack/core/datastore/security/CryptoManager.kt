package com.myanitrack.core.datastore.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OAuth token-larini diske sifreli yazmak icin AndroidKeyStore destekli AES/GCM sarmalayici.
 *
 * Anahtar Keystore icinde uretilir ve hicbir zaman uygulama surecine cikmaz.
 * Bicim: [1 bayt IV uzunlugu][IV][sifreli veri].
 *
 * Not: androidx.security:security-crypto (EncryptedFile/EncryptedSharedPreferences)
 * kullanim disi birakildigi icin dogrudan Keystore API-si tercih edildi.
 */
@Singleton
class CryptoManager @Inject constructor() {

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    private fun secretKey(): SecretKey {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
        }.generateKey()
    }

    fun encryptTo(plain: ByteArray, output: OutputStream) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val encrypted = cipher.doFinal(plain)
        output.write(cipher.iv.size)
        output.write(cipher.iv)
        output.write(encrypted)
        output.flush()
    }

    /** Govde bos ya da bozuksa null doner; cagiran taraf bunu "oturum yok" olarak yorumlar. */
    fun decryptFrom(input: InputStream): ByteArray? {
        val ivSize = input.read()
        if (ivSize <= 0) return null
        val iv = ByteArray(ivSize)
        if (input.read(iv) != ivSize) return null
        val payload = input.readBytes()
        if (payload.isEmpty()) return null
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        }
        return cipher.doFinal(payload)
    }

    /** Anahtari siler; kayitli tum sifreli veri okunamaz hale gelir (cikis yaparken). */
    fun clearKey() {
        runCatching { keyStore.deleteEntry(KEY_ALIAS) }
    }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "myanitrack_auth_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH_BITS = 128
    }
}
