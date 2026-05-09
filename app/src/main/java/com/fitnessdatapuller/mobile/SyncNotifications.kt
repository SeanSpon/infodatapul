package com.fitnessdatapuller.mobile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SyncNotifications {
    const val CHANNEL_ID = "seanos_sync"
    const val CHANNEL_NAME = "SeanOS Health Sync"
    const val PERSISTENT_NOTIFICATION_ID = 1001
    const val ACTION_SYNC_NOW = "com.fitnessdatapuller.mobile.SYNC_NOW"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "One-tap sync and daily auto-sync status."
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun postPersistent(
        context: Context,
        statusLine: String,
        timestamp: Long? = System.currentTimeMillis(),
    ) {
        ensureChannel(context)
        val syncIntent = Intent(context, SyncActionReceiver::class.java).apply {
            action = ACTION_SYNC_NOW
            setPackage(context.packageName)
        }
        val syncPending = PendingIntent.getBroadcast(
            context,
            0,
            syncIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            context,
            1,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val whenText = timestamp?.let {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it))
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("SeanOS Health Sync")
            .setContentText(statusLine)
            .setContentIntent(openPending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(timestamp != null)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_popup_sync, "Sync now", syncPending)

        if (timestamp != null) builder.setWhen(timestamp)
        if (whenText != null) {
            builder.setSubText("Last sync $whenText")
        }

        runCatching {
            NotificationManagerCompat.from(context).notify(PERSISTENT_NOTIFICATION_ID, builder.build())
        }
    }
}
