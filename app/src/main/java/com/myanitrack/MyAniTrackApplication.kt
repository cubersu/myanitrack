package com.myanitrack

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.myanitrack.core.common.di.ApplicationScope
import com.myanitrack.core.domain.repository.UserPreferencesRepository
import com.myanitrack.notification.AiringNotifier
import com.myanitrack.work.AiringSyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okio.Path.Companion.toOkioPath

@HiltAndroidApp
class MyAniTrackApplication :
    Application(),
    SingletonImageLoader.Factory,
    Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var airingNotifier: AiringNotifier

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    /** WorkManager, @HiltWorker isaretli worker-lari bu fabrika ile olusturur. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        airingNotifier.ensureChannel()
        scheduleAiringSync()
    }

    /**
     * Yayin takvimi senkronizasyonunu kurar.
     *
     * Tercih kapaliysa is iptal edilir; boylece kullanici bildirimleri kapattiginda
     * arka planda gereksiz ag trafigi olusmaz.
     */
    private fun scheduleAiringSync() {
        applicationScope.launch {
            val preferences = preferencesRepository.preferences.first()
            val workManager = WorkManager.getInstance(this@MyAniTrackApplication)
            if (preferences.airingNotificationsEnabled) {
                AiringSyncWorker.schedule(workManager, preferences.syncOnlyOnWifi)
            } else {
                AiringSyncWorker.cancel(workManager)
            }
        }
    }

    /**
     * Kapak gorselleri sik tekrar ettigi icin Coil-e acik bir disk onbellegi
     * veriyoruz; liste kaydirmasi cevrimdisi de akici kaliyor.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, MEMORY_CACHE_PERCENT)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(DISK_CACHE_BYTES)
                    .build()
            }
            .crossfade(true)
            .build()

    private companion object {
        const val MEMORY_CACHE_PERCENT = 0.20
        const val DISK_CACHE_BYTES = 256L * 1024 * 1024
    }
}
