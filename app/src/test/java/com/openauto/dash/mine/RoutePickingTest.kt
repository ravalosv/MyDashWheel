package com.openauto.dash.mine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutePickingTest {
    /** A vertical line at lon -103.35, from lat 20.60 to 20.70, as the helper takes points: (lat, lon). */
    private val a = 20.60 to -103.35
    private val b = 20.70 to -103.35

    @Test
    fun `a tap on the line is no distance at all`() {
        assertTrue(RoutePicking.segmentDistance(20.65, -103.35, a, b) < 1e-9)
    }

    @Test
    fun `a tap beside the line is as far as it looks`() {
        // 0.01° of longitude at lat 20.65, once the latitude's cosine is taken.
        assertEquals(0.0094, RoutePicking.segmentDistance(20.65, -103.34, a, b), 0.0005)
    }

    @Test
    fun `a tap past the end measures to the end, not to the endless line`() {
        assertEquals(0.10, RoutePicking.segmentDistance(20.80, -103.35, a, b), 0.0005)
    }

    @Test
    fun `a tap far from the line is well outside the touch distance`() {
        assertTrue(RoutePicking.segmentDistance(21.50, -104.50, a, b) > 0.5)
    }
}
