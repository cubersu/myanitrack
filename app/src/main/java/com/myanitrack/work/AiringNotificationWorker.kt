package com.myanitrack.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.myanitrack.core.domain.repository.UserPreferencesRepository
import com.myanitrack.notification.AiringNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.first

/**
 * Tek bir yaklasan bolum icin bildirimi gosterir.
 *
 * [AiringSyncWorker] tarafindan gecikmeli olarak kuyruga alinir. Calistigi anda
 * tercihi yeniden kontrol eder: kullanici arada bildirimleri kapatmis olabilir.
 */
@HiltWorker
class AiringNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val notifier: AiringNotifier,
    private val preferencesRepository: UserPreferencesRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!preferencesRepository.preferences.first().airingNotificationsEnabled) {
            return Result.success()
        }

        val malId = inputData.getInt(KEY_MAL_ID, 0)
        val title = inputData.getString(KEY_TITLE).orEmpty()
        if (malId == 0 || title.isBlank()) return Result.failure()

        val airingAtSeconds = inputData.getLong(KEY_AIRING_AT, 0L)
        val minutesUntilAiring = if (airingAtSeconds > 0L) {
            Duration.between(Instant.now(), Instant.ofEpochSecond(airingAtSeconds)).toMinutes()
        } else {
            0L
        }

        // Is cok gecikmis olabilir (cihaz kapaliydi vb.); bolum coktan yayinlandiysa
        // eski bir hatirlatma gostermenin anlami yok.
        if (minutesUntilAiring < -LATE_TOLERANCE_MINUTES) return Result.success()

        notifier.notifyUpcomingEpisode(
            malId = malId,
            title = title,
            minutesUntilAiring = minutesUntilAiring,
        )
        return Result.success()
    }

    companion object {
        const val KEY_MAL_ID = "mal_id"
        const val KEY_TITLE = "title"
        const val KEY_AIRING_AT = "airing_at"

        /** Yayindan sonra bu kadar dakikaya kadar gec bildirim hala anlamli. */
        const val LATE_TOLERANCE_MINUTES = 60L
    }
}
