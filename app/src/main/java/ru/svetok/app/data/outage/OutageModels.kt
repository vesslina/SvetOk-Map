package ru.svetok.app.data.outage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

enum class OutageMapStatus {
    PLANNED,
    ACTIVE,
}

enum class OutageType {
    PLANNED,
    EMERGENCY,
    UNKNOWN;

    val label: String
        get() = when (this) {
            PLANNED -> "Плановое"
            EMERGENCY -> "Аварийное"
            UNKNOWN -> "Отключение"
        }

    companion object {
        fun fromString(value: String?): OutageType = when (value) {
            "planned" -> PLANNED
            "emergency" -> EMERGENCY
            else -> UNKNOWN
        }
    }
}

enum class OutageSource {
    DEMO,
    API,
    CACHE,
}

data class MapOutage(
    val id: String,
    val title: String,
    val outageType: OutageType,
    val status: OutageMapStatus,
    val reason: String,
    val timeLabel: String,
    val streetNorms: List<String>,
    val streetLabels: List<String>,
)

data class OutageLoadResult(
    val source: OutageSource,
    val outages: List<MapOutage>,
    val infoMessage: String? = null,
    val updatedAtMs: Long? = null,
)

interface OutageRepository {
    fun observeCachedOutages(): Flow<OutageLoadResult?> = emptyFlow()

    suspend fun refreshCurrentOutages(): OutageLoadResult = loadCurrentOutages()

    suspend fun loadCurrentOutages(): OutageLoadResult
}
