package com.myanitrack.work

import android.content.Context
import androidx.work.WorkManager
import com.myanitrack.core.domain.schedule.AiringSyncScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerAiringSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : AiringSyncScheduler {

    override fun applyPreferences(notificationsEnabled: Boolean, onlyOnWifi: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (notificationsEnabled) {
            // Kisitlar degismis olabilecegi icin isi yeniden kur.
            AiringSyncWorker.cancel(workManager)
            AiringSyncWorker.schedule(workManager, requireUnmeteredNetwork = onlyOnWifi)
        } else {
            AiringSyncWorker.cancel(workManager)
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkModule {

    @Binds
    @Singleton
    abstract fun bindsAiringSyncScheduler(
        impl: WorkManagerAiringSyncScheduler,
    ): AiringSyncScheduler
}
