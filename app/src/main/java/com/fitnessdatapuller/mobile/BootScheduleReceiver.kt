package com.fitnessdatapuller.mobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        SyncNotifications.ensureChannel(context)
        SyncScheduler.scheduleDaily(context.applicationContext)
        SyncNotifications.postPersistent(
            context,
            "Auto-sync scheduled. Tap Sync now to update immediately.",
        )
    }
}
