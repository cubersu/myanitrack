package com.myanitrack.core.data.details

import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.common.result.map
import com.myanitrack.core.common.di.AppDispatcher
import com.myanitrack.core.common.di.Dispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.myanitrack.core.network.mal.MalWebService
import com.myanitrack.core.data.cache.RemoteCache
import com.myanitrack.core.domain.repository.PersonRepository
import com.myanitrack.core.model.PersonDetails
import com.myanitrack.core.network.jikan.JikanApiService
import com.myanitrack.core.network.jikan.dto.JikanPersonDto
import com.myanitrack.core.network.util.safeApiCall
import javax.inject.Inject

class PersonRepositoryImpl @Inject constructor(
    private val api: JikanApiService,
    private val cache: RemoteCache,
    private val web: MalWebService,
    @Dispatcher(AppDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : PersonRepository {
    override suspend fun getPerson(id: Int, isCharacter: Boolean) = withContext(ioDispatcher) { cache.cachedCall(
        key = "jikan:person:$isCharacter:$id",
        serializer = JikanPersonDto.serializer(),
        ttl = RemoteCache.STATIC_TTL,
    ) {
        val primary = safeApiCall { if (isCharacter) api.getCharacterFull(id) else api.getPersonFull(id) }.requireData()
        if (primary is AppResult.Success) primary else {
            val fallback = safeApiCall {
                parsePersonPage(if (isCharacter) web.getCharacter(id) else web.getPerson(id), id, isCharacter)
            }
            if (fallback is AppResult.Success) fallback else primary
        }
    }.map { PersonDetails(it.name, it.images?.large, it.about,
        it.nameKanji ?: listOfNotNull(it.familyName, it.givenName).joinToString(" ").takeIf(String::isNotBlank), it.favorites) }
    }
}
