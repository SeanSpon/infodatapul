package com.fitnessdatapuller.mobile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthPayload(
    val date: String,
    val steps: Long,
    @SerialName("active_calories") val activeCalories: Double,
    @SerialName("sleep_hours") val sleepHours: Double,
    @SerialName("sleep_quality") val sleepQuality: String = "unknown",
    @SerialName("weight_lbs") val weightLbs: Double? = null,
    @SerialName("resting_hr") val restingHr: Long? = null,
    @SerialName("source_updated_at") val sourceUpdatedAt: String,
    @SerialName("ai_summary") val aiSummary: String = "Synced from Health Connect.",
)
