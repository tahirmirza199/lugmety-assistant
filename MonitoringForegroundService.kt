package com.lugmety.orderassistant.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lugmety.orderassistant.LugmetyApp
import com.lugmety.orderassistant.R
import com.lugmety.orderassistant.ui.MainActivity

class MonitoringForegroundService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_MONITORING"
        const val ACTION_PAUSE = "ACTION_PAUSE_MONITORING"
        const val ACTION_STOP = "ACTION_STOP_MONITORING"

        const val EXTRA_INTERVAL = "EXTRA_INTERVAL"
        const val EXTRA_COOLDOWN = "EXTRA_COOLDOWN"
        const val EXTRA_AUTO_REFRESH = "EXTRA_AUTO_REFRESH"
        const val EXTRA_ORDER_DETECTION = "EXTRA_ORDER_DETECTION"

        fun start(
            context: Context,
            intervalSec: Int = 8,
            cooldownSec: Int = 4,
            autoRefresh: Boolean = true,
            orderDetection: Boolean = true
        ) {
            val intent = Intent(context, MonitoringForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_INTERVAL, intervalSec)
                putExtra(EXTRA_COOLDOWN, cooldownSec)
                putExtra(EXTRA_AUTO_REFRESH, autoRefresh)
                putExtra(EXTRA_ORDER_DETECTION, orderDetection)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MonitoringForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun showNewOrderNotification(context: Context, orderNum: String, store: String, payout: String) {
            val openLugmetyIntent = try {
                context.packageManager.getLaunchIntentForPackage("com.lugmety.courier")
                    ?: context.packageManager.getLaunchIntentForPackage("com.lugmety.driver")
                    ?: context.packageManager.getLaunchIntentForPackage("com.lugmety.delivery")
                    ?: Intent(context, MainActivity::class.java)
            } catch (_: Exception) {
                Intent(context, MainActivity::class.java)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                openLugmetyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, LugmetyApp.CHANNEL_NEW_ORDERS)
                .setSmallIcon(R.drawable.ic_notification_order)
                .setContentTitle("New Lugmety Order Available!")
                .setContentText("#$orderNum - $store ($payout)")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Order #$orderNum\nRestaurant: $store\nEstimated Payout: $payout\nTap to review and accept in Lugmety.")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_open_app, "Open Lugmety", pendingIntent)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
            } catch (e: SecurityException) {
                // Handled if POST_NOTIFICATIONS was revoked
            }
        }

        fun showDiagnosticAlert(context: Context, title: String, message: String) {
            val notification = NotificationCompat.Builder(context, LugmetyApp.CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(1002, notification)
            } catch (_: SecurityException) {}
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val interval = intent.getIntExtra(EXTRA_INTERVAL, 8)
                val cooldown = intent.getIntExtra(EXTRA_COOLDOWN, 4)
                val autoRefresh = intent.getBooleanExtra(EXTRA_AUTO_REFRESH, true)
                val orderDetection = intent.getBooleanExtra(EXTRA_ORDER_DETECTION, true)

                startForeground(NOTIFICATION_ID, buildForegroundNotification("Active: Monitoring Lugmety screen"))
                // Pass all four parameters to startMonitoring
                LugmetyAccessibilityService.instance?.startMonitoring(
                    intervalSec = interval,
                    cooldownSec = cooldown,
                    autoRefresh = autoRefresh,
                    orderDetection = orderDetection
                )
            }
            ACTION_PAUSE -> {
                updateNotification("Monitoring Paused")
                LugmetyAccessibilityService.instance?.pauseMonitoring("Paused from notification")
            }
            ACTION_STOP -> {
                // Correct emergencyStopAll() call
                LugmetyAccessibilityService.instance?.emergencyStopAll()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun buildForegroundNotification(statusText: String): Notification {
        val appIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MonitoringForegroundService::class.java).apply { action = ACTION_PAUSE },
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, MonitoringForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, LugmetyApp.CHANNEL_MONITORING)
            .setSmallIcon(R.drawable.ic_service_active)
            .setContentTitle("Lugmety Order Assistant")
            .setContentText(statusText)
            .setContentIntent(appIntent)
            .setOngoing(true)
            .addAction(R.drawable.ic_pause, "Pause", pauseIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopIntent)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val notification = buildForegroundNotification(statusText)
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
