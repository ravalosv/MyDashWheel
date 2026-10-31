package com.openauto.dash.mine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The box the «Ruta completa» button frames: every point of the line, no more (#23). */
class MapRouteOverviewTest {
    private fun line(vararg points: Pair<Double, Double>) = RouteLine("test", points.toList())

    @Test
    fun theBoxHoldsEveryPointOfTheRoute() {
        val box = MapRouteOverview.bounds(
            line(21.8 to -102.3, 22.1 to -101.9, 21.9 to -102.6, 22.05 to -102.0)
        )
        assertEquals(RouteBox(south = 21.8, west = -102.6, north = 22.1, east = -101.9), box)
    }

    @Test
    fun aStraightLineIsItsOwnBox() {
        // A route that only runs east: its box has no north–south stretch.
        val box = MapRouteOverview.bounds(line(21.8 to -102.3, 21.8 to -102.0, 21.8 to -101.7))
        assertEquals(RouteBox(south = 21.8, west = -102.3, north = 21.8, east = -101.7), box)
    }

    @Test
    fun theCornersDoNotDependOnTheOrderOfThePoints() {
        val forwards = MapRouteOverview.bounds(line(21.8 to -102.3, 22.1 to -101.9))
        val backwards = MapRouteOverview.bounds(line(22.1 to -101.9, 21.8 to -102.3))
        assertEquals(forwards, backwards)
    }

    @Test
    fun withoutAPointThereIsNothingToFrame() {
        assertNull(MapRouteOverview.bounds(null))
        assertNull(MapRouteOverview.bounds(RouteLine("empty", emptyList())))
    }
}
