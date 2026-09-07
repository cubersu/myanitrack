package com.myanitrack.core.network.jikan.mapper

import com.myanitrack.core.model.AiringStatus
import com.myanitrack.core.model.MediaSubType
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.SeasonName
import com.myanitrack.core.network.jikan.dto.JikanCharacterEntryDto
import com.myanitrack.core.network.jikan.dto.JikanDateRangeDto
import com.myanitrack.core.network.jikan.dto.JikanImageDto
import com.myanitrack.core.network.jikan.dto.JikanImageSetDto
import com.myanitrack.core.network.jikan.dto.JikanMediaDto
import com.myanitrack.core.network.jikan.dto.JikanNamedDto
import com.myanitrack.core.network.jikan.dto.JikanPersonRefDto
import com.myanitrack.core.network.jikan.dto.JikanPromoDto
import com.myanitrack.core.network.jikan.dto.JikanRelationDto
import com.myanitrack.core.network.jikan.dto.JikanReviewDto
import com.myanitrack.core.network.jikan.dto.JikanTrailerDto
import com.myanitrack.core.network.jikan.dto.JikanUserMetaDto
import com.myanitrack.core.network.jikan.dto.JikanVideosDto
import com.myanitrack.core.network.jikan.dto.JikanVoiceActorDto
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class JikanMappersTest {

    @ParameterizedTest
    @CsvSource(
        "2009-04-05T00:00:00+00:00, 2009-04-05",
        "2021-12-31T23:00:00+09:00, 2021-12-31",
    )
    @DisplayName("ISO-8601 zaman damgasi yerel tarihe cevrilir")
    fun `parses iso timestamps`(raw: String, expected: String) {
        assertEquals(LocalDate.parse(expected), raw.toJikanDateOrNull())
    }

    @Test
    @DisplayName("Bos ya da bozuk tarih null doner")
    fun `handles bad dates`() {
        assertNull("".toJikanDateOrNull())
        assertNull("   ".toJikanDateOrNull())
        assertNull("bogus".toJikanDateOrNull())
        assertNull(null.toJikanDateOrNull())
    }

    @ParameterizedTest
    @CsvSource(
        "TV, TV",
        "Movie, MOVIE",
        "Light Novel, LIGHT_NOVEL",
        "One-shot, UNKNOWN",
    )
    @DisplayName("Jikan tur adlari domain alt turlerine eslenir")
    fun `maps sub types`(raw: String, expected: String) {
        val node = JikanMediaDto(malId = 1, type = raw).toNode(MediaType.ANIME)
        assertEquals(MediaSubType.valueOf(expected), node.subType)
    }

    @ParameterizedTest
    @CsvSource(
        "Currently Airing, AIRING",
        "Finished Airing, FINISHED",
        "Not yet aired, NOT_YET_AIRED",
    )
    @DisplayName("Anime yayin durumlari eslenir")
    fun `maps anime status`(raw: String, expected: String) {
        val node = JikanMediaDto(malId = 1, status = raw).toNode(MediaType.ANIME)
        assertEquals(AiringStatus.valueOf(expected), node.airingStatus)
    }

    @ParameterizedTest
    @CsvSource(
        "Publishing, PUBLISHING",
        "Finished, FINISHED_PUBLISHING",
        "On Hiatus, NOT_YET_PUBLISHED",
    )
    @DisplayName("Manga durumlari anime-den farkli eslenir")
    fun `maps manga status`(raw: String, expected: String) {
        val node = JikanMediaDto(malId = 1, status = raw).toNode(MediaType.MANGA)
        assertEquals(AiringStatus.valueOf(expected), node.airingStatus)
    }

    @Test
    @DisplayName("Gorsel webp varsa webp, yoksa jpg secilir")
    fun `prefers webp image`() {
        val withWebp = JikanImageSetDto(
            jpg = JikanImageDto(imageUrl = "a.jpg", largeImageUrl = "a-large.jpg"),
            webp = JikanImageDto(imageUrl = "a.webp", largeImageUrl = "a-large.webp"),
        )
        assertEquals("a-large.webp", withWebp.large)

        val jpgOnly = JikanImageSetDto(jpg = JikanImageDto(imageUrl = "b.jpg"))
        assertEquals("b.jpg", jpgOnly.large)
    }

    @Test
    @DisplayName("Detay alanlari eksiksiz aktarilir")
    fun `maps details`() {
        val dto = JikanMediaDto(
            malId = 5114,
            title = "Fullmetal Alchemist: Brotherhood",
            titleEnglish = "FMA: Brotherhood",
            type = "TV",
            status = "Finished Airing",
            episodes = 64,
            aired = JikanDateRangeDto(
                from = "2009-04-05T00:00:00+00:00",
                to = "2010-07-04T00:00:00+00:00",
            ),
            score = 9.1,
            scoredBy = 2_000_000,
            rank = 1,
            season = "spring",
            year = 2009,
            genres = listOf(JikanNamedDto(malId = 1, name = "Action")),
            studios = listOf(JikanNamedDto(malId = 4, name = "Bones")),
            trailer = JikanTrailerDto(youtubeId = "abc123"),
        )

        val details = dto.toDetails(MediaType.ANIME)

        assertEquals("FMA: Brotherhood", details.englishTitle)
        assertEquals(64, details.totalUnits)
        assertEquals(LocalDate.of(2009, 4, 5), details.startDate)
        assertEquals(LocalDate.of(2010, 7, 4), details.endDate)
        assertEquals(SeasonName.SPRING, details.season?.name)
        assertEquals(2009, details.season?.year)
        assertEquals(listOf("Action"), details.genres.map { it.name })
        assertEquals("abc123", details.trailer?.youtubeId)
        assertEquals("https://www.youtube.com/watch?v=abc123", details.trailer?.watchUrl)
        assertEquals("https://myanimelist.net/anime/5114", details.malUrl)
    }

    @Test
    @DisplayName("Sezon adi eksikse sezon bilgisi bos kalir")
    fun `season needs both name and year`() {
        assertNull(JikanMediaDto(malId = 1, year = 2009).toDetails(MediaType.ANIME).season)
        assertNull(JikanMediaDto(malId = 1, season = "spring").toDetails(MediaType.ANIME).season)
    }

    @Test
    @DisplayName("Iliskili yapimlarda anime/manga disi turler atlanir")
    fun `filters non media relations`() {
        val dto = JikanMediaDto(
            malId = 1,
            relations = listOf(
                JikanRelationDto(
                    relation = "Adaptation",
                    entry = listOf(
                        JikanNamedDto(malId = 25, type = "manga", name = "FMA"),
                        JikanNamedDto(malId = 7, type = "person", name = "Someone"),
                    ),
                ),
                JikanRelationDto(
                    relation = "Other",
                    entry = listOf(JikanNamedDto(malId = 9, type = "character", name = "X")),
                ),
            ),
        )

        val related = dto.toDetails(MediaType.ANIME).related

        assertEquals(1, related.size, "Yalnizca gecerli girdisi olan grup kalmali")
        assertEquals("Adaptation", related.first().relation)
        assertEquals(listOf(25), related.first().entries.map { it.id })
        assertEquals(MediaType.MANGA, related.first().entries.first().mediaType)
    }

    @Test
    @DisplayName("Yetiskin turleri dolu ise kayit NSFW isaretlenir")
    fun `explicit genres mark nsfw`() {
        val safe = JikanMediaDto(malId = 1).toNode(MediaType.ANIME)
        val explicit = JikanMediaDto(
            malId = 2,
            explicitGenres = listOf(JikanNamedDto(malId = 12, name = "Hentai")),
        ).toNode(MediaType.ANIME)

        assertFalse(safe.nsfw)
        assertTrue(explicit.nsfw)
    }

    @Test
    @DisplayName("Karakterde Japonca seslendiren tercih edilir")
    fun `prefers japanese voice actor`() {
        val dto = JikanCharacterEntryDto(
            character = JikanPersonRefDto(malId = 12, name = "Alphonse Elric"),
            role = "Main",
            favorites = 5000,
            voiceActors = listOf(
                JikanVoiceActorDto(
                    person = JikanPersonRefDto(name = "Maxey Whitehead"),
                    language = "English",
                ),
                JikanVoiceActorDto(
                    person = JikanPersonRefDto(name = "Kugimiya, Rie"),
                    language = "Japanese",
                ),
            ),
        )

        assertEquals("Kugimiya, Rie", dto.toDomain().voiceActorName)
    }

    @Test
    @DisplayName("Japonca seslendiren yoksa ilk seslendirene duser")
    fun `falls back to first voice actor`() {
        val dto = JikanCharacterEntryDto(
            character = JikanPersonRefDto(malId = 1, name = "X"),
            voiceActors = listOf(
                JikanVoiceActorDto(
                    person = JikanPersonRefDto(name = "Someone"),
                    language = "German",
                ),
            ),
        )
        assertEquals("Someone", dto.toDomain().voiceActorName)
    }

    @Test
    @DisplayName("Seslendiren yoksa alan bos kalir")
    fun `handles missing voice actors`() {
        val dto = JikanCharacterEntryDto(character = JikanPersonRefDto(malId = 1, name = "X"))
        assertNull(dto.toDomain().voiceActorName)
    }

    @Test
    @DisplayName("Review kullanici bilgisi ve tepki sayisiyla eslenir")
    fun `maps review`() {
        val dto = JikanReviewDto(
            malId = 99,
            review = "Great show",
            score = 9,
            isSpoiler = true,
            date = "2020-01-02T00:00:00+00:00",
            user = JikanUserMetaDto(username = "someone"),
            reactions = com.myanitrack.core.network.jikan.dto.JikanReactionsDto(overall = 42),
        )

        val review = dto.toDomain()

        assertEquals("someone", review.userName)
        assertEquals(9, review.score)
        assertTrue(review.isSpoiler)
        assertEquals("2020-01-02", review.dateText)
        assertEquals(42, review.reactionsCount)
    }

    @Test
    @DisplayName("YouTube kimligi olmayan videolar elenir")
    fun `filters videos without youtube id`() {
        val dto = JikanVideosDto(
            promo = listOf(
                JikanPromoDto(title = "PV 1", trailer = JikanTrailerDto(youtubeId = "aaa")),
                JikanPromoDto(title = "PV 2", trailer = JikanTrailerDto(youtubeId = null)),
                JikanPromoDto(title = "PV 3", trailer = null),
            ),
        )

        val videos = dto.toPromoVideos()

        assertEquals(1, videos.size)
        assertEquals("PV 1", videos.first().title)
    }
}
