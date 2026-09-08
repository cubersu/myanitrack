package com.myanitrack.core.domain.repository

import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.model.ListFilter
import com.myanitrack.core.model.ListStatusUpdate
import com.myanitrack.core.model.MediaListEntry
import com.myanitrack.core.model.MediaType
import kotlinx.coroutines.flow.Flow

/**
 * Kullanicinin anime/manga listesi.
 *
 * Okuma her zaman yerel Room onbelleginden yapilir (cevrimdisi calisir),
 * [refresh] ile MAL API v2-den tazelenir. Yazma islemleri once MAL-e gider,
 * basarili olursa onbellek guncellenir.
 */
interface MediaListRepository {

    fun observeList(mediaType: MediaType, filter: ListFilter): Flow<List<MediaListEntry>>

    fun observeEntry(mediaType: MediaType, malId: Int): Flow<MediaListEntry?>

    /** Filtre menusunde gosterilecek, listede fiilen kullanilan etiketler. */
    fun observeTags(mediaType: MediaType): Flow<List<String>>

    /** Her durum icin kac kayit oldugu (sekme rozetleri). */
    fun observeStatusCounts(mediaType: MediaType): Flow<Map<com.myanitrack.core.model.ListStatus, Int>>

    /**
     * MAL-e gonderilmeyi bekleyen yerel degisiklik sayisi.
     * Cevrimdisi yapilan duzenlemeler burada gorunur.
     */
    fun observePendingSyncCount(): Flow<Int>

    /** Bekleyen degisiklikleri MAL-e gonderir; senkronizasyon isi bunu cagirir. */
    suspend fun syncPendingChanges(): AppResult<Unit>

    suspend fun refresh(mediaType: MediaType): AppResult<Unit>

    suspend fun updateEntry(
        mediaType: MediaType,
        malId: Int,
        update: ListStatusUpdate,
    ): AppResult<MediaListEntry>

    suspend fun deleteEntry(mediaType: MediaType, malId: Int): AppResult<Unit>
}
