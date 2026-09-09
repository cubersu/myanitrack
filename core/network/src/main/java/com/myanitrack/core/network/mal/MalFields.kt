package com.myanitrack.core.network.mal

/**
 * MAL API v2 varsayilan olarak sadece id/title/main_picture donuyor; istenen her
 * alan `fields` parametresinde acikca sayilmali. Alan listelerini tek yerde tutuyoruz.
 */
object MalFields {

    private const val NODE_COMMON =
        "id,title,main_picture,alternative_titles,start_date,end_date,synopsis," +
            "mean,rank,popularity,nsfw,genres,media_type,status"

    private const val LIST_STATUS_ANIME =
        "list_status{status,score,num_episodes_watched,is_rewatching,updated_at," +
            "start_date,finish_date,priority,num_times_rewatched,rewatch_value,tags,comments}"

    private const val LIST_STATUS_MANGA =
        "list_status{status,score,num_volumes_read,num_chapters_read,is_rereading,updated_at," +
            "start_date,finish_date,priority,num_times_reread,reread_value,tags,comments}"

    /** /users/@me/animelist icin alan listesi. */
    val ANIME_LIST = "$NODE_COMMON,num_episodes,studios,$LIST_STATUS_ANIME"

    /** /users/@me/mangalist icin alan listesi. */
    val MANGA_LIST = "$NODE_COMMON,num_chapters,num_volumes,$LIST_STATUS_MANGA"

    val USER = "picture,gender,birthday,location,joined_at,anime_statistics"
}
