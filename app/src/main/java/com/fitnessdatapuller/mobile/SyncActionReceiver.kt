package com.fitnessdatapuller.mobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SyncActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != SyncNotifications.ACTION_SYNC_NOW) return
        SyncNotifications.postPersistent(context, "Syncing…")
        SyncScheduler.runNow(context.applicationContext)
    }
}
