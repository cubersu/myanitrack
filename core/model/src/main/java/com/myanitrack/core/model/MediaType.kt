package com.myanitrack.core.model

/** Uygulama genelinde anime ile manga ayrimi. */
enum class MediaType {
    ANIME,
    MANGA,
    ;

    val isAnime: Boolean get() = this == ANIME
    val isManga: Boolean get() = this == MANGA
}

/** MAL-in alt tur bilgisi (TV, movie, manga, novel...). Gorunum icin. */
enum class MediaSubType(val apiValue: String) {
    TV("tv"),
    OVA("ova"),
    MOVIE("movie"),
    SPECIAL("special"),
    ONA("ona"),
    MUSIC("music"),
    MANGA("manga"),
    NOVEL("novel"),
    LIGHT_NOVEL("light_novel"),
    ONE_SHOT("one_shot"),
    DOUJINSHI("doujinshi"),
    MANHWA("manhwa"),
    MANHUA("manhua"),
    UNKNOWN("unknown"),
    ;

    companion object {
        fun fromApi(value: String?): MediaSubType =
            entries.firstOrNull { it.apiValue == value } ?: UNKNOWN
    }
}

/** Yayin durumu. */
enum class AiringStatus(val apiValue: String) {
    FINISHED("finished_airing"),
    AIRING("currently_airing"),
    NOT_YET_AIRED("not_yet_aired"),
    FINISHED_PUBLISHING("finished"),
    PUBLISHING("currently_publishing"),
    NOT_YET_PUBLISHED("not_yet_published"),
    UNKNOWN("unknown"),
    ;

    companion object {
        fun fromApi(value: String?): AiringStatus =
            entries.firstOrNull { it.apiValue == value } ?: UNKNOWN
    }
}
