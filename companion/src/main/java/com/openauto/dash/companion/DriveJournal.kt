package com.openauto.dash.companion

import android.content.Context
import com.openauto.dash.companion.mine.DriveJournalContent // MINE: #41
import com.openauto.dash.companion.mine.DriveJournalRules // MINE: #41
import com.openauto.dash.link.DriveSummaries
import com.openauto.dash.link.DriveSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The car's drives, as the head unit reports them over the link ([DriveSummary]):
 * the trip computer's figures and the eco-driving score of each. Kept in
 * private preferences, newest first, so they are there once the car is
 * switched off and the link is gone. One entry per drive (its start time): the
 * drive under way is reported as it goes and replaced by its final figures.
 */
object DriveJournal {
    private const val PREFS = "drive_journal"
    private const val KEY = "drives"
    private const val KEY_DELETED = "deleted_started_at" // MINE: #41
    private const val MAX_ITEMS = 100

    private val _drives = MutableStateFlow<List<DriveSummary>>(emptyList())
    val drives: StateFlow<List<DriveSummary>> = _drives
    private var loaded = false
    private var deletedStartedAt = emptySet<Long>() // MINE: #41

    @Synchronized
    fun load(context: Context): List<DriveSummary> {
        if (!loaded) {
            _drives.value = prefs(context).getString(KEY, null)?.let { DriveSummaries.decode(it) }.orEmpty()
            // MINE: #41 Keep deletions through sync reports from the head unit.
            deletedStartedAt = prefs(context).getStringSet(KEY_DELETED, emptySet())
                .orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
            loaded = true
        }
        return _drives.value
    }

    /** One drive's latest figures. */
    @Synchronized
    fun update(context: Context, drive: DriveSummary) {
        write(context, DriveJournalRules.update(content(context), drive, MAX_ITEMS)) // MINE: #41
    }

    /** The drive that started at [startedAt] is gone, and the rest stays (#41). */
    @Synchronized
    fun remove(context: Context, startedAt: Long) {
        write(context, DriveJournalRules.remove(content(context), startedAt)) // MINE: #41
    }

    /** Every drive: the log starts over, the trip computer starts over (#41). */
    @Synchronized
    fun clear(context: Context) {
        write(context, DriveJournalRules.clear(content(context))) // MINE: #41
    }

    /** Everything the head unit knows, as the link comes up. */
    @Synchronized
    fun sync(context: Context, drives: List<DriveSummary>) {
        write(context, DriveJournalRules.sync(content(context), drives, MAX_ITEMS)) // MINE: #41
    }

    private fun content(context: Context): DriveJournalContent {
        load(context)
        return DriveJournalContent(_drives.value, deletedStartedAt)
    }

    private fun write(context: Context, content: DriveJournalContent) {
        if (content.drives == _drives.value && content.deletedStartedAt == deletedStartedAt) return
        _drives.value = content.drives
        deletedStartedAt = content.deletedStartedAt
        prefs(context).edit()
            .putString(KEY, DriveSummaries.encode(content.drives))
            // MINE: #41 Persist tombstones in the same preference edit as the drive list.
            .putStringSet(KEY_DELETED, content.deletedStartedAt.mapTo(mutableSetOf()) { it.toString() })
            .apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
