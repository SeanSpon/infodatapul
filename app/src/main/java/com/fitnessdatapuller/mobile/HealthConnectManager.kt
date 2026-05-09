package com.fitnessdatapuller.mobile

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
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
    private val healthConnectClient: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(FloorsClimbedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(RespiratoryRateRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
    )

    fun isAvailable(): Boolean = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    fun requestPermissionsContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun hasAllPermissions(): Boolean {
        if (!isAvailable()) return false
        return healthConnectClient.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    suspend fun readToday(): HealthPayload {
        check(isAvailable()) { "Health Connect is not available on this device." }
        val client = healthConnectClient
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = today.atStartOfDay(zone).toInstant()
        val sleepStart = today.minusDays(1).atStartOfDay(zone).toInstant()
        val end = Instant.now()
        val range = TimeRangeFilter.between(start, end)

        val aggregate = client.aggregate(
            AggregateRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                    ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                    TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                    DistanceRecord.DISTANCE_TOTAL,
                    FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL,
                    ExerciseSessionRecord.EXERCISE_DURATION_TOTAL,
                    NutritionRecord.ENERGY_TOTAL,
                    NutritionRecord.PROTEIN_TOTAL,
                    NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL,
                    NutritionRecord.TOTAL_FAT_TOTAL,
                    NutritionRecord.SUGAR_TOTAL,
                    NutritionRecord.DIETARY_FIBER_TOTAL,
                    NutritionRecord.SODIUM_TOTAL,
                    HydrationRecord.VOLUME_TOTAL,
                    RestingHeartRateRecord.BPM_AVG,
                ),
                timeRangeFilter = range,
            ),
        )

        val steps = aggregate[StepsRecord.COUNT_TOTAL] ?: 0L
        val activeCaloriesKcal = aggregate[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
            ?.inKilocalories
            ?: 0.0
        val workouts = readWorkouts(client, start, end)
        val avgHr = readAverageHeartRate(client, range)
        val restingHr = aggregate[RestingHeartRateRecord.BPM_AVG] ?: avgHr
        val sources = collectSources(client, start, end)

        return HealthPayload(
            date = today.toString(),
            steps = steps,
            activeCalories = activeCaloriesKcal.roundTo(1),
            totalCalories = aggregate[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories?.roundTo(1),
            distanceMiles = aggregate[DistanceRecord.DISTANCE_TOTAL]?.inMiles?.roundTo(2),
            floorsClimbed = aggregate[FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL]?.roundTo(1),
            exerciseMinutes = aggregate[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]
                ?.toMinutes()
                ?.toDouble()
                ?.roundTo(1),
            workoutCount = workouts.size,
            workouts = workouts,
            nutrition = NutritionPayload(
                calories = aggregate[NutritionRecord.ENERGY_TOTAL]?.inKilocalories?.roundTo(1),
                proteinG = aggregate[NutritionRecord.PROTEIN_TOTAL]?.inGrams?.roundTo(1),
                carbsG = aggregate[NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL]?.inGrams?.roundTo(1),
                fatG = aggregate[NutritionRecord.TOTAL_FAT_TOTAL]?.inGrams?.roundTo(1),
                sugarG = aggregate[NutritionRecord.SUGAR_TOTAL]?.inGrams?.roundTo(1),
                fiberG = aggregate[NutritionRecord.DIETARY_FIBER_TOTAL]?.inGrams?.roundTo(1),
                sodiumMg = aggregate[NutritionRecord.SODIUM_TOTAL]?.inMilligrams?.roundTo(1),
            ),
            hydrationLiters = aggregate[HydrationRecord.VOLUME_TOTAL]?.inLiters?.roundTo(2),
            sleepHours = readSleepHours(client, sleepStart, end, start).roundTo(2),
            weightLbs = readLatestWeightPounds(client, start, end)?.roundTo(1),
            restingHr = restingHr,
            avgHr = avgHr,
            hrvRmssdMs = readAverageHrv(client, start, end)?.roundTo(1),
            oxygenSaturationPct = readAverageOxygenSaturation(client, start, end)?.roundTo(1),
            respiratoryRate = readAverageRespiratoryRate(client, start, end)?.roundTo(1),
            sources = sources,
            sourceUpdatedAt = ZonedDateTime.now(zone).toInstant().toString(),
            aiSummary = "Synced from Health Connect sources including Samsung Health/Galaxy wearables, Hevy, Cronometer, and any other connected apps that wrote today's permitted data.",
        )
    }

    private suspend fun readSleepHours(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
        todayStart: Instant,
    ): Double {
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
            ),
        ).records

        val seconds = records
            .filter { record -> record.endTime.isAfter(todayStart) }
            .sumOf { record ->
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

    private suspend fun readWorkouts(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
    ): List<WorkoutPayload> {
        return client.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
            ),
        ).records.map { record ->
            WorkoutPayload(
                title = record.title,
                exerciseType = record.exerciseType,
                startTime = record.startTime.toString(),
                endTime = record.endTime.toString(),
                durationMinutes = (Duration.between(record.startTime, record.endTime).seconds / 60.0).roundTo(1),
                source = record.metadata.dataOrigin.packageName,
            )
        }
    }

    private suspend fun readAverageHrv(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
    ): Double? {
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateVariabilityRmssdRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
            ),
        ).records
        return records.map { it.heartRateVariabilityMillis }.averageOrNull()
    }

    private suspend fun readAverageOxygenSaturation(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
    ): Double? {
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = OxygenSaturationRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
            ),
        ).records
        return records.map { it.percentage.value }.averageOrNull()
    }

    private suspend fun readAverageRespiratoryRate(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
    ): Double? {
        val records = client.readRecords(
            ReadRecordsRequest(
                recordType = RespiratoryRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
            ),
        ).records
        return records.map { it.rate }.averageOrNull()
    }

    private suspend fun collectSources(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
    ): List<String> {
        val sources = mutableSetOf<String>()
        sources += readDataOrigins<ExerciseSessionRecord>(client, start, end)
        sources += readDataOrigins<NutritionRecord>(client, start, end)
        sources += readDataOrigins<HydrationRecord>(client, start, end)
        sources += readDataOrigins<SleepSessionRecord>(client, start, end)
        sources += readDataOrigins<HeartRateRecord>(client, start, end)
        sources += readDataOrigins<StepsRecord>(client, start, end)
        return sources.sorted()
    }

    private suspend inline fun <reified T : androidx.health.connect.client.records.Record> readDataOrigins(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
    ): Set<String> {
        return client.readRecords(
            ReadRecordsRequest(
                recordType = T::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                pageSize = 100,
            ),
        ).records.map { it.metadata.dataOrigin.packageName }.toSet()
    }

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()

    private fun Double.roundTo(decimals: Int): Double {
        val scale = Math.pow(10.0, decimals.toDouble())
        return kotlin.math.round(this * scale) / scale
    }
}
