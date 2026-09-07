package com.myanitrack.core.data.cache

import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.database.dao.RemoteCacheDao
import com.myanitrack.core.database.entity.RemoteCacheEntity
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Jikan cagrilari icin cache-then-network sarmalayicisi.
 *
 * Onbellege DOMAIN modeli degil, DTO yazilir: DTO-lar zaten kablo formati ve
 * `@Serializable`; domain modelleri ise java.time turleri iceriyor ve seri hale
 * getirilmek uzere tasarlanmadi.
 *
 * Sira:
 * 1. Onbellekte taze kayit varsa doner - ag-a hic dokunulmaz (3/sn - 60/dk limiti).
 * 2. Yoksa ag cagrilir, sonuc onbellege yazilir.
 * 3. Ag basarisiz olur ama elde BAYAT kayit varsa o dondurulur. Jikan sik sik
 *    504 verdigi icin bu, ekranin bosalmasini engelleyen esas koruma.
 * 4. Ne onbellek ne ag varsa hata dondurulur.
 */
@Singleton
class RemoteCache @Inject constructor(
    private val dao: RemoteCacheDao,
    private val json: Json,
) {

    suspend fun <T> cachedCall(
        key: String,
        serializer: KSerializer<T>,
        ttl: Duration,
        forceRefresh: Boolean = false,
        fetch: suspend () -> AppResult<T>,
    ): AppResult<T> {
        if (!forceRefresh) {
            read(key, serializer, ttl)?.let { return AppResult.Success(it) }
        }

        return when (val fetched = fetch()) {
            is AppResult.Success -> {
                write(key, serializer, fetched.data)
                fetched
            }

            is AppResult.Failure -> read(key, serializer, ttl = null)
                ?.let { AppResult.Success(it) }
                ?: fetched
        }
    }

    /**
     * Onbellekten okur. [ttl] null ise tazelige bakilmaz (ag hatasi sonrasi
     * bayat veriye dusme durumu).
     *
     * Bozuk kayit uygulamayi cokertmemeli: sema degistiginde eski govde
     * cozulemez; o kayit silinir ve cagri ag-a duser.
     */
    private suspend fun <T> read(key: String, serializer: KSerializer<T>, ttl: Duration?): T? {
        val entry = dao.get(key) ?: return null
        if (ttl != null) {
            val ageSeconds = Instant.now().epochSecond - entry.fetchedAtEpochSeconds
            if (ageSeconds > ttl.seconds) return null
        }
        return runCatching { json.decodeFromString(serializer, entry.payload) }
            .onFailure { dao.delete(key) }
            .getOrNull()
    }

    private suspend fun <T> write(key: String, serializer: KSerializer<T>, value: T) {
        // Onbellege yazamamak (disk dolu, sema kilidi vb.) cagriyi bozmamali;
        // veri zaten elde, sadece bir dahaki sefere tekrar ag-a gidilir.
        runCatching {
            dao.put(
                RemoteCacheEntity(
                    cacheKey = key,
                    payload = json.encodeToString(serializer, value),
                    fetchedAtEpochSeconds = Instant.now().epochSecond,
                ),
            )
        }
    }

    suspend fun clear(): AppResult<Unit> = runCatching { dao.clear() }.toUnitResult()

    suspend fun approximateSizeBytes(): Long =
        runCatching { dao.approximateSizeBytes() }.getOrDefault(0L)

    /** Ayarlardaki bakim islemi: suresi cok gecmis kayitlari at. */
    suspend fun evictOlderThan(maxAge: Duration): AppResult<Unit> = runCatching {
        dao.deleteOlderThan(Instant.now().minusSeconds(maxAge.seconds).epochSecond)
    }.toUnitResult()

    private fun Result<Unit>.toUnitResult(): AppResult<Unit> = fold(
        onSuccess = { AppResult.Success(Unit) },
        onFailure = { AppResult.Failure(AppError.Storage(it)) },
    )

    companion object {
        /** Detay verisi nadiren degisir. */
        val DETAILS_TTL: Duration = Duration.ofHours(24)

        /** Karakter ve staff pratikte hic degismez. */
        val STATIC_TTL: Duration = Duration.ofDays(7)

        /** Top ve sezonluk listeler gun icinde degisebilir. */
        val LISTING_TTL: Duration = Duration.ofHours(6)

        /** Tur listesi neredeyse sabit. */
        val GENRES_TTL: Duration = Duration.ofDays(30)
    }
}
