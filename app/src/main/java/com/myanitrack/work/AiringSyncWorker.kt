package com.myanitrack.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.domain.repository.ScheduleRepository
import com.myanitrack.core.domain.repository.UserPreferencesRepository
import com.myanitrack.core.domain.schedule.NextEpisodeCalculator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Yaklasan bolumler icin bildirim zamanlar.
 *
 * Calisma bicimi:
 * 1. Kullanicinin izlemekte oldugu animelerin yayin takvimini alir (onbellekli).
 * 2. Bir sonraki calismaya kadar ([SYNC_INTERVAL_HOURS]) yayinlanacak olanlari secer.
 * 3. Her biri icin, yayindan [NOTIFY_BEFORE] once tetiklenecek tek seferlik bir
 *    [AiringNotificationWorker] kuyruga alir.
 *
 * Neden bu tasarim: her bolum icin ayri alarm kurmak (AlarmManager) tam zamanli
 * calisir ama Android 12+ tam alarm izni ister ve pil kisitlarina takilir.
 * WorkManager gecikmeli is + periyodik tazeleme kombinasyonu, birkac dakikalik
 * sapma pahasina izin gerektirmeden ve sistem dostu calisir.
 */
@HiltWorker
class AiringSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val scheduleRepository: ScheduleRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val preferences = preferencesRepository.preferences.first()
        if (!preferences.airingNotificationsEnabled) return Result.success()

        val upcoming = when (val result = scheduleRepository.getUpcomingForMyList()) {
            is AppResult.Success -> result.data
            // Ag/Jikan gecici olarak erisilemiyor; bir sonraki periyotta tekrar denenir.
            is AppResult.Failure -> return Result.retry()
        }

        val now = Instant.now()
        val horizon = Duration.ofHours(SYNC_INTERVAL_HOURS)
        val workManager = WorkManager.getInstance(applicationContext)

        upcoming
            .filter { NextEpisodeCalculator.airsWithin(it.nextAiringAt, now, horizon) }
            .forEach { entry ->
                val airingAt = entry.nextAiringAt ?: return@forEach
                // Yayindan biraz once uyar; yayin cok yakinsa hemen bildir.
                val delay = Duration.between(now, airingAt).minus(NOTIFY_BEFORE)
                    .coerceAtLeast(Duration.ZERO)

                val request = OneTimeWorkRequestBuilder<AiringNotificationWorker>()
                    .setInitialDelay(delay.toMinutes(), TimeUnit.MINUTES)
                    .setInputData(
                        workDataOf(
                            AiringNotificationWorker.KEY_MAL_ID to entry.node.id,
                            AiringNotificationWorker.KEY_TITLE to entry.node.title,
                            AiringNotificationWorker.KEY_AIRING_AT to airingAt.epochSecond,
                        ),
                    )
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()

                // REPLACE: ayni yapim icin zaten kuyrukta is varsa guncel olanla degistir.
                workManager.enqueueUniqueWork(
                    notificationWorkName(entry.node.id),
                    ExistingWorkPolicy.REPLACE,
                    request,
                )
            }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "airing_sync"

        /** Periyodik tazeleme araligi; ayni zamanda bildirim planlama ufku. */
        const val SYNC_INTERVAL_HOURS = 6L

        /** Bolumden ne kadar once haber verilecegi. */
        val NOTIFY_BEFORE: Duration = Duration.ofMinutes(30)

        fun notificationWorkName(malId: Int) = "airing_notification_$malId"

        /**
         * Periyodik isi kurar. [ExistingPeriodicWorkPolicy.KEEP] sayesinde her
         * acilista zamanlayici sifirlanmaz.
         */
        fun schedule(workManager: WorkManager, requireUnmeteredNetwork: Boolean) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (requireUnmeteredNetwork) NetworkType.UNMETERED else NetworkType.CONNECTED,
                )
                .build()

            val request = PeriodicWorkRequestBuilder<AiringSyncWorker>(
                SYNC_INTERVAL_HOURS,
                TimeUnit.HOURS,
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(workManager: WorkManager) {
            workManager.cancelUniqueWork(WORK_NAME)
        }
    }
}
