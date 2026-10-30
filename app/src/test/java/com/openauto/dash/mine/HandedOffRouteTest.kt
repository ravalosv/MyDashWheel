// MINE: código propio del fork MyDashwheel.
package com.openauto.dash.mine

import com.openauto.dash.NavHandoff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class HandedOffRouteTest {

    private val now = 1_000_000_000L

    private fun target(name: String = "Casa", lat: Double? = 19.43, lng: Double? = -99.13, query: String? = null, at: Long = now) =
        NavHandoff.Target(name, lat, lng, query, at)

    @Test
    fun nothingWithoutADestination() {
        assertNull(HandedOffRoute.shown(null, guidingInApp = false, nowMs = now))
    }

    @Test
    fun nothingWhileTheInAppGuidanceIsTheOneGuiding() {
        // The in-app guidance owns the line then: a second one would sit on top of it.
        assertNull(HandedOffRoute.shown(target(), guidingInApp = true, nowMs = now))
    }

    @Test
    fun aTripFromYesterdayIsNotDrawn() {
        assertNull(HandedOffRoute.shown(target(at = now - HandedOffRoute.MAX_AGE_MS - 1), guidingInApp = false, nowMs = now))
        val recent = target(at = now - 60_000L)
        assertSame(recent, HandedOffRoute.shown(recent, guidingInApp = false, nowMs = now))
    }

    @Test
    fun aDestinationWithAPositionIsDrawn() {
        val t = target()
        assertSame(t, HandedOffRoute.shown(t, guidingInApp = false, nowMs = now))
    }

    @Test
    fun aDestinationWithWordsButNoPositionIsLookedUpAndDrawn() {
        val t = target(lat = null, lng = null, query = "Calle 5 de Mayo 123, Centro")
        assertSame(t, HandedOffRoute.shown(t, guidingInApp = false, nowMs = now))
    }

    @Test
    fun aDestinationWithNeitherIsNotDrawn() {
        assertNull(HandedOffRoute.shown(target(lat = null, lng = null, query = ""), guidingInApp = false, nowMs = now))
        assertNull(HandedOffRoute.shown(target(lat = null, lng = null, query = null), guidingInApp = false, nowMs = now))
    }

    @Test
    fun theGapGrowsWithFailuresAndStopsAtThreeMinutes() {
        assertEquals(45_000L, HandedOffRoute.gap(0))
        assertEquals(90_000L, HandedOffRoute.gap(1))
        assertEquals(180_000L, HandedOffRoute.gap(2))
        assertEquals(180_000L, HandedOffRoute.gap(9))
        assertEquals(45_000L, HandedOffRoute.gap(-1))
    }
}
