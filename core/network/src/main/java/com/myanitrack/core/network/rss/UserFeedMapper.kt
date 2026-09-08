package com.myanitrack.core.network.rss

import com.myanitrack.core.model.FeedUpdate
import com.myanitrack.core.model.MediaType

/**
 * MAL kullanici RSS beslemesini ([MalRssService.getUserFeed]) domain modeline cevirir.
 *
 * Ornek oge:
 * ```
 * <title>One Piece - TV</title>
 * <link>https://myanimelist.net/anime/21/One_Piece</link>
 * <description><![CDATA[Watching - 623 of ? episodes]]></description>
 * ```
 * Yapim kimligi ve turu yalnizca baglantida var, ayri alan yok; bu yuzden adresten
 * ayristiriliyor. Beklenen bicime uymayan oge atlanir (null doner) - besleme
 * bozulursa akis tumden cokmez, sadece o satir eksilir.
 */
fun RssItem.toFeedUpdate(userName: String): FeedUpdate? {
    val (mediaType, malId) = link.parseMalMediaLink() ?: return null
    return FeedUpdate(
        userName = userName,
        malId = malId,
        // Baslik "One Piece - TV" seklinde; sondaki tur etiketini atiyoruz.
        title = title.substringBeforeLast(" - ").ifBlank { title },
        mediaType = mediaType,
        statusText = description.trim(),
        publishedAt = publishedAt,
        url = link,
    )
}

/**
 * `https://myanimelist.net/anime/21/One_Piece` -> (ANIME, 21)
 *
 * Adres bicimi degisirse null doner; cagiran taraf kaydi atlar.
 */
internal fun String.parseMalMediaLink(): Pair<MediaType, Int>? {
    val segments = substringAfter("myanimelist.net/", missingDelimiterValue = "")
        .substringBefore('?')
        .split('/')
        .filter { it.isNotBlank() }
    if (segments.size < 2) return null

    val mediaType = when (segments[0].lowercase()) {
        "anime" -> MediaType.ANIME
        "manga" -> MediaType.MANGA
        else -> return null
    }
    val malId = segments[1].toIntOrNull() ?: return null
    return mediaType to malId
}
