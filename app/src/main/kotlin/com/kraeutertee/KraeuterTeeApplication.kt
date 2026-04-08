package com.kraeutertee

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration

class KraeuterTeeApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Harvest channel
            NotificationChannel(
                CHANNEL_HARVEST,
                getString(R.string.channel_harvest_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.channel_harvest_desc)
                manager.createNotificationChannel(this)
            }

            // Drying channel
            NotificationChannel(
                CHANNEL_DRYING,
                getString(R.string.channel_drying_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.channel_drying_desc)
                manager.createNotificationChannel(this)
            }
        }
    }

    companion object {
        const val CHANNEL_HARVEST = "harvest_reminders"
        const val CHANNEL_DRYING  = "drying_reminders"
    }
}
