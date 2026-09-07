package com.myanitrack.core.model

import java.time.Instant

/**
 * MAL haber akisindaki tek makale.
 *
 * Kaynak MAL-in resmi RSS beslemesi (`rss.php?type=news`). Jikan-in yalnizca
 * yapim bazli haber ucu var; genel akis icin resmi RSS hem daha dogru hem de
 * hiz sinirina takilmiyor.
 */
data class NewsArticle(
    val id: String,
    val title: String,
    val excerpt: String,
    val imageUrl: String? = null,
    val publishedAt: Instant? = null,
    val url: String,
)
