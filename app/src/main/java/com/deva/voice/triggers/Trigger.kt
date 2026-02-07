package com.deva.voice.triggers

import java.util.UUID

enum class TriggerType {
    SCHEDULED_TIME,
    NOTIFICATION,
    CHARGING_STATE
}

data class Trigger(
    val id: String = UUID.randomUUID().toString(),
    val type: TriggerType,
    val instruction: String,
    var isEnabled: Boolean = true,
    // For SCHEDULED_TIME triggers
    val hour: Int? = null,
    val minute: Int? = null,
    // For NOTIFICATION triggers
    val packageName: String? = null,
    val appName: String? = null, // For display purposes
    // For SCHEDULED_TIME triggers
    val daysOfWeek: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7), // Default to all days
    // For CHARGING_STATE triggers
    val chargingStatus: String? = null // e.g., "Connected", "Disconnected"
)




