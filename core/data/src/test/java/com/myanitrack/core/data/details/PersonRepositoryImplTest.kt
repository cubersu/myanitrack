package com.myanitrack.core.data.details

import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.data.cache.RemoteCache
import com.myanitrack.core.database.dao.RemoteCacheDao
import com.myanitrack.core.network.jikan.JikanApiService
import com.myanitrack.core.network.mal.MalWebService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PersonRepositoryImplTest {
    @Test fun `Jikan failure loads public character page and caches parsed information`() = runTest {
        val api = mockk<JikanApiService>()
        val web = mockk<MalWebService>()
        val dao = mockk<RemoteCacheDao>(relaxed = true)
        coEvery { dao.get(any()) } returns null
        coEvery { api.getCharacterFull(11) } throws IOException("upstream unavailable")
        coEvery { web.getCharacter(11) } returns checkNotNull(javaClass.getResource("/people/character.html")).readText()
        val repository = PersonRepositoryImpl(api, RemoteCache(dao, Json), web, StandardTestDispatcher(testScheduler))
        val result = repository.getPerson(11, true)
        assertTrue(result is AppResult.Success)
        assertEquals("Edward Elric", (result as AppResult.Success).data.name)
        coVerify(exactly = 1) { web.getCharacter(11) }
        coVerify(exactly = 1) { dao.put(match { it.payload.contains("Edward Elric") }) }
    }
}
