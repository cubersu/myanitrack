package com.myanitrack.core.common.deeplink

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class DeepLinkParserTest {

    @ParameterizedTest
    @CsvSource(
        "myanitrack://anime/5114, ANIME, 5114",
        "myanitrack://manga/2, MANGA, 2",
        "myanitrack://manga/2/Berserk, MANGA, 2",
        "myanitrack://ANIME/5114, ANIME, 5114",
    )
    @DisplayName("Kisa sema bicimi ayristirilir")
    fun `parses short scheme`(url: String, type: String, id: Int) {
        assertEquals(DeepLinkTarget.Media(type, id), DeepLinkParser.parse(url))
    }

    @ParameterizedTest
    @CsvSource(
        "myanitrack://myanimelist.net/anime/5114/Fullmetal_Alchemist, ANIME, 5114",
        "myanitrack://www.myanimelist.net/manga/2/Berserk, MANGA, 2",
        "https://myanimelist.net/anime/21/One_Piece, ANIME, 21",
        "https://www.myanimelist.net/manga/2, MANGA, 2",
    )
    @DisplayName("MAL adresi iceren bicim de ayristirilir (orijinal malclient:// deseni)")
    fun `parses mal link form`(url: String, type: String, id: Int) {
        assertEquals(DeepLinkTarget.Media(type, id), DeepLinkParser.parse(url))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "myanitrack://profile/Xinil",
            "myanitrack://user/Xinil",
            "https://myanimelist.net/profile/Xinil",
        ],
    )
    @DisplayName("Kullanici sayfasi baglantilari ayristirilir")
    fun `parses profile links`(url: String) {
        assertEquals(DeepLinkTarget.Profile("Xinil"), DeepLinkParser.parse(url))
    }

    @Test
    @DisplayName("Sorgu ve fragman parcalari yok sayilir")
    fun `ignores query and fragment`() {
        assertEquals(
            DeepLinkTarget.Media("ANIME", 5114),
            DeepLinkParser.parse("https://myanimelist.net/anime/5114/X?q=1#top"),
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "myanitrack://anime",
            "myanitrack://anime/notanumber",
            "myanitrack://forum/1",
            "https://myanimelist.net/",
            "https://example.com/anime/1",
            "",
            "   ",
        ],
    )
    @DisplayName("Taninmayan ya da eksik baglantilar null doner")
    fun `rejects unsupported links`(url: String) {
        assertNull(DeepLinkParser.parse(url))
    }

    @Test
    @DisplayName("null girdi null doner")
    fun `null input`() {
        assertNull(DeepLinkParser.parse(null))
    }

    @Test
    @DisplayName("Bos kullanici adi kabul edilmez")
    fun `rejects blank user name`() {
        assertNull(DeepLinkParser.parse("myanitrack://profile/"))
    }

    @Test
    @DisplayName("MAL-a benzeyen alan adi yol parcasi olarak ele alinir, hedefe cevrilmez")
    fun `lookalike host is not stripped`() {
        // "notmyanimelist.net" bilinen alan adi degil; ilk parca olarak kaldigi
        // icin "anime/manga/profile" ile eslesmez ve null doner.
        assertNull(DeepLinkParser.parse("https://notmyanimelist.net/anime/5114"))
    }
}
