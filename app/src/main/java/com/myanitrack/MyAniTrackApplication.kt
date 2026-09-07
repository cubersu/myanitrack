package com.myanitrack

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import okio.Path.Companion.toOkioPath

@HiltAndroidApp
class MyAniTrackApplication : Application(), SingletonImageLoader.Factory {

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
