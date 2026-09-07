package com.myanitrack.core.model

/**
 * Kullanicinin listesindeki durum.
 *
 * MAL API v2 anime icin "watching/plan_to_watch", manga icin "reading/plan_to_read"
 * degerlerini kullaniyor. Ayni kavrami tek enum ile temsil edip API-ye giderken
 * [apiValue] uzerinden dogru dizeye ceviriyoruz.
 */
enum class ListStatus {
    WATCHING,
    COMPLETED,
    ON_HOLD,
    DROPPED,
    PLAN_TO_WATCH,
    ;

    fun apiValue(mediaType: MediaType): String = when (this) {
        WATCHING -> if (mediaType.isAnime) "watching" else "reading"
        COMPLETED -> "completed"
        ON_HOLD -> "on_hold"
        DROPPED -> "dropped"
        PLAN_TO_WATCH -> if (mediaType.isAnime) "plan_to_watch" else "plan_to_read"
    }

    companion object {
        fun fromApi(value: String?): ListStatus? = when (value) {
            "watching", "reading" -> WATCHING
            "completed" -> COMPLETED
            "on_hold" -> ON_HOLD
            "dropped" -> DROPPED
            "plan_to_watch", "plan_to_read" -> PLAN_TO_WATCH
            else -> null
        }
    }
}
