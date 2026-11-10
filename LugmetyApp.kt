package com.lugmety.orderassistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.lugmety.orderassistant.data.local.AppDatabase

class LugmetyApp : Application() {

    companion object {
        const val CHANNEL_MONITORING = "channel_monitoring_status"
        const val CHANNEL_NEW_ORDERS = "channel_new_orders"
        const val CHANNEL_ALERTS = "channel_diagnostic_alerts"
        lateinit var instance: LugmetyApp
            private set
    }

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // 1. Ongoing Foreground Monitoring Channel
            val monitoringChannel = NotificationChannel(
                CHANNEL_MONITORING,
                getString(R.string.channel_monitoring_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_monitoring_desc)
                setShowBadge(false)
            }

            // 2. High-Priority New Order Channel
            val orderChannel = NotificationChannel(
                CHANNEL_NEW_ORDERS,
                getString(R.string.channel_orders_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_orders_desc)
                enableVibration(true)
                setShowBadge(true)
            }

            // 3. Alerts Channel (Warnings, Failures)
            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS,
                getString(R.string.channel_alerts_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )

            notificationManager.createNotificationChannels(
                listOf(monitoringChannel, orderChannel, alertChannel)
            )
        }
    }
}
