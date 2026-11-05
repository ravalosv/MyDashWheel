package com.openauto.dash.companion.mine

import com.openauto.dash.link.DriveSummary
import com.openauto.dash.companion.mine.DriveJournalContent
import com.openauto.dash.companion.mine.DriveJournalRules
import org.junit.Assert.assertEquals
import org.junit.Test

class DriveJournalRulesTest {

    private fun drive(startedAt: Long) = DriveSummary(
        startedAt = startedAt,
        endedAt = startedAt + 60_000L,
        distanceKm = 1.0,
        movingMs = 50_000L,
        maxSpeedKmh = 40
    )

    @Test
    fun removedDriveDoesNotReturnFromReportsOrSync() {
        val content = DriveJournalContent(drives = listOf(drive(1L), drive(2L)))
        val removed = DriveJournalRules.remove(content, 1L)

        assertEquals(listOf(2L), DriveJournalRules.update(removed, drive(1L), 100).drives.map { it.startedAt })
        assertEquals(listOf(2L), DriveJournalRules.sync(removed, listOf(drive(1L), drive(2L)), 100).drives.map { it.startedAt })
    }

    @Test
    fun clearingKeepsDeletionMarkersForEveryKnownDrive() {
        val content = DriveJournalContent(drives = listOf(drive(1L), drive(2L)))
        val cleared = DriveJournalRules.clear(content)

        assertEquals(emptyList<Long>(), DriveJournalRules.sync(cleared, listOf(drive(1L), drive(2L)), 100).drives.map { it.startedAt })
        assertEquals(setOf(1L, 2L), cleared.deletedStartedAt)
    }

    @Test
    fun unrelatedReportsContinueToMergeNewestFirst() {
        val content = DriveJournalContent(drives = listOf(drive(2L)))

        assertEquals(
            listOf(3L, 2L),
            DriveJournalRules.update(content, drive(3L), 100).drives.map { it.startedAt }
        )
    }
}
