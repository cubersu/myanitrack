package com.myanitrack.core.domain.repository

import com.myanitrack.core.common.result.AppResult

/** Ayarlardaki "onbellek yonetimi" bolumu. */
interface CacheRepository {

    /** Jikan yanit onbelleginin yaklasik boyutu (bayt). */
    suspend fun approximateSizeBytes(): Long

    /**
     * Onbellegi temizler.
     *
     * Kullanicinin kendi listesi SILINMEZ; o onbellek degil, cevrimdisi
     * calismanin temeli. Yalnizca Jikan yanitlari (detay, karakter, takvim,
     * top listeler) ve haber beslemesi temizlenir.
     */
    suspend fun clear(): AppResult<Unit>
}
