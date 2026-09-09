package com.myanitrack.feature.profile

import com.myanitrack.core.model.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProfileInsightsTest {
    private fun entry(id: Int, score: Int, status: ListStatus, progress: Int = 0) = MediaListEntry(
        MediaNode(id, MediaType.ANIME, "Anime", genres = listOf("Drama", "Drama", "Comedy")),
        MyListStatus(status = status, score = score, numEpisodesWatched = progress),
    )

    @Test
    fun `unrated entries do not skew score distribution`() {
        val entries = listOf(entry(1, 0, ListStatus.WATCHING), entry(2, 8, ListStatus.COMPLETED), entry(3, 8, ListStatus.COMPLETED), entry(4, 10, ListStatus.COMPLETED))
        assertEquals(mapOf(8 to 2, 10 to 1), scoreDistribution(entries))
    }

    @Test
    fun `genre counts exclude untouched plans and count each genre once per title`() {
        val entries = listOf(entry(1, 0, ListStatus.PLAN_TO_WATCH), entry(2, 8, ListStatus.COMPLETED), entry(3, 0, ListStatus.WATCHING, 1))
        assertEquals(listOf("Comedy" to 2, "Drama" to 2), watchedGenres(entries))
    }
}
