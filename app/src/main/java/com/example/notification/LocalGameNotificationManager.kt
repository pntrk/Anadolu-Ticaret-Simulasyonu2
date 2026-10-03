package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Native Local Push Notification Manager.
 * Schedules alarms for delivery vehicle arrivals, bank deposit maturities,
 * borsa price alerts, and daily quest retention reminders.
 */
object LocalGameNotificationManager {

    private const val TAG = "GameNotifications"

    const val CHANNEL_LOGISTICS = "channel_logistics_v1"
    const val CHANNEL_BANKING = "channel_banking_v1"
    const val CHANNEL_QUESTS = "channel_quests_v1"
    const val CHANNEL_MARKET = "channel_market_v1"

    private var appContext: Context? = null
    @Volatile
    private var isAppInForeground: Boolean = true
    @Volatile
    private var lastInstantNotificationTimeMs: Long = 0L

    fun setAppInForeground(inForeground: Boolean) {
        isAppInForeground = inForeground
    }

    /**
     * Initializes all game notification channels for Android 8.0+ (Oreo+).
     */
    fun initializeChannels(context: Context) {
        appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val logisticsChannel = NotificationChannel(
                    CHANNEL_LOGISTICS,
                    "Lojistik & Teslimat",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Sevkiyat araçları hedefe vardığında ve mallar depoya indiğinde bildirim gönderir."
                    enableVibration(false)
                }

                val bankingChannel = NotificationChannel(
                    CHANNEL_BANKING,
                    "Banka & Mevduat",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Vadeli mevduat faiz getirileri ve kredi ödeme hatırlatmaları."
                    enableVibration(false)
                }

                val questsChannel = NotificationChannel(
                    CHANNEL_QUESTS,
                    "Günlük Görevler & Ödüller",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Günlük görev yenilenmeleri ve sezon pasosu ödül bildirimleri."
                    enableVibration(false)
                }

                val marketChannel = NotificationChannel(
                    CHANNEL_MARKET,
                    "Borsa & Piyasa Fırsatları",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Borsa krizleri, ralli fırsatları ve müzayede teklif durumları."
                    enableVibration(false)
                }

                notificationManager.createNotificationChannels(
                    listOf(logisticsChannel, bankingChannel, questsChannel, marketChannel)
                )
                Log.d(TAG, "Notification channels initialized successfully.")
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to initialize notification channels safely", e)
            }
        }
    }

    /**
     * Schedules a local notification when a logistics shipment reaches its destination city.
     */
    fun scheduleDeliveryArrival(
        itemId: String,
        itemDisplayName: String,
        quantity: Int,
        targetCityName: String,
        targetDeliveryTimeMs: Long,
        deliveryId: Int = (1000..9999).random()
    ) {
        val ctx = appContext ?: return
        val now = System.currentTimeMillis()
        if (targetDeliveryTimeMs <= now) return

        val title = "🚚 Lojistik Teslimatı Tamamlandı!"
        val message = "$targetCityName merkez depoya $quantity Ton $itemDisplayName sevkiyatınız güvenle ulaştı."
        
        scheduleAlarmNotification(
            context = ctx,
            channelId = CHANNEL_LOGISTICS,
            title = title,
            message = message,
            triggerAtMs = targetDeliveryTimeMs,
            requestCode = 10000 + deliveryId
        )
    }

    /**
     * Schedules a notification for when a locked bank term deposit matures with its interest.
     */
    fun scheduleDepositMaturity(
        totalReturnAmount: Long,
        maturityTimeMs: Long
    ) {
        val ctx = appContext ?: return
        val now = System.currentTimeMillis()
        if (maturityTimeMs <= now) return

        val title = "🏦 Vadeli Mevduat Hesabınız Doldu!"
        val message = "Vadeli mevduatınız faiz getirisiyle birlikte hesabınıza aktarılmaya hazır."

        scheduleAlarmNotification(
            context = ctx,
            channelId = CHANNEL_BANKING,
            title = title,
            message = message,
            triggerAtMs = maturityTimeMs,
            requestCode = 20001
        )
    }

    /**
     * Schedules a retention notification to remind the player of unfinished daily quests / login bonus.
     */
    fun scheduleDailyQuestReminder(
        delayHours: Int = 4
    ) {
        val ctx = appContext ?: return
        val triggerTimeMs = System.currentTimeMillis() + (delayHours * 3600 * 1000L)
        val title = "🎯 Günlük Ticaret Görevleri Sizi Bekliyor!"
        val message = "Günün ticaret hedeflerini tamamlayın, Elmas ve Sezon Pasosu TP'si kazanın!"

        scheduleAlarmNotification(
            context = ctx,
            channelId = CHANNEL_QUESTS,
            title = title,
            message = message,
            triggerAtMs = triggerTimeMs,
            requestCode = 30001
        )
    }

    /**
     * Schedules a notification for new Historical Museum Artifact Auction open for bids.
     */
    fun scheduleMuseumAuctionReminder(
        delayHours: Int = 4
    ) {
        val ctx = appContext ?: return
        val triggerTimeMs = System.currentTimeMillis() + (delayHours * 3600 * 1000L)
        val title = "🏛️ Müze Müzayedesinde Yeni Bir Tarihi Eser Teklife Açıldı!"
        val message = "Nadir Selçuklu & Osmanlı eserlerini koleksiyonunuza katmak ve şirket prestijinizi katlamak için teklif verin!"

        scheduleAlarmNotification(
            context = ctx,
            channelId = CHANNEL_MARKET,
            title = title,
            message = message,
            triggerAtMs = triggerTimeMs,
            requestCode = 30002
        )
    }

    /**
     * Schedules a notification when factories are approaching offline capacity.
     */
    fun scheduleOfflineCapacityReminder(
        delayHours: Int = 12
    ) {
        val ctx = appContext ?: return
        val triggerTimeMs = System.currentTimeMillis() + (delayHours * 3600 * 1000L)
        val title = "🌙 Fabrikalarınız Üretim Kapasitesine Ulaşmak Üzere!"
        val message = "Tesis depoları dolmadan önce üretilen malları toplayın ve ihracata gönderin!"

        scheduleAlarmNotification(
            context = ctx,
            channelId = CHANNEL_LOGISTICS,
            title = title,
            message = message,
            triggerAtMs = triggerTimeMs,
            requestCode = 30003
        )
    }

    /**
     * Schedules a notification when 24h max offline capacity is reached.
     */
    fun scheduleOfflineMaxCapacityReminder(
        delayHours: Int = 24
    ) {
        val ctx = appContext ?: return
        val triggerTimeMs = System.currentTimeMillis() + (delayHours * 3600 * 1000L)
        val title = "⚠️ Çevrimdışı Fabrikanız 24 Saatlik Maksimum Üretim Kapasitesine Ulaştı!"
        val message = "Üretimin durmaması ve makinelerin çalışmaya devam etmesi için fabrikanızı kontrol edin!"

        scheduleAlarmNotification(
            context = ctx,
            channelId = CHANNEL_LOGISTICS,
            title = title,
            message = message,
            triggerAtMs = triggerTimeMs,
            requestCode = 30004
        )
    }

    /**
     * Schedules smart retention reminders upon app exit (onStop).
     */
    fun scheduleSmartRetentionPack() {
        // 1. 4 Hours: Museum auction or daily quest reminder
        scheduleMuseumAuctionReminder(delayHours = 4)
        // 2. 12 Hours: Factory capacity reminder
        scheduleOfflineCapacityReminder(delayHours = 12)
        // 3. 24 Hours: 24h Max offline accumulation reached
        scheduleOfflineMaxCapacityReminder(delayHours = 24)
    }

    /**
     * Helper to schedule an exact or inexact alarm based on Android API level.
     */
    private fun scheduleAlarmNotification(
        context: Context,
        channelId: String,
        title: String,
        message: String,
        triggerAtMs: Long,
        requestCode: Int
    ) {
        try {
            val intent = Intent(context, LocalGameNotificationReceiver::class.java).apply {
                putExtra(LocalGameNotificationReceiver.EXTRA_CHANNEL_ID, channelId)
                putExtra(LocalGameNotificationReceiver.EXTRA_TITLE, title)
                putExtra(LocalGameNotificationReceiver.EXTRA_MESSAGE, message)
                putExtra(LocalGameNotificationReceiver.EXTRA_NOTIFICATION_ID, requestCode)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm notification '$title' in ${(triggerAtMs - System.currentTimeMillis()) / 1000}s (ReqCode: $requestCode)")
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to schedule alarm notification: ${e.message}")
        }
    }

    /**
     * Posts an instant Android system push notification immediately.
     */
    fun postInstantNotification(
        context: Context,
        channelId: String,
        title: String,
        message: String,
        notificationId: Int = 40001
    ) {
        // When app is in foreground, UI already displays in-app notifications (SmartNotificationManager).
        // Suppress system push notifications to prevent filling Binder IPC buffer with PendingIntents.
        if (isAppInForeground) {
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastInstantNotificationTimeMs < 5000L) {
            // Rate limit background system notifications to at most once per 5 seconds
            return
        }
        lastInstantNotificationTimeMs = now

        try {
            val launchIntent = Intent(context, com.example.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(com.example.R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(notificationId, builder.build())
            Log.d(TAG, "Instant push notification sent: $title (ID: $notificationId)")
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to post instant push notification", e)
        }
    }

    /**
     * Posts a native push notification when a commodity enters borsa crisis (≤ 999 Ton).
     */
    fun postBorsaCrisisNotification(
        productDisplayName: String,
        price: Long,
        stock: Long
    ) {
        val ctx = appContext ?: return
        val title = "🚨 Borsa Kriz Alarmı: $productDisplayName!"
        val message = "Rezervler tükendi ($stock Ton)! Fiyat ₳${com.example.ui.components.formatCredit(price)} seviyesine fırladı. Tesislerindeki stokları satma ve +%25 Devlet Teşvik Primini toplama tam zamanı!"
        
        postInstantNotification(
            context = ctx,
            channelId = CHANNEL_MARKET,
            title = title,
            message = message,
            notificationId = 40000 + (productDisplayName.hashCode().let { if (it < 0) -it else it } % 1000)
        )
    }

    /**
     * Posts a notification when an AI arbitrage or limit order is executed.
     */
    fun postArbitrageExecutedNotification(
        productDisplayName: String,
        actionType: String, // "ALIM" or "SATIM"
        quantity: Int,
        price: Long,
        profit: Long = 0L
    ) {
        val ctx = appContext ?: return
        val title = "🤖 AI Borsa Arbitraj Botu: $actionType Gerçekleşti!"
        val profitText = if (profit > 0L) " (+₳${com.example.ui.components.formatCredit(profit)} Net Kâr)" else ""
        val message = "$productDisplayName emri uygulandı: $quantity Ton @ ₳${com.example.ui.components.formatCredit(price)}$profitText."

        postInstantNotification(
            context = ctx,
            channelId = CHANNEL_MARKET,
            title = title,
            message = message,
            notificationId = 45000 + (productDisplayName.hashCode().let { if (it < 0) -it else it } % 1000)
        )
    }

    /**
     * Cancels any previously scheduled alarm notification.
     */
    fun cancelScheduledNotification(requestCode: Int) {
        val ctx = appContext ?: return
        try {
            val intent = Intent(ctx, LocalGameNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                ctx,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to cancel notification: ${e.message}")
        }
    }

    /**
     * Posts a native push notification when monthly leaderboard reward is awarded.
     */
    fun postMonthlyRewardNotification(
        monthName: String,
        rank: Int,
        gems: Int
    ) {
        val ctx = appContext ?: return
        val title = "🏆 $monthName Ayı Sıralama Ödülünüz Hesabınızda!"
        val message = "Tebrikler! $monthName ayı holding liginde $rank. oldunuz. +$gems 💎 Elmas otomatik olarak hesabınıza yansıtıldı!"

        postInstantNotification(
            context = ctx,
            channelId = CHANNEL_QUESTS,
            title = title,
            message = message,
            notificationId = 55000 + rank
        )
    }
}
