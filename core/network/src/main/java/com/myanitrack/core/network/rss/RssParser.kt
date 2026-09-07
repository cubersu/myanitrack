package com.myanitrack.core.network.rss

import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** RSS `<item>` ogesinin ham hali. */
data class RssItem(
    val guid: String,
    val title: String,
    val description: String,
    val link: String,
    val thumbnailUrl: String?,
    val publishedAt: Instant?,
)

/**
 * Kucuk, bagimliliksiz RSS 2.0 ayristiricisi.
 *
 * `javax.xml.parsers` kullaniliyor cunku hem Android hem duz JVM uzerinde
 * calisiyor - bu sayede ayristirma mantigi Robolectric olmadan birim testi
 * edilebiliyor (Android-in `XmlPullParser`-i JVM testlerinde yok).
 *
 * XXE saldirilarina karsi harici varlik cozumleme kapatildi; besleme MAL-dan
 * gelse de ayristiriciyi guvenli varsayilanlarla calistirmak dogru.
 */
@Singleton
class RssParser @Inject constructor() {

    fun parse(xml: String): List<RssItem> {
        if (xml.isBlank()) return emptyList()

        val document = runCatching {
            documentBuilderFactory()
                .newDocumentBuilder()
                .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
        }.getOrNull() ?: return emptyList()

        val items = document.getElementsByTagName("item")
        return (0 until items.length).mapNotNull { index ->
            (items.item(index) as? Element)?.toRssItem()
        }
    }

    private fun documentBuilderFactory(): DocumentBuilderFactory =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            runCatching {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            }
            isExpandEntityReferences = false
        }

    private fun Element.toRssItem(): RssItem? {
        val link = text("link").ifBlank { text("guid") }
        if (link.isBlank()) return null
        return RssItem(
            guid = text("guid").ifBlank { link },
            title = text("title"),
            description = text("description"),
            link = link,
            // Namespace-siz ayristirdigimiz icin etiket adi "media:thumbnail" olarak gelir.
            thumbnailUrl = text("media:thumbnail").ifBlank { text("thumbnail") }.takeIf { it.isNotBlank() },
            publishedAt = text("pubDate").toRssInstantOrNull(),
        )
    }

    private fun Element.text(tag: String): String =
        getElementsByTagName(tag).item(0)?.textContent?.trim().orEmpty()
}

/**
 * RSS tarihi RFC 1123 bicimindedir: "Mon, 07 Sep 2026 08:43:02 -0700".
 * Bazi beslemeler sayisal ofset yerine "GMT" gibi kisaltma kullaniyor, o yuzden
 * iki bicim de deneniyor. Cozulemeyen tarih null olur; siralama sona duser.
 */
internal fun String.toRssInstantOrNull(): Instant? {
    if (isBlank()) return null
    val formatters = listOf(
        DateTimeFormatter.RFC_1123_DATE_TIME,
        DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.ENGLISH),
    )
    for (formatter in formatters) {
        try {
            return Instant.from(formatter.parse(this))
        } catch (_: DateTimeParseException) {
            // Sonraki bicimi dene.
        }
    }
    return null
}
