package com.myanitrack.core.domain.repository

import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.model.FeedUpdate
import com.myanitrack.core.model.Friend
import com.myanitrack.core.model.HistoryEntry
import com.myanitrack.core.model.MediaType
import com.myanitrack.core.model.UserProfileDetails
import kotlinx.coroutines.flow.Flow

/**
 * Profil, gecmis, arkadaslar ve arkadas akisi.
 *
 * Profil/gecmis/arkadaslar Jikan-dan (MAL API v2 baskasinin profilini vermiyor),
 * arkadas akisi ise MAL-in resmi kullanici RSS beslemesinden gelir.
 */
interface ProfileRepository {

    /** Giris yapmis kullanicinin adi; oturum yoksa null. */
    val currentUserName: Flow<String?>

    suspend fun getProfile(
        userName: String,
        forceRefresh: Boolean = false,
    ): AppResult<UserProfileDetails>

    /** [mediaType] null ise hem anime hem manga gecmisi. */
    suspend fun getHistory(
        userName: String,
        mediaType: MediaType? = null,
        forceRefresh: Boolean = false,
    ): AppResult<List<HistoryEntry>>

    suspend fun getFriends(
        userName: String,
        forceRefresh: Boolean = false,
    ): AppResult<List<Friend>>

    /**
     * Arkadaslarin liste guncellemeleri, tarihe gore birlestirilmis.
     *
     * Her arkadas icin ayri bir RSS istegi gerektigi icin en son cevrimici olan
     * ilk birkac arkadasla sinirlidir; aksi halde 100+ arkadasi olan bir hesapta
     * ekran acilisi dakikalar surerdi.
     */
    suspend fun getFriendsFeed(
        userName: String,
        forceRefresh: Boolean = false,
    ): AppResult<List<FeedUpdate>>

    /** Kullanicinin kendi liste guncellemeleri (RSS). */
    suspend fun getUserFeed(
        userName: String,
        forceRefresh: Boolean = false,
    ): AppResult<List<FeedUpdate>>
}
