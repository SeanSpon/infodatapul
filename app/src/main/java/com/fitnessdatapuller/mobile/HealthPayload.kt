package com.fitnessdatapuller.mobile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthPayload(
    val date: String,
    val steps: Long,
    @SerialName("active_calories") val activeCalories: Double,
    @SerialName("total_calories") val totalCalories: Double? = null,
    @SerialName("distance_miles") val distanceMiles: Double? = null,
    @SerialName("floors_climbed") val floorsClimbed: Double? = null,
    @SerialName("exercise_minutes") val exerciseMinutes: Double? = null,
    @SerialName("workout_count") val workoutCount: Int = 0,
    val workouts: List<WorkoutPayload> = emptyList(),
    @SerialName("nutrition") val nutrition: NutritionPayload = NutritionPayload(),
    @SerialName("hydration_liters") val hydrationLiters: Double? = null,
    @SerialName("sleep_hours") val sleepHours: Double,
    @SerialName("sleep_quality") val sleepQuality: String = "unknown",
    @SerialName("weight_lbs") val weightLbs: Double? = null,
    @SerialName("resting_hr") val restingHr: Long? = null,
    @SerialName("avg_hr") val avgHr: Long? = null,
    @SerialName("hrv_rmssd_ms") val hrvRmssdMs: Double? = null,
    @SerialName("oxygen_saturation_pct") val oxygenSaturationPct: Double? = null,
    @SerialName("respiratory_rate") val respiratoryRate: Double? = null,
    val sources: List<String> = emptyList(),
    @SerialName("source_updated_at") val sourceUpdatedAt: String,
    @SerialName("ai_summary") val aiSummary: String = "Synced from Health Connect.",
)

@Serializable
data class WorkoutPayload(
    val title: String? = null,
    @SerialName("exercise_type") val exerciseType: Int,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("duration_minutes") val durationMinutes: Double,
    val source: String,
)

@Serializable
data class NutritionPayload(
    @SerialName("calories") val calories: Double? = null,
    @SerialName("protein_g") val proteinG: Double? = null,
    @SerialName("carbs_g") val carbsG: Double? = null,
    @SerialName("fat_g") val fatG: Double? = null,
    @SerialName("sugar_g") val sugarG: Double? = null,
    @SerialName("fiber_g") val fiberG: Double? = null,
    @SerialName("sodium_mg") val sodiumMg: Double? = null,
)
