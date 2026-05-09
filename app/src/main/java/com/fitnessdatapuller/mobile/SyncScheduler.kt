package com.fitnessdatapuller.mobile

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object SyncScheduler {
    private const val DAILY_WORK_NAME = "seanos_daily_sync"
    private const val ONESHOT_WORK_NAME = "seanos_oneshot_sync"
    private const val DAILY_HOUR = 8
    private const val DAILY_MINUTE = 0

    fun scheduleDaily(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val targetToday = LocalDate.now(zone)
            .atTime(LocalTime.of(DAILY_HOUR, DAILY_MINUTE))
            .atZone(zone)
        val firstFire = if (targetToday.isAfter(now)) targetToday else targetToday.plusDays(1)
        val initialDelayMs = Duration.between(now, firstFire).toMillis().coerceAtLeast(0L)

        val request = PeriodicWorkRequestBuilder<SyncWorker>(Duration.ofHours(24))
            .setConstraints(constraints)
            .setInitialDelay(Duration.ofMillis(initialDelayMs))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(15))
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun runNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofSeconds(30))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONESHOT_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
