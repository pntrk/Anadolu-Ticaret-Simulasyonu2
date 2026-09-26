package com.example.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

/**
 * BroadcastReceiver for scheduled local Android OS game notifications.
 * Triggers when deliveries arrive, deposits mature, or daily quests reset.
 */
class LocalGameNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: LocalGameNotificationManager.CHANNEL_LOGISTICS
            val title = intent.getStringExtra(EXTRA_TITLE) ?: "Anadolu Ticaret Simülasyonu"
            val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "İşletmenizde yeni gelişmeler var!"
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 1001)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val iconRes = R.mipmap.ic_launcher

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(iconRes)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(notificationId, builder.build())
        } catch (_: Throwable) {
            // Absorb any system notification binder failure safely
        }
    }

    companion object {
        const val EXTRA_CHANNEL_ID = "extra_channel_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
