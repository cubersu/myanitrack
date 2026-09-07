package com.myanitrack.core.data.news

import com.myanitrack.core.common.di.AppDispatcher
import com.myanitrack.core.common.di.Dispatcher
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.common.result.map
import com.myanitrack.core.data.cache.RemoteCache
import com.myanitrack.core.domain.repository.NewsRepository
import com.myanitrack.core.model.NewsArticle
import com.myanitrack.core.network.rss.MalRssService
import com.myanitrack.core.network.rss.RssParser
import com.myanitrack.core.network.rss.toNewsArticle
import com.myanitrack.core.network.util.safeApiCall
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.serializer

@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val rssService: MalRssService,
    private val rssParser: RssParser,
    private val cache: RemoteCache,
    @Dispatcher(AppDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : NewsRepository {

    /**
     * Ham XML onbellege alinir, ayristirma her okumada yapilir.
     *
     * Neden ham govde: ayristirilmis modeli saklamak icin [NewsArticle]-i
     * `@Serializable` yapmak gerekirdi; XML zaten kucuk (~20 KB) ve ayristirma
     * milisaniyeler suruyor. Ayrica ayristirma mantigi degistiginde onbellek
     * kendiliginden dogru sonuc uretir.
     */
    override suspend fun getNews(forceRefresh: Boolean): AppResult<List<NewsArticle>> =
        withContext(ioDispatcher) {
            cache.cachedCall(
                key = CACHE_KEY,
                serializer = String.serializer(),
                ttl = NEWS_TTL,
                forceRefresh = forceRefresh,
            ) {
                safeApiCall { rssService.getNewsFeed() }
            }.map { xml ->
                rssParser.parse(xml)
                    .map { it.toNewsArticle() }
                    .sortedByDescending { it.publishedAt }
            }
        }

    private companion object {
        const val CACHE_KEY = "mal:rss:news"

        /** Haber akisi gun icinde birkac kez guncelleniyor. */
        val NEWS_TTL: Duration = Duration.ofMinutes(30)
    }
}
