package com.example.drift

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build

const val DRIFT_NOTIFICATION_CHANNEL_ID = "drift_updates"

class DriftApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        UsageCollectionWorker.schedule(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val sound = Uri.parse("android.resource://$packageName/${R.raw.drift_notification}")
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannel(
            DRIFT_NOTIFICATION_CHANNEL_ID,
            "Drift notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders and helpful updates from Drift"
            setSound(sound, audioAttributes)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
