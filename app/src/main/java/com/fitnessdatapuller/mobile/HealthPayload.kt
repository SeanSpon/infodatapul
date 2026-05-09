package com.fitnessdatapuller.mobile

import kotlinx.serialization.Serializable

@Serializable
data class HealthPayload(
    val date: String,
    val steps: Long,
    val activeCalories: Double,
    val totalCalories: Double? = null,
    val distanceMiles: Double? = null,
    val floorsClimbed: Double? = null,
    val exerciseMinutes: Double? = null,
    val workoutCount: Int = 0,
    val workouts: List<WorkoutPayload> = emptyList(),
    val nutrition: NutritionPayload = NutritionPayload(),
    val hydrationLiters: Double? = null,
    val sleepHours: Double,
    val sleepQuality: String = "unknown",
    val weightLbs: Double? = null,
    val restingHr: Long? = null,
    val avgHr: Long? = null,
    val hrvRmssdMs: Double? = null,
    val oxygenSaturationPct: Double? = null,
    val respiratoryRate: Double? = null,
    val sources: List<String> = emptyList(),
    val sourceUpdatedAt: String,
    val aiSummary: String = "Synced from Health Connect.",
)

@Serializable
data class WorkoutPayload(
    val title: String? = null,
    val exerciseType: Int,
    val startTime: String,
    val endTime: String,
    val durationMinutes: Double,
    val source: String,
)

@Serializable
data class NutritionPayload(
    val calories: Double? = null,
    val proteinG: Double? = null,
    val carbsG: Double? = null,
    val fatG: Double? = null,
    val sugarG: Double? = null,
    val fiberG: Double? = null,
    val sodiumMg: Double? = null,
)
