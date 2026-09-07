package com.myanitrack.core.network.rss

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class RssParserTest {

    private val parser = RssParser()

    /** MAL-in gercek haber beslemesinin kisaltilmis hali. */
    private val newsFeed = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
          <channel>
            <title>News - MyAnimeList</title>
            <item>
              <guid>https://myanimelist.net/news/74682926?_location=rss</guid>
              <title>Manga &#039;Kimi wa Yotsuba no Clover&#039; Gets Anime Adaptation</title>
              <description>The official account announced on Tuesday...</description>
              <media:thumbnail>https://cdn.myanimelist.net/s/common/uploaded_files/a.jpeg</media:thumbnail>
              <pubDate>Mon, 07 Sep 2026 08:43:02 -0700</pubDate>
              <link>https://myanimelist.net/news/74682926?_location=rss</link>
            </item>
            <item>
              <guid>https://myanimelist.net/news/74682835?_location=rss</guid>
              <title>Studio KAI Produces Hololive TV Anime</title>
              <description>Production company Kadokawa announced...</description>
              <pubDate>Sun, 06 Sep 2026 10:00:00 -0700</pubDate>
              <link>https://myanimelist.net/news/74682835?_location=rss</link>
            </item>
          </channel>
        </rss>
    """.trimIndent()

    @Test
    @DisplayName("Besleme ogeleri ayristirilir")
    fun `parses items`() {
        val items = parser.parse(newsFeed)

        assertEquals(2, items.size)
        assertEquals("Manga 'Kimi wa Yotsuba no Clover' Gets Anime Adaptation", items[0].title)
        assertEquals(
            "https://cdn.myanimelist.net/s/common/uploaded_files/a.jpeg",
            items[0].thumbnailUrl,
        )
    }

    @Test
    @DisplayName("Gorseli olmayan oge null thumbnail ile gelir")
    fun `handles missing thumbnail`() {
        val items = parser.parse(newsFeed)
        assertNull(items[1].thumbnailUrl)
    }

    @Test
    @DisplayName("RFC 1123 tarihi cozulur")
    fun `parses rfc1123 date`() {
        val items = parser.parse(newsFeed)

        // Mon, 07 Sep 2026 08:43:02 -0700 == 2026-09-07T15:43:02Z
        assertEquals(Instant.parse("2026-09-07T15:43:02Z"), items[0].publishedAt)
    }

    @Test
    @DisplayName("Kisaltmali saat dilimi de cozulur")
    fun `parses named zone date`() {
        assertEquals(
            Instant.parse("2026-09-07T08:43:02Z"),
            "Mon, 07 Sep 2026 08:43:02 GMT".toRssInstantOrNull(),
        )
    }

    @Test
    @DisplayName("Cozulemeyen tarih null doner, ayristirma devam eder")
    fun `bad date does not break parsing`() {
        val feed = newsFeed.replace("Mon, 07 Sep 2026 08:43:02 -0700", "not a date")

        val items = parser.parse(feed)

        assertEquals(2, items.size)
        assertNull(items[0].publishedAt)
    }

    @Test
    @DisplayName("Bozuk XML bos liste doner, istisna firlatmaz")
    fun `malformed xml returns empty`() {
        assertTrue(parser.parse("<rss><channel><item>").isEmpty())
        assertTrue(parser.parse("bu XML degil").isEmpty())
        assertTrue(parser.parse("").isEmpty())
    }

    @Test
    @DisplayName("Baglantisi olmayan oge atlanir")
    fun `skips items without link`() {
        val feed = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"><channel>
              <item><title>Baglantisiz</title></item>
              <item><title>Iyi</title><link>https://myanimelist.net/news/1</link></item>
            </channel></rss>
        """.trimIndent()

        val items = parser.parse(feed)

        assertEquals(1, items.size)
        assertEquals("Iyi", items[0].title)
    }

    @Test
    @DisplayName("Haber makalesine cevirirken izleme parametresi temizlenir")
    fun `maps to news article`() {
        val article = parser.parse(newsFeed).first().toNewsArticle()

        assertEquals("https://myanimelist.net/news/74682926", article.url)
        assertEquals("74682926", article.id)
        assertTrue(article.excerpt.isNotBlank())
    }
}
