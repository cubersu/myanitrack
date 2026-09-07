package com.myanitrack.core.domain.repository

import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.model.NewsArticle

/** MAL haber akisi (resmi RSS, salt okunur). */
interface NewsRepository {

    suspend fun getNews(forceRefresh: Boolean = false): AppResult<List<NewsArticle>>
}
