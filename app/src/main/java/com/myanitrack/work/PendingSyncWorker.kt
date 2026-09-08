package com.myanitrack.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.domain.repository.MediaListRepository
import com.myanitrack.core.domain.sync.PendingSyncScheduler
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cevrimdisi yapilan liste degisikliklerini MAL-e gonderir.
 *
 * Ag baglantisi kisitiyla kuyruga alinir; cihaz cevrimdisiyken calismaz,
 * baglanti geldiginde sistem tarafindan tetiklenir. Gecici hata durumunda
 * ustel geri cekilmeyle yeniden denenir.
 */
@HiltWorker
class PendingSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val listRepository: MediaListRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = when (listRepository.syncPendingChanges()) {
        is AppResult.Success -> Result.success()
        // Gecici hata: WorkManager backoff ile tekrar deneyecek.
        is AppResult.Failure -> Result.retry()
    }

    companion object {
        const val WORK_NAME = "pending_list_sync"
    }
}

@Singleton
class WorkManagerPendingSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : PendingSyncScheduler {

    override fun scheduleSync() {
        val request = OneTimeWorkRequestBuilder<PendingSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()

        // KEEP: arka arkaya yapilan cevrimdisi duzenlemeler tek is kuyruga alsin.
        // Is zaten tum bekleyen kayitlari birlikte gonderiyor.
        WorkManager.getInstance(context).enqueueUniqueWork(
            PendingSyncWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private companion object {
        const val BACKOFF_SECONDS = 30L
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PendingSyncModule {

    @Binds
    @Singleton
    abstract fun bindsPendingSyncScheduler(
        impl: WorkManagerPendingSyncScheduler,
    ): PendingSyncScheduler
}
