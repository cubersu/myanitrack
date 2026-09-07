package com.myanitrack.core.domain.schedule

/**
 * Yayin bildirimleri arka plan isini yoneten soyutlama.
 *
 * Somut uygulama WorkManager kullanir ve :app modulunde yasar; ayarlar ekrani
 * yalnizca bu arayuzu gorur, boylece feature modulu WorkManager-a bagimli olmaz.
 */
interface AiringSyncScheduler {

    /** Tercihe gore periyodik isi kurar ya da iptal eder. */
    fun applyPreferences(notificationsEnabled: Boolean, onlyOnWifi: Boolean)
}
