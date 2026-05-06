package com.fitnessdatapuller.mobile

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

class HealthConnectManager(private val context: Context) {
    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
    )

    fun isAvailable(): Boolean = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    fun requestPermissionsContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun hasAllPermissions(): Boolean {
        if (!isAvailable()) return false
        return HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions().containsAll(permissions)
    }

    suspend fun readToday(): HealthPayload {
        check(isAvailable()) { "Health Connect is not available on this device." }
        val client = HealthConnectClient.getOrCreate(context)
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = today.atStartOfDay(zone).toInstant()
        val end = Instant.now()
        val range = TimeRangeFilter.between(start, end)

        val aggregate = client.aggregate(
            AggregateRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                    ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                ),
                timeRangeFilter = range,
            ),
        )

        val steps = aggregate[StepsRecord.COUNT_TOTAL] ?: 0L
        val activeCaloriesKcal = aggregate[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
            ?.inKilocalories
            ?: 0.0

        return HealthPayload(
            date = today.toString(),
            steps = steps,
            activeCalories = activeCaloriesKcal.roundTo(1),
            sleepHours = readSleepHours(client, start, end).roundTo(2),
            weightLbs = readLatestWeightPounds(client, start, end)?.roundTo(1),
            restingHr = readAverageHeartRate(client, range),
            sourceUpdatedAt = ZonedDateTime.now(zone).toInstant().toString(),
        )
    }

    private suspend fun readSleepHours(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
    ): Double {
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
            ),
        ).records

        val seconds = records.sumOf { record ->
            val overlapStart = max(record.startTime.epochSecond, start.epochSecond)
            val overlapEnd = min(record.endTime.epochSecond, end.epochSecond)
            max(0, overlapEnd - overlapStart)
        }
        return seconds / 3600.0
    }

    private suspend fun readLatestWeightPounds(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
    ): Double? {
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
            ),
        ).records

        val latest = records.maxByOrNull { it.time } ?: return null
        return latest.weight.inKilograms * 2.2046226218
    }

    private suspend fun readAverageHeartRate(
        client: HealthConnectClient,
        range: TimeRangeFilter,
    ): Long? {
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = range,
            ),
        ).records
        val samples = records.flatMap { it.samples }.map { it.beatsPerMinute }
        if (samples.isEmpty()) return null
        return samples.average().roundToLong()
    }

    private fun Double.roundTo(decimals: Int): Double {
        val scale = Math.pow(10.0, decimals.toDouble())
        return kotlin.math.round(this * scale) / scale
    }
}
