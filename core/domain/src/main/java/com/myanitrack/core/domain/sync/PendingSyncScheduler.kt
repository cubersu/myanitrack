package com.myanitrack.core.domain.sync

/**
 * Cevrimdisi yapilan liste degisikliklerinin MAL-e gonderilmesini tetikler.
 *
 * Somut uygulama WorkManager kullanir ve :app modulunde yasar; veri katmani
 * yalnizca bu arayuzu gorur. Boylece repository "ag geri geldiginde gonder"
 * isini planlayabilir ama WorkManager-a bagimli olmaz.
 */
interface PendingSyncScheduler {

    /** Ag baglantisi geri geldiginde bekleyen degisiklikleri gonder. */
    fun scheduleSync()
}
