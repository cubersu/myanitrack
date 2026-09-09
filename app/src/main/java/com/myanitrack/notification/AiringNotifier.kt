package com.myanitrack.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.myanitrack.MainActivity
import com.myanitrack.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Yayin hatirlatmalarini gosteren tek nokta.
 *
 * Push sunucusu yok; bildirimler tamamen cihaz icinde, WorkManager tarafindan
 * zamanlanir (bkz. [com.myanitrack.work.AiringSyncWorker]).
 */
@Singleton
class AiringNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    /**
     * Kanali olusturur. Android 8+ zorunlu kilar; tekrar cagrilmasi zararsizdir,
     * bu yuzden Application-da bir kez cagirmak yeterli.
     */
    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_AIRING,
            context.getString(R.string.notification_channel_airing),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_airing_description)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /** Android 13+ calisma zamani izni verilmediyse bildirim gosterilemez. */
    fun canPostNotifications(): Boolean =
        notificationManager.areNotificationsEnabled() &&
            (android.os.Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    /**
     * Yaklasan bolum bildirimi. [malId] hem bildirim kimligi hem de derin
     * baglanti hedefi olarak kullanilir; ayni yapim icin ikinci bildirim
     * oncekini gunceller, kuyruk birikmez.
     */
    fun notifyUpcomingEpisode(malId: Int, title: String, minutesUntilAiring: Long) {
        if (!canPostNotifications()) return

        val content = when {
            minutesUntilAiring <= 0 ->
                context.getString(R.string.notification_airing_now)

            minutesUntilAiring < 60 ->
                context.getString(R.string.notification_airing_in_minutes, minutesUntilAiring.toInt())

            else ->
                context.getString(
                    R.string.notification_airing_in_hours,
                    (minutesUntilAiring / 60).toInt(),
                )
        }

        val intent = Intent(
            Intent.ACTION_VIEW,
            "myanitrack://anime/$malId".toUri(),
            context,
            MainActivity::class.java,
        )
        val pendingIntent = PendingIntent.getActivity(
            context,
            malId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_AIRING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Izin calisma zamaninda geri alinmis olabilir; kontrolu gectikten sonra
        // bile SecurityException gelebilecegi icin sarmalanmis cagri.
        try {
            notificationManager.notify(malId, notification)
        } catch (_: SecurityException) {
            // Permission can be revoked after the check; skip this reminder.
        }
    }

    companion object {
        const val CHANNEL_AIRING = "airing_reminders"
    }
}
