package com.myanitrack.core.ui.component

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * WebView-in kendisi JVM testlerinde calistirilamaz; ama icindeki tek gercek
 * karar (bu adres iceride mi kalsin) saf bir fonksiyona ayrildigi icin
 * dogrudan test edilebiliyor.
 */
class MalWebViewHostTest {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "myanimelist.net",
            "www.myanimelist.net",
            "cdn.myanimelist.net",
            "MyAnimeList.net",
            "WWW.MYANIMELIST.NET",
        ],
    )
    @DisplayName("MAL ve alt alan adlari WebView icinde kalir")
    fun `keeps mal hosts inside`(host: String) {
        assertFalse(shouldOpenExternally(host))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "google.com",
            "twitter.com",
            "youtube.com",
            "api.jikan.moe",
        ],
    )
    @DisplayName("Diger siteler sistem tarayicisinda acilir")
    fun `sends other hosts outside`(host: String) {
        assertTrue(shouldOpenExternally(host))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "notmyanimelist.net",
            "myanimelist.net.evil.com",
            "fakemyanimelist.net",
        ],
    )
    @DisplayName("MAL-a benzeyen ama farkli alan adlari disari cikar")
    fun `rejects lookalike hosts`(host: String) {
        assertTrue(
            shouldOpenExternally(host),
            "$host MAL degil; WebView icinde acilmamali",
        )
    }

    @Test
    @DisplayName("Alan adi yoksa (orn. intent: semasi) iceride birakilir")
    fun `null host stays inside`() {
        // Alan adi olmayan adresleri disari gondermeye calismak anlamsiz;
        // WebView kendi hata sayfasini gosterir.
        assertFalse(shouldOpenExternally(null))
    }
}
