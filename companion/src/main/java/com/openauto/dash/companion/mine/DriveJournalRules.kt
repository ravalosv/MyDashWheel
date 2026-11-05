package com.openauto.dash.companion.mine

import com.openauto.dash.link.DriveSummaries
import com.openauto.dash.link.DriveSummary

internal data class DriveJournalContent(
    val drives: List<DriveSummary> = emptyList(),
    val deletedStartedAt: Set<Long> = emptySet()
)

internal object DriveJournalRules {
    fun update(content: DriveJournalContent, drive: DriveSummary, maxItems: Int): DriveJournalContent =
        if (drive.startedAt in content.deletedStartedAt) content
        else content.copy(drives = DriveSummaries.merge(content.drives, drive, maxItems))

    fun sync(content: DriveJournalContent, drives: List<DriveSummary>, maxItems: Int): DriveJournalContent =
        drives.fold(content) { current, drive -> update(current, drive, maxItems) }

    fun remove(content: DriveJournalContent, startedAt: Long): DriveJournalContent =
        content.copy(
            drives = content.drives.filterNot { it.startedAt == startedAt },
            deletedStartedAt = content.deletedStartedAt + startedAt
        )

    fun clear(content: DriveJournalContent): DriveJournalContent =
        content.copy(
            drives = emptyList(),
            deletedStartedAt = content.deletedStartedAt + content.drives.map { it.startedAt }
        )
}
