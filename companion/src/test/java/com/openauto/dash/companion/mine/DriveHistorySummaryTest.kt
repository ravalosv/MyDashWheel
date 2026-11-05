package com.openauto.dash.companion.mine

import com.openauto.dash.link.DriveSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class DriveHistorySummaryTest {

    @Test
    fun combinesEveryDriveAndUsesMovingTimeForGlobalAverage() {
        val summary = DriveHistorySummary.from(
            listOf(
                DriveSummary(0L, 3_600_000L, 36.0, 1_800_000L, 90),
                DriveSummary(0L, 5_400_000L, 54.0, 3_600_000L, 120)
            )
        )

        assertEquals(90.0, summary.distanceKm, 0.0)
        assertEquals(9_000_000L, summary.elapsedMs)
        assertEquals(60, summary.averageSpeedKmh)
        assertEquals(120, summary.maxSpeedKmh)
        assertEquals(3_600_000L, summary.stoppedMs)
    }

    @Test
    fun emptyHistoryHasZeroTotals() {
        assertEquals(DriveHistorySummary(0.0, 0L, 0, 0, 0L), DriveHistorySummary.from(emptyList()))
    }
}
