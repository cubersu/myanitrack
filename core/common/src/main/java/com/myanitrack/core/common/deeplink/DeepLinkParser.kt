package com.myanitrack.core.common.deeplink

/**
 * `myanitrack://` ve `https://myanimelist.net/...` adreslerini uygulama ici
 * hedeflere cevirir.
 *
 * Orijinal MALClient `malclient://<mal-link>` bicimini kullaniyordu; burada hem
 * o bicim (semadan sonra MAL yolu) hem de kisa bicim destekleniyor:
 *
 * ```
 * myanitrack://anime/5114
 * myanitrack://manga/2/Berserk
 * myanitrack://profile/Xinil          (user/ da kabul edilir)
 * myanitrack://myanimelist.net/anime/5114/Fullmetal_Alchemist
 * https://myanimelist.net/anime/5114/Fullmetal_Alchemist
 * ```
 *
 * Saf fonksiyon: `android.net.Uri` yerine dize uzerinde calisiyor, boylece
 * birim testi Robolectric olmadan yazilabiliyor.
 */
object DeepLinkParser {

    fun parse(rawUrl: String?): DeepLinkTarget? {
        val url = rawUrl?.trim().orEmpty()
        if (url.isEmpty()) return null

        val segments = url.toPathSegments() ?: return null
        if (segments.isEmpty()) return null

        return when (segments[0].lowercase()) {
            HOST_ANIME -> segments.mediaTarget(MEDIA_TYPE_ANIME)
            HOST_MANGA -> segments.mediaTarget(MEDIA_TYPE_MANGA)
            HOST_PROFILE, HOST_USER -> segments.getOrNull(1)
                ?.takeIf { it.isNotBlank() }
                ?.let { DeepLinkTarget.Profile(it) }

            else -> null
        }
    }

    /**
     * Adresi sema, alan adi ve sorgu parcalarindan arindirip yol parcalarina boler.
     *
     * `myanitrack://myanimelist.net/anime/5114/X` ve `myanitrack://anime/5114`
     * bicimlerinin ikisi de ayni listeye indirgenir; bunun icin bilinen MAL alan
     * adi bas taraftan atilir.
     */
    private fun String.toPathSegments(): List<String>? {
        val withoutScheme = substringAfter("://", missingDelimiterValue = this)
        if (withoutScheme.isBlank()) return null

        val path = withoutScheme.substringBefore('?').substringBefore('#')
        val segments = path.split('/').filter { it.isNotBlank() }.toMutableList()

        // Bilinen alan adlarini at: "myanimelist.net", "www.myanimelist.net".
        segments.firstOrNull()
            ?.lowercase()
            ?.removePrefix("www.")
            ?.takeIf { it == MAL_HOST }
            ?.let { segments.removeAt(0) }

        return segments
    }

    private fun List<String>.mediaTarget(mediaTypeName: String): DeepLinkTarget? {
        val malId = getOrNull(1)?.toIntOrNull() ?: return null
        return DeepLinkTarget.Media(mediaTypeName = mediaTypeName, malId = malId)
    }

    private const val MAL_HOST = "myanimelist.net"
    private const val HOST_ANIME = "anime"
    private const val HOST_MANGA = "manga"
    private const val HOST_PROFILE = "profile"
    private const val HOST_USER = "user"

    /**
     * Tur adlari [com.myanitrack.core.model.MediaType] ile eslesmeli.
     * :core:common model modulune bagimli olmadigi icin dize olarak tutuluyor.
     */
    private const val MEDIA_TYPE_ANIME = "ANIME"
    private const val MEDIA_TYPE_MANGA = "MANGA"
}
