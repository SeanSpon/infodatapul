package com.fitnessdatapuller.mobile

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val settings = SettingsStore(context).load()
        if (settings.apiBaseUrl.isBlank() || settings.syncApiKey.isBlank()) {
            SyncNotifications.postPersistent(
                context,
                "Open the app and save your API URL + key — auto-sync is paused.",
            )
            return Result.failure()
        }

        val manager = HealthConnectManager(context)
        if (!manager.isAvailable()) {
            SyncNotifications.postPersistent(context, "Health Connect not available on this device.")
            return Result.failure()
        }
        if (!manager.hasAllPermissions()) {
            SyncNotifications.postPersistent(
                context,
                "Health Connect permissions missing — open the app to grant them.",
            )
            return Result.failure()
        }

        val client = SyncClient()
        return runCatching {
            val payload = manager.readToday()
            client.sync(settings, payload)
            payload
        }.fold(
            onSuccess = { payload ->
                val workouts = payload.workoutCount
                val cal = payload.nutrition.calories?.toInt()
                val steps = payload.steps
                val summary = buildString {
                    append("Synced · steps ")
                    append(steps)
                    if (cal != null && cal > 0) {
                        append(" · food ")
                        append(cal)
                        append(" kcal")
                    }
                    if (workouts > 0) {
                        append(" · ")
                        append(workouts)
                        append(if (workouts == 1) " workout" else " workouts")
                    }
                }
                SyncNotifications.postPersistent(context, summary)
                Result.success()
            },
            onFailure = { err ->
                val msg = err.message?.take(120) ?: err::class.java.simpleName
                SyncNotifications.postPersistent(context, "Sync failed: $msg")
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            },
        )
    }
}
