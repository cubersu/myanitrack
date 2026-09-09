package com.myanitrack.core.data.profile

import com.myanitrack.core.common.di.AppDispatcher
import com.myanitrack.core.common.di.Dispatcher
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.common.result.map
import com.myanitrack.core.data.cache.RemoteCache
import com.myanitrack.core.data.details.requireData
import com.myanitrack.core.datastore.auth.AuthSessionStore
import com.myanitrack.core.domain.repository.ProfileRepository
import com.myanitrack.core.model.FeedUpdate
import com.myanitrack.core.model.Friend
import com.myanitrack.core.model.HistoryEntry
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.UserProfileDetails
import com.myanitrack.core.model.UserMediaStatistics
import com.myanitrack.core.network.mal.MalApiService
import com.myanitrack.core.network.mal.dto.MalUserDto
import com.myanitrack.core.network.jikan.JikanApiService
import com.myanitrack.core.network.jikan.dto.JikanFriendDto
import com.myanitrack.core.network.jikan.dto.JikanHistoryDto
import com.myanitrack.core.network.jikan.dto.JikanUserProfileDto
import com.myanitrack.core.network.jikan.mapper.toDomain
import com.myanitrack.core.network.rss.MalRssService
import com.myanitrack.core.network.rss.RssParser
import com.myanitrack.core.network.rss.toFeedUpdate
import com.myanitrack.core.network.util.safeApiCall
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val jikan: JikanApiService,
    private val mal: MalApiService,
    private val rssService: MalRssService,
    private val rssParser: RssParser,
    private val cache: RemoteCache,
    private val sessionStore: AuthSessionStore,
    @Dispatcher(AppDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : ProfileRepository {

    override val currentUserName: Flow<String?> = sessionStore.session.map { it?.userName }

    override suspend fun getProfile(
        userName: String,
        forceRefresh: Boolean,
    ): AppResult<UserProfileDetails> = withContext(ioDispatcher) {
        val result = cache.cachedCall(
            key = "jikan:profile:${userName.lowercase()}",
            serializer = JikanUserProfileDto.serializer(),
            ttl = PROFILE_TTL,
            forceRefresh = forceRefresh,
        ) {
            safeApiCall { jikan.getUserProfile(userName) }.requireData()
        }.map { it.toDomain() }
        if (!sessionStore.current()?.userName.equals(userName, ignoreCase = true)) {
            return@withContext result
        }
        // Only @me is supported by the official API; never substitute it for another user.
        val official = cache.cachedCall(
            key = "mal:profile:v2:${userName.lowercase()}",
            serializer = MalUserDto.serializer(),
            ttl = PROFILE_TTL,
            forceRefresh = forceRefresh,
        ) { safeApiCall { mal.getMyUser() } }.map { user ->
            UserProfileDetails(
                userName = user.name,
                malId = user.id,
                imageUrl = user.picture,
                gender = user.gender,
                location = user.location,
                birthday = user.birthday?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
                joinedAt = user.joinedAt?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() },
                animeStats = user.animeStatistics?.let { stats ->
                    UserMediaStatistics(
                        mediaType = MediaType.ANIME, daysSpent = stats.numDaysWatched,
                        meanScore = stats.meanScore, inProgress = stats.numItemsWatching,
                        completed = stats.numItemsCompleted, onHold = stats.numItemsOnHold,
                        dropped = stats.numItemsDropped, planned = stats.numItemsPlanToWatch,
                        totalEntries = stats.numItems, repeated = stats.numTimesRewatched,
                        unitsConsumed = stats.numEpisodes,
                    )
                },
            )
        }
        when {
            official is AppResult.Success && result is AppResult.Success -> AppResult.Success(
                result.data.copy(
                    imageUrl = official.data.imageUrl?.takeIf { it.isNotBlank() } ?: result.data.imageUrl,
                    animeStats = official.data.animeStats ?: result.data.animeStats,
                ),
            )
            official is AppResult.Success -> official
            else -> result
        }
    }

    override suspend fun getHistory(
        userName: String,
        mediaType: MediaType?,
        forceRefresh: Boolean,
    ): AppResult<List<HistoryEntry>> = withContext(ioDispatcher) {
        val typeParam = mediaType?.name?.lowercase()
        cache.cachedCall(
            key = "jikan:history:${userName.lowercase()}:${typeParam ?: "all"}",
            serializer = ListSerializer(JikanHistoryDto.serializer()),
            ttl = HISTORY_TTL,
            forceRefresh = forceRefresh,
        ) {
            safeApiCall { jikan.getUserHistory(userName, typeParam) }.requireData()
        }.map { entries ->
            entries.mapNotNull { it.toDomain() }.sortedByDescending { it.date }
        }
    }

    override suspend fun getFriends(
        userName: String,
        forceRefresh: Boolean,
    ): AppResult<List<Friend>> = withContext(ioDispatcher) {
        cache.cachedCall(
            key = "jikan:friends:${userName.lowercase()}",
            serializer = ListSerializer(JikanFriendDto.serializer()),
            ttl = FRIENDS_TTL,
            forceRefresh = forceRefresh,
        ) {
            safeApiCall { jikan.getUserFriends(userName) }.let { result ->
                when (result) {
                    is AppResult.Failure -> result
                    is AppResult.Success -> AppResult.Success(result.data.data)
                }
            }
        }.map { friends ->
            friends.map { it.toDomain() }.sortedByDescending { it.lastOnlineAt }
        }
    }

    /**
     * Arkadas akisi: once arkadas listesi (Jikan), sonra her arkadas icin MAL RSS.
     *
     * RSS istekleri Jikan hiz sinirlayicisindan gecmez (dogrudan MAL-a gider),
     * ama yine de kibar davranmak ve acilisi hizli tutmak icin en son cevrimici
     * olan [MAX_FEED_FRIENDS] arkadasla sinirli ve paralel calisiyor.
     *
     * Tek bir arkadasin beslemesi alinamazsa o arkadas atlanir; akis yayina devam eder.
     */
    override suspend fun getFriendsFeed(
        userName: String,
        forceRefresh: Boolean,
    ): AppResult<List<FeedUpdate>> = withContext(ioDispatcher) {
        val friends = when (val result = getFriends(userName, forceRefresh)) {
            is AppResult.Failure -> return@withContext result
            is AppResult.Success -> result.data
        }
        if (friends.isEmpty()) return@withContext AppResult.Success(emptyList())

        val updates = coroutineScope {
            friends.take(MAX_FEED_FRIENDS)
                .map { friend -> async { fetchUserFeed(friend.userName, forceRefresh) } }
                .flatMap { it.await() }
        }

        AppResult.Success(updates.sortedByDescending { it.publishedAt }.take(MAX_FEED_ITEMS))
    }

    override suspend fun getUserFeed(
        userName: String,
        forceRefresh: Boolean,
    ): AppResult<List<FeedUpdate>> = withContext(ioDispatcher) {
        AppResult.Success(fetchUserFeed(userName, forceRefresh))
    }

    /**
     * Bir kullanicinin anime + manga RSS beslemesi.
     * Hata durumunda bos liste doner; cagiran taraf icin "bu kullanicidan
     * guncelleme yok" ile "besleme alinamadi" ayrimi akis genelinde anlamsiz.
     */
    private suspend fun fetchUserFeed(userName: String, forceRefresh: Boolean): List<FeedUpdate> =
        coroutineScope {
            listOf(FEED_TYPE_ANIME, FEED_TYPE_MANGA)
                .map { type -> async { fetchFeedType(userName, type, forceRefresh) } }
                .flatMap { it.await() }
        }

    private suspend fun fetchFeedType(
        userName: String,
        type: String,
        forceRefresh: Boolean,
    ): List<FeedUpdate> {
        val result = cache.cachedCall(
            key = "mal:rss:$type:${userName.lowercase()}",
            serializer = String.serializer(),
            ttl = FEED_TTL,
            forceRefresh = forceRefresh,
        ) {
            safeApiCall { rssService.getUserFeed(type = type, userName = userName) }
        }
        return when (result) {
            is AppResult.Failure -> emptyList()
            is AppResult.Success -> rssParser.parse(result.data)
                .mapNotNull { it.toFeedUpdate(userName) }
        }
    }

    private companion object {
        val PROFILE_TTL: Duration = Duration.ofHours(6)
        val HISTORY_TTL: Duration = Duration.ofMinutes(30)
        val FRIENDS_TTL: Duration = Duration.ofDays(1)
        val FEED_TTL: Duration = Duration.ofMinutes(30)

        /** MAL RSS tipleri: rw = anime ("read watching"), rm = manga. */
        const val FEED_TYPE_ANIME = "rw"
        const val FEED_TYPE_MANGA = "rm"

        /** Akista kac arkadasin beslemesi cekilecek. */
        const val MAX_FEED_FRIENDS = 15

        /** Akista gosterilecek toplam guncelleme sayisi. */
        const val MAX_FEED_ITEMS = 100
    }
}
