package com.myanitrack.core.network.rss

import com.myanitrack.core.model.NewsArticle

/**
 * RSS ogesini haber makalesine cevirir.
 *
 * MAL baglantilari `?_location=rss` izleme parametresiyle geliyor; kimlik ve
 * gorunen adres icin temizleniyor.
 */
fun RssItem.toNewsArticle(): NewsArticle {
    val cleanUrl = link.substringBefore("?_location=")
    return NewsArticle(
        id = cleanUrl.substringAfterLast('/').ifBlank { guid },
        title = title,
        excerpt = description,
        imageUrl = thumbnailUrl,
        publishedAt = publishedAt,
        url = cleanUrl,
    )
}
