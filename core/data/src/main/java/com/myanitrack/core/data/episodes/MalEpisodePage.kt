package com.myanitrack.core.data.episodes

import org.jsoup.Jsoup
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class MalEpisodePage(val latestReleased: Int?, val lastOffset: Int)

internal fun parseEpisodePage(html: String, now: Instant): MalEpisodePage {
    val doc = Jsoup.parse(html)
    val today = now.atZone(ZoneId.of("Asia/Tokyo")).toLocalDate()
    val latest = doc.select("tr.episode-list-data").mapNotNull { row ->
        val number = row.selectFirst("td.episode-number")?.text()?.toIntOrNull() ?: return@mapNotNull null
        val date = row.selectFirst("td.episode-aired")?.text().orEmpty()
        val aired = runCatching { LocalDate.parse(date, DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)) }.getOrNull()
        // Undated rows may be placeholders; require a date or an actual discussion with replies.
        val replies = row.selectFirst("td.episode-forum")?.attr("data-raw")?.toIntOrNull() ?: 0
        number.takeIf { it > 0 && (aired?.let { day -> !day.isAfter(today) } ?: (replies > 0)) }
    }.maxOrNull()
    val lastOffset = doc.select("a[href*=/episode?offset=]").mapNotNull {
        Regex("[?&]offset=(\\d+)").find(it.attr("href"))?.groupValues?.get(1)?.toIntOrNull()
    }.maxOrNull() ?: 0
    return MalEpisodePage(latest, lastOffset)
}
