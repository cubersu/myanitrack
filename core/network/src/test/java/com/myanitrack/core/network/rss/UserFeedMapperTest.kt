package com.myanitrack.core.network.rss

import com.myanitrack.core.model.MediaType
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class UserFeedMapperTest {

    private val parser = RssParser()

    /** MAL kullanici besleme ornegi (rss.php?type=rw&u=...). */
    private val userFeed = """
        <?xml version="1.0" encoding="utf-8"?>
        <rss version="2.0"><channel>
          <title>Xinil's Anime from MyAnimeList.net</title>
          <item>
            <title>One Piece - TV</title>
            <link>https://myanimelist.net/anime/21/One_Piece</link>
            <guid>https://myanimelist.net/anime/21/One_Piece</guid>
            <description><![CDATA[Watching - 623 of ? episodes]]></description>
            <pubDate>Mon, 19 Apr 2021 14:44:42 -0700</pubDate>
          </item>
          <item>
            <title>Berserk - Manga</title>
            <link>https://myanimelist.net/manga/2/Berserk</link>
            <guid>https://myanimelist.net/manga/2/Berserk</guid>
            <description><![CDATA[Reading - 380 of ? chapters]]></description>
            <pubDate>Wed, 23 Sep 2020 21:50:42 -0700</pubDate>
          </item>
          <item>
            <title>Bozuk Kayit</title>
            <link>https://myanimelist.net/profile/someone</link>
            <description><![CDATA[Ilgisiz]]></description>
          </item>
        </channel></rss>
    """.trimIndent()

    @ParameterizedTest
    @CsvSource(
        "https://myanimelist.net/anime/21/One_Piece, ANIME, 21",
        "https://myanimelist.net/manga/2/Berserk, MANGA, 2",
        "https://myanimelist.net/anime/5114, ANIME, 5114",
        "https://myanimelist.net/anime/1/x?q=1, ANIME, 1",
    )
    @DisplayName("MAL baglantisindan tur ve kimlik cikarilir")
    fun `parses mal links`(url: String, expectedType: String, expectedId: Int) {
        val parsed = url.parseMalMediaLink()

        assertEquals(MediaType.valueOf(expectedType) to expectedId, parsed)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "https://myanimelist.net/profile/someone",
            "https://myanimelist.net/anime",
            "https://myanimelist.net/anime/notanumber/x",
            "https://example.com/anime/21/x",
            "",
        ],
    )
    @DisplayName("Beklenmeyen baglanti bicimleri null doner")
    fun `rejects unexpected links`(url: String) {
        assertNull(url.parseMalMediaLink())
    }

    @Test
    @DisplayName("Besleme ogeleri guncellemeye cevrilir")
    fun `maps feed updates`() {
        val updates = parser.parse(userFeed).mapNotNull { it.toFeedUpdate("Xinil") }

        assertEquals(2, updates.size, "Baglantisi tanimsiz olan kayit atlanmali")

        val anime = updates.first()
        assertEquals("Xinil", anime.userName)
        assertEquals(21, anime.malId)
        assertEquals(MediaType.ANIME, anime.mediaType)
        assertEquals("One Piece", anime.title, "Sondaki tur etiketi atilmali")
        assertEquals("Watching - 623 of ? episodes", anime.statusText)
        assertEquals(Instant.parse("2021-04-19T21:44:42Z"), anime.publishedAt)

        val manga = updates[1]
        assertEquals(MediaType.MANGA, manga.mediaType)
        assertEquals("Berserk", manga.title)
    }

    @Test
    @DisplayName("Tur etiketi olmayan baslik oldugu gibi kalir")
    fun `keeps title without type suffix`() {
        val item = RssItem(
            guid = "g",
            title = "Monster",
            description = "Completed",
            link = "https://myanimelist.net/anime/19/Monster",
            thumbnailUrl = null,
            publishedAt = null,
        )

        assertEquals("Monster", item.toFeedUpdate("u")?.title)
    }

    @Test
    @DisplayName("Anahtar kullanici, yapim ve zaman birlesiminden uretilir")
    fun `builds stable key`() {
        val updates = parser.parse(userFeed).mapNotNull { it.toFeedUpdate("Xinil") }

        assertEquals("Xinil:21:1618868682", updates.first().key)
    }
}
