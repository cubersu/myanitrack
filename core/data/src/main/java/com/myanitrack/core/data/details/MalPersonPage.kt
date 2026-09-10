package com.myanitrack.core.data.details

import com.myanitrack.core.network.jikan.dto.JikanPersonDto
import com.myanitrack.core.network.jikan.dto.JikanImageDto
import com.myanitrack.core.network.jikan.dto.JikanImageSetDto
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

internal fun parsePersonPage(html: String, id: Int, isCharacter: Boolean): JikanPersonDto {
    val doc = Jsoup.parse(html)
    val kind = if (isCharacter) "character" else "people"
    val canonical = doc.selectFirst("link[rel=canonical]")?.attr("href").orEmpty()
    require(Regex("https://myanimelist\\.net/$kind/$id(?:/.*)?").matches(canonical)) { "Unexpected person page" }
    val name = doc.selectFirst("meta[property=og:title]")?.attr("content").orEmpty()
    require(name.isNotBlank() && doc.selectFirst("#content") != null) { "Missing person information" }
    val image = doc.selectFirst("meta[property=og:image]")?.attr("content")
    val header = doc.selectFirst("#content h2.normal_header")
    val biography = if (isCharacter) {
        val fragment = Element("div")
        var node = header?.nextSibling()
        while (node != null) {
            if (node is Element && node.hasClass("normal_header")) break
            // Ignore advertising containers; keep biography text, line breaks and spoilers.
            if (node !is Element || node.tagName() != "div" || node.hasClass("spoiler")) fragment.appendChild(node.clone())
            node = node.nextSibling()
        }
        fragment
    } else doc.selectFirst(".people-informantion-more")?.clone()
    biography?.select("script, style, iframe, button, input")?.remove()
    val favorites = Regex("""Member Favorites:\s*([\d,]+)""").find(doc.selectFirst("#content")!!.text())
        ?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: 0
    return JikanPersonDto(
        name = name,
        images = image?.let { JikanImageSetDto(jpg = JikanImageDto(imageUrl = it)) },
        about = biography?.wholeText()?.trim()?.takeIf(String::isNotBlank),
        nameKanji = header?.selectFirst("small")?.text()?.removeSurrounding("(", ")"),
        favorites = favorites,
    )
}
