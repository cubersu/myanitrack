package com.myanitrack.core.data.cache

import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.database.dao.RemoteCacheDao
import com.myanitrack.core.database.entity.RemoteCacheEntity
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class RemoteCacheTest {

    /** Bellek ici sahte DAO; Room-a gerek kalmadan onbellek mantigini surer. */
    private class FakeCacheDao : RemoteCacheDao {
        val entries = mutableMapOf<String, RemoteCacheEntity>()
        var deletions = 0

        override suspend fun get(key: String): RemoteCacheEntity? = entries[key]

        override suspend fun put(entry: RemoteCacheEntity) {
            entries[entry.cacheKey] = entry
        }

        override suspend fun delete(key: String) {
            deletions++
            entries.remove(key)
        }

        override suspend fun deleteOlderThan(threshold: Long) {
            entries.values.removeIf { it.fetchedAtEpochSeconds < threshold }
        }

        override suspend fun clear() = entries.clear()

        override suspend fun approximateSizeBytes(): Long =
            entries.values.sumOf { it.payload.length.toLong() }
    }

    private val dao = FakeCacheDao()
    private val cache = RemoteCache(dao, Json { ignoreUnknownKeys = true })
    private val ttl: Duration = Duration.ofHours(1)

    private fun seed(key: String, value: String, ageSeconds: Long) {
        dao.entries[key] = RemoteCacheEntity(
            cacheKey = key,
            payload = "\"$value\"",
            fetchedAtEpochSeconds = Instant.now().epochSecond - ageSeconds,
        )
    }

    @Test
    @DisplayName("Taze onbellek varken ag hic cagrilmaz")
    fun `fresh cache short circuits network`() = runTest {
        seed("k", "cached", ageSeconds = 10)
        var networkCalls = 0

        val result = cache.cachedCall("k", String.serializer(), ttl) {
            networkCalls++
            AppResult.Success("fresh")
        }

        assertEquals("cached", (result as AppResult.Success).data)
        assertEquals(0, networkCalls, "Taze onbellek varken ag cagrilmamaliydi")
    }

    @Test
    @DisplayName("Bayat onbellek ag cagrisini tetikler ve sonuc yazilir")
    fun `stale cache triggers network and stores result`() = runTest {
        seed("k", "old", ageSeconds = 7_200)

        val result = cache.cachedCall("k", String.serializer(), ttl) {
            AppResult.Success("fresh")
        }

        assertEquals("fresh", (result as AppResult.Success).data)
        assertEquals("\"fresh\"", dao.entries.getValue("k").payload)
    }

    @Test
    @DisplayName("forceRefresh taze onbellegi de atlar")
    fun `force refresh bypasses fresh cache`() = runTest {
        seed("k", "cached", ageSeconds = 1)

        val result = cache.cachedCall("k", String.serializer(), ttl, forceRefresh = true) {
            AppResult.Success("fresh")
        }

        assertEquals("fresh", (result as AppResult.Success).data)
    }

    @Test
    @DisplayName("Ag hata verirse BAYAT onbellek dondurulur (Jikan 504 senaryosu)")
    fun `falls back to stale cache on network failure`() = runTest {
        seed("k", "old", ageSeconds = 7_200)

        val result = cache.cachedCall("k", String.serializer(), ttl) {
            AppResult.Failure(AppError.Server(504))
        }

        assertEquals("old", (result as AppResult.Success).data)
    }

    @Test
    @DisplayName("Ne onbellek ne ag varsa ag hatasi dondurulur")
    fun `returns failure when nothing is available`() = runTest {
        val result = cache.cachedCall("k", String.serializer(), ttl) {
            AppResult.Failure(AppError.Network())
        }

        assertInstanceOf(AppError.Network::class.java, (result as AppResult.Failure).error)
    }

    @Test
    @DisplayName("Bozuk onbellek kaydi silinir ve ag-a dusulur")
    fun `corrupt entry is evicted`() = runTest {
        dao.entries["k"] = RemoteCacheEntity(
            cacheKey = "k",
            payload = "{ bu gecerli JSON degil",
            fetchedAtEpochSeconds = Instant.now().epochSecond,
        )

        val result = cache.cachedCall("k", String.serializer(), ttl) {
            AppResult.Success("fresh")
        }

        assertEquals("fresh", (result as AppResult.Success).data)
        assertEquals(1, dao.deletions, "Bozuk kayit silinmeliydi")
    }

    @Test
    @DisplayName("Onbellege yazamamak cagriyi bozmaz")
    fun `write failure does not break the call`() = runTest {
        val failingDao = object : RemoteCacheDao by FakeCacheDao() {
            override suspend fun put(entry: RemoteCacheEntity) = throw IllegalStateException("disk")
            override suspend fun get(key: String): RemoteCacheEntity? = null
        }
        val failingCache = RemoteCache(failingDao, Json)

        val result = failingCache.cachedCall("k", String.serializer(), ttl) {
            AppResult.Success("fresh")
        }

        assertEquals("fresh", (result as AppResult.Success).data)
    }

    @Test
    @DisplayName("Suresi gecmis kayitlar temizlenir")
    fun `evicts old entries`() = runTest {
        seed("old", "a", ageSeconds = 100_000)
        seed("new", "b", ageSeconds = 10)

        cache.evictOlderThan(Duration.ofHours(1))

        assertNull(dao.entries["old"])
        assertEquals(1, dao.entries.size)
    }

    @Test
    @DisplayName("Depolama hatasi Storage hatasina cevrilir")
    fun `maps storage failure`() = runTest {
        val failingDao = object : RemoteCacheDao by FakeCacheDao() {
            override suspend fun clear() = throw IllegalStateException("disk")
        }

        val result = RemoteCache(failingDao, Json).clear()

        assertInstanceOf(AppError.Storage::class.java, (result as AppResult.Failure).error)
    }
}
