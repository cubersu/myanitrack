package com.myanitrack.core.data.episodes

import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.data.cache.RemoteCache
import com.myanitrack.core.domain.repository.EpisodeRepository
import com.myanitrack.core.network.mal.MalWebService
import com.myanitrack.core.network.util.safeApiCall
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.serializer

@Singleton
class EpisodeRepositoryImpl @Inject constructor(
    private val web: MalWebService,
    private val cache: RemoteCache,
) : EpisodeRepository {
    private val requests = Mutex()

    override suspend fun getReleasedEpisodeCount(animeId: Int): AppResult<Int> = cache.cachedCall(
        key = "mal:released-episodes:v3:$animeId",
        serializer = Int.serializer(),
        ttl = Duration.ofHours(2),
    ) {
        // Jikan's episode cache can lag months behind MAL. Read the public source directly.
        val result = requests.withLock {
            delay(500)
            safeApiCall {
                val first = parseEpisodePage(web.getEpisodes(animeId), Instant.now())
                if (first.lastOffset > 0) {
                    delay(500)
                    parseEpisodePage(web.getEpisodes(animeId, first.lastOffset), Instant.now()).latestReleased
                } else first.latestReleased
            }
        }
        when (result) {
            is AppResult.Failure -> result
            is AppResult.Success -> result.data?.let { AppResult.Success(it) }
                ?: AppResult.Failure(AppError.FeatureUnavailable("released_episode_count"))
        }
    }
}
