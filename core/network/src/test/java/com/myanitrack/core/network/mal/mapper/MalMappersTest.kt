package com.myanitrack.core.network.mal.mapper

import com.myanitrack.core.model.AiringStatus
import com.myanitrack.core.model.ListStatus
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.network.mal.dto.MalListStatusDto
import com.myanitrack.core.network.mal.dto.MalNamedDto
import com.myanitrack.core.network.mal.dto.MalNodeDto
import com.myanitrack.core.network.mal.dto.MalPictureDto
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class MalMappersTest {

    @ParameterizedTest
    @CsvSource(
        "2021-04-03, 2021-04-03",
        "2021-04, 2021-04-01",
        "2021, 2021-01-01",
    )
    @DisplayName("MAL kismi tarihleri tam tarihe tamamlanir")
    fun `parses partial dates`(raw: String, expected: String) {
        assertEquals(LocalDate.parse(expected), raw.toLocalDateOrNull())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   ", "not-a-date", "2021-13-45"])
    @DisplayName("Cozulemeyen tarihler null doner")
    fun `returns null for unparsable dates`(raw: String) {
        assertNull(raw.toLocalDateOrNull())
    }

    @Test
    @DisplayName("null tarih null kalir")
    fun `null date stays null`() {
        assertNull(null.toLocalDateOrNull())
    }

    @Test
    @DisplayName("Dugum alanlari domain modeline dogru aktarilir")
    fun `maps node fields`() {
        val dto = MalNodeDto(
            id = 5114,
            title = "Fullmetal Alchemist: Brotherhood",
            mainPicture = MalPictureDto(medium = "m.jpg", large = "l.jpg"),
            startDate = "2009-04-05",
            mean = 9.1,
            nsfw = "white",
            genres = listOf(MalNamedDto(1, "Action"), MalNamedDto(2, "Drama")),
            mediaType = "tv",
            status = "finished_airing",
            numEpisodes = 64,
            studios = listOf(MalNamedDto(4, "Bones")),
        )

        val node = dto.toDomain(MediaType.ANIME)

        assertEquals(5114, node.id)
        assertEquals("l.jpg", node.picture.best)
        assertEquals(LocalDate.of(2009, 4, 5), node.startDate)
        assertEquals(AiringStatus.FINISHED, node.airingStatus)
        assertEquals(listOf("Action", "Drama"), node.genres)
        assertEquals(listOf("Bones"), node.studios)
        assertEquals(64, node.totalUnits)
        assertFalse(node.nsfw)
    }

    @ParameterizedTest
    @CsvSource("white, false", "gray, true", "black, true")
    @DisplayName("white disindaki nsfw degerleri yetiskin icerik sayilir")
    fun `maps nsfw flag`(raw: String, expected: Boolean) {
        val node = MalNodeDto(id = 1, nsfw = raw).toDomain(MediaType.ANIME)
        assertEquals(expected, node.nsfw)
    }

    @Test
    @DisplayName("Anime liste durumu anime alanlarini kullanir")
    fun `maps anime list status`() {
        val dto = MalListStatusDto(
            status = "watching",
            score = 8,
            numEpisodesWatched = 12,
            isRewatching = true,
            numTimesRewatched = 2,
            rewatchValue = 4,
            tags = listOf("favorite"),
        )

        val status = dto.toDomain(MediaType.ANIME)!!

        assertEquals(ListStatus.WATCHING, status.status)
        assertEquals(12, status.progress(MediaType.ANIME))
        assertTrue(status.isRepeating)
        assertEquals(2, status.numTimesRepeated)
        assertEquals(4, status.repeatValue)
    }

    @Test
    @DisplayName("Manga liste durumu manga alanlarini kullanir")
    fun `maps manga list status`() {
        val dto = MalListStatusDto(
            status = "reading",
            numChaptersRead = 30,
            numVolumesRead = 3,
            isRereading = true,
            numTimesReread = 1,
            rereadValue = 5,
        )

        val status = dto.toDomain(MediaType.MANGA)!!

        assertEquals(ListStatus.WATCHING, status.status)
        assertEquals(30, status.progress(MediaType.MANGA))
        assertEquals(3, status.numVolumesRead)
        assertTrue(status.isRepeating)
        assertEquals(1, status.numTimesRepeated)
        assertEquals(5, status.repeatValue)
    }

    @Test
    @DisplayName("Tanimsiz durum degeri null doner - kayit listede sayilmaz")
    fun `unknown status maps to null`() {
        assertNull(MalListStatusDto(status = null).toDomain(MediaType.ANIME))
        assertNull(MalListStatusDto(status = "bogus").toDomain(MediaType.ANIME))
    }

    @Test
    @DisplayName("Durum degerleri medya turune gore dogru API dizesine cevrilir")
    fun `status api values differ per media type`() {
        assertEquals("watching", ListStatus.WATCHING.apiValue(MediaType.ANIME))
        assertEquals("reading", ListStatus.WATCHING.apiValue(MediaType.MANGA))
        assertEquals("plan_to_watch", ListStatus.PLAN_TO_WATCH.apiValue(MediaType.ANIME))
        assertEquals("plan_to_read", ListStatus.PLAN_TO_WATCH.apiValue(MediaType.MANGA))
        assertEquals("completed", ListStatus.COMPLETED.apiValue(MediaType.MANGA))
    }

    @Test
    @DisplayName("Tarih MAL formatina cevrilir")
    fun `formats date for mal`() {
        assertEquals("2026-09-07", LocalDate.of(2026, 9, 7).toMalDateOrNull())
        assertNull(null.toMalDateOrNull())
    }
}
