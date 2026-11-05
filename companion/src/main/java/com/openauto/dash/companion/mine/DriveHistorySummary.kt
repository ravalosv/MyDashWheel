package com.openauto.dash.companion.mine

import com.openauto.dash.link.DriveSummary
import kotlin.math.roundToInt

internal data class DriveHistorySummary(
    val distanceKm: Double,
    val elapsedMs: Long,
    val averageSpeedKmh: Int,
    val maxSpeedKmh: Int,
    val stoppedMs: Long
) {
    companion object {
        fun from(drives: List<DriveSummary>): DriveHistorySummary {
            val distanceKm = drives.sumOf { it.distanceKm }
            val elapsedMs = drives.sumOf { it.elapsedMs }
            val movingMs = drives.sumOf { it.movingMs }
            return DriveHistorySummary(
                distanceKm = distanceKm,
                elapsedMs = elapsedMs,
                averageSpeedKmh = if (movingMs > 0) {
                    (distanceKm / (movingMs / 3_600_000.0)).roundToInt()
                } else {
                    0
                },
                maxSpeedKmh = drives.maxOfOrNull { it.maxSpeedKmh } ?: 0,
                stoppedMs = drives.sumOf { (it.elapsedMs - it.movingMs).coerceAtLeast(0L) }
            )
        }
    }
}
